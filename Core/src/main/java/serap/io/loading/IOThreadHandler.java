package serap.io.loading;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import serap.io.ResourceLib;
import serap.io.discovery.SearchItemStorage;
import serap.io.loading.loader.context.LoaderContext;
import serap.io.resource.Unloadable;
import serap.io.resource.identifier.ResourceIdentifier;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class IOThreadHandler implements MergingThreadManager {
    private final ExecutorService executorService;
    private final List<ResourceMerger<?>> mergers = new ObjectArrayList<>();
    private final List<ResourceMerger<?>> mergersAwaitingRemoval = new ObjectArrayList<>();
    private final Queue<ResourceMerger<?>> incomingMergers = new ConcurrentLinkedQueue<>();
    private final Queue<CompletableFuture<?>> incomingFutures = new ConcurrentLinkedQueue<>();
    private final ObjectArrayList<CompletableFuture<?>> futures = new ObjectArrayList<>();
    private volatile Phase phase = Phase.IDLE;
    /**
     * stored for debugging
     */
    @Getter
    private volatile CompletableFuture<Void> finalizingFuture;
    private final SearchItemStorage storage;

    public IOThreadHandler(ExecutorService executorService, @NotNull SearchItemStorage searchItemStorage) {
        this.executorService = executorService;
        this.storage = searchItemStorage;
    }

    private int getTotalExpectedSubmissions() {
        return this.storage.totalReloadItems();
    }

    //main thread targeted
    public void startReload() {
        if(phase != Phase.IDLE) {
            ResourceLib.warn("Attempted to reload resources before the thread manager was ready, ignoring");
            return;
        }
        this.finalizingFuture = null;
        if(!this.mergers.isEmpty()) {
            ResourceLib.debug("Mergers list was not empty, adding all mergers to removal queue");
            this.mergersAwaitingRemoval.addAll(this.mergers);
            this.mergers.clear();

        }
        if(!this.futures.isEmpty()) {
            synchronized (this.futures) {
                this.futures.clear();
            }
        }
        this.phase = Phase.LOADING;
    }


    //async
    @Override
    public <T> void submit(ResourceIdentifier identifier, LoaderContext<T> loaderContext, Supplier<T> loadFunction) {
        CompletableFuture<T> future = CompletableFuture.supplyAsync(loadFunction, this.executorService);
        ResourceMerger<?> merger = ResourceMerger.of(identifier, loaderContext, future);
        this.onSubmission(future, merger);
    }

    //async
    private void onSubmission(CompletableFuture<?> future, ResourceMerger<?> merger) {
        this.incomingFutures.add(future);
        this.incomingMergers.add(merger);
    }

    //main thread targeted
    private void onMergeFutures() {
        if(this.phase == Phase.LOADING && isFinalSubmission()) {

            this.finalizingFuture = this.createFinalizingFuture();
        }
    }

    private void onFinishLoading() {
        this.phase = Phase.MERGING;
    }

    //main thread targeted
    private CompletableFuture<Void> createFinalizingFuture() {
        CompletableFuture<Void> future = this.futureOfAll();

        if(future.isDone()) {
            this.onFinishLoading();
        } else {
            future.thenRun(this::onFinishLoading);
        }

        return future;
    }

    //main thread targeted
    private CompletableFuture<Void> futureOfAll() {
        return CompletableFuture.allOf(this.futures.toArray(new CompletableFuture[]{}));
    }

    //unsure
    @Override
    public boolean isReadyToMergeResults() {
        return true;
    }

    private void mergeFutures() {
        CompletableFuture<?> incomingFuture;
        boolean addedFutures = false;
        while((incomingFuture = this.incomingFutures.poll()) != null) {
            addedFutures = true;
            this.futures.add(incomingFuture);
        }
        if(addedFutures) this.onMergeFutures();
    }

    private void mergeMergers() {
        ResourceMerger<?> incomingMerger;
        while((incomingMerger = this.incomingMergers.poll()) != null) {
            this.mergers.add(incomingMerger);
        }
    }

    private void mergeResultantMergers() {
        var iterator = this.mergers.iterator();
        while (iterator.hasNext()) {
            var merger = iterator.next();
            if (!merger.isReadyToMerge()) return;

            if (merger.finishedExceptionally()) {
                Throwable t = new IllegalStateException("Exception occurred while opening " + merger.identifier.getNamespacedIdentifier(), merger.getException());

                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));

                ResourceLib.error(sw.toString());
                return;
            };

            merger.merge();
            iterator.remove();
        }

        if(this.phase == Phase.IDLE) {
            this.removeFuturesIfDone();
        }
    }

    private void removeFuturesIfDone() {
        this.futures.removeIf(CompletableFuture::isDone);
    }

    //main thread targeted
    @Override
    public void mergeResults() {

        if(!ResourceLib.isMainThread()) {
            ResourceLib.warn("attempted to merge off the main thread, skipping to avoid possible concurrent modification");
            return;
        }

        this.mergeFutures();
        this.mergeMergers();

        if(this.phase != Phase.LOADING) {
            this.mergeResultantMergers();

            if(this.phase == Phase.MERGING) {
                this.phase = Phase.IDLE;
            }
        }
    }

    //main thread targeted
    @Override
    public void cancelRedundantComputes() {
        this.mergersAwaitingRemoval.forEach(ResourceMerger::cancelMerger);
        this.mergersAwaitingRemoval.clear();
    }

    @Override
    public boolean isStable() {
        return this.phase == Phase.IDLE;
    }

    //main thread targeted
    private boolean isFinalSubmission() {
        return this.futures.size() == this.getTotalExpectedSubmissions();
    }

    @Override
    public void cleanup() {
        this.executorService.close();
    }


    private static class ResourceMerger<T> {
        ResourceIdentifier identifier;
        LoaderContext<T> context;
        CompletableFuture<T> future;
        boolean canceled = false;

        private boolean isCanceled() {
            return this.canceled;
        }

        private void cancelMerger() {
            if(this.isCanceled()) return;
            this.canceled = true;
            if(this.isReadyToMerge()) {
                var result = this.future.getNow(null);
                if(result instanceof Unloadable unloadable) {
                    unloadable.unloadIfLoaded();
                }
                return;
            }

            if(!this.future.cancel(true)) {
                ResourceLib.warn("Failed to cancel future, this may result in leaking memory");
            }
        }

        /**
         * USE {@link ResourceMerger#of(ResourceIdentifier, LoaderContext, CompletableFuture)}
         */
        @Deprecated
        @SuppressWarnings("all")
        private ResourceMerger(ResourceIdentifier identifier, LoaderContext<T> context, CompletableFuture<T> future) {
            this.identifier = identifier;
            this.context = context;
            this.future = future;
        }

        @Contract(value = "_, _, _ -> new", pure = true)
        private static <T> @NotNull ResourceMerger<?> of(ResourceIdentifier identifier, LoaderContext<T> context, CompletableFuture<T> future) {
            return new ResourceMerger<>(identifier, context, future);
        }

        private boolean isReadyToMerge() {
            return this.future.isDone();
        }

        private boolean finishedExceptionally() {
            return this.future.isCompletedExceptionally();
        }

        public Throwable getException() {
            return this.future.exceptionNow();
        }

        private void merge() {
            try {
                this.context.deposit(this.identifier, this.future.get());
            } catch (InterruptedException | CancellationException | ExecutionException e) {
                throw new IllegalStateException("Failed to merge future into registry", e);
            }
        }
    }

    public enum Phase {
        IDLE,
        LOADING,
        MERGING
    }
}
