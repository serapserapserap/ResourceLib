package serap.io;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import serap.core.pointer.MutablePointer;
import serap.core.registry.Registry;
import serap.io.discovery.ResourceDependencyProcessor;
import serap.io.discovery.SearchItemProcessor;
import serap.io.discovery.SearchItemStorage;
import serap.io.loading.MergingThreadManager;
import serap.io.loading.loader.ResourceOpener;
import serap.io.resource.Resource;
import serap.io.resource.Unloadable;
import serap.io.resource.accessor.ResourceAccessor;
import serap.io.resource.identifier.ResourceIdentifier;
import serap.io.resource.source.ClasspathResourceSource;
import serap.io.resource.source.ResourceSource;
import serap.io.resource.source.StackedResourceSource;
import serap.io.resource.stack.ResourceSourceStack;
import serap.io.resource.stack.ResourceStack;
import serap.io.resource.type.ResourceType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ResourceManager {
    /**
     * a list of sources which should ALWAYS be able to provide a valid accessor for ANY identifier
     * present inside of {@link ResourceManager#searchItemStorage}.
     * should NEVER be cleared
     */
    private final ObjectArrayList<ResourceSource> baseSources = new ObjectArrayList<>();
    private final List<Registry.Mutable<?, ? extends MutablePointer<?>>> resourceRegistries = new ArrayList<>();
    @Getter
    private ResourceStack resourceStack;
    @Getter
    private ResourceSource resourceSource;
    private final Supplier<ResourceSource[]> sourceSupplier;
    private final MergingThreadManager threadManager;
    private final SearchItemStorage searchItemStorage;

    /**
     * creates a resource manager
     * @param searchItemStorage the {@link SearchItemStorage} the resource manager should use. Default is {@link serap.io.discovery.SearchItemContext}
     * @param threadManager the {@link MergingThreadManager} the resource manager should use. Default is {@link serap.io.loading.IOThreadHandler}
     * @param sourceSupplier the {@link Supplier} that should give the stack of resource sources, that the resource manager should use
     *                            objects. Default is {@link ResourceDependencyProcessor}
     * @param processors the additional {@link SearchItemProcessor} that the resource manager should run on the supplied {@link SearchItemStorage}
     */
    public ResourceManager(
            SearchItemStorage searchItemStorage,
            MergingThreadManager threadManager,
            Supplier<ResourceSource[]> sourceSupplier,
            SearchItemProcessor... processors
    ) {
        if(processors.length == 0) {
            ResourceLib.warn("Resource manager was not any processors, no resources dependencies will be loaded");
        }

        for (SearchItemProcessor processor : processors) {
            searchItemStorage.process(processor);
        }
        this.searchItemStorage = searchItemStorage;
        this.threadManager = threadManager;
        this.sourceSupplier = sourceSupplier;
    }

    /**
     * Adds a base source to the resource stack. A base source is generally a{@link ClasspathResourceSource}.
     * After all base sources are added, if ALL other sources in the stack were to fail, the stack should ALWAYS
     * be able to fall back on the base sources and have them NEVER fail
     * @param source a {@link ResourceSource} that the resource manager can rely on to ALWAYS produce a valid
     *               {@link ResourceAccessor} for some or all resources registered
     */
    public void addBaseSource(ResourceSource source) {
        this.baseSources.add(source);
    }

    /**
     * @return a {@link ResourceSource} that represents all the {@link ResourceSource} added by {@link ResourceManager#addBaseSource(ResourceSource)}  
     */
    private @NotNull ResourceSource generateBaseSource() {
        ResourceStack baseStack = new ResourceSourceStack(baseSources.toArray(new ResourceSource[]{}));
        return new StackedResourceSource(baseStack);
    }


    /**
     * Rebuilds the stack from the given array of {@link ResourceSource}
     * @param sources the array of {@link ResourceSource}
     * @return the built {@link ResourceStack}
     */
    private ResourceStack rebuildStackFrom(ResourceSource @NotNull ... sources) {
        ResourceSource baseSource = this.generateBaseSource();
        ResourceStack stack = new ResourceSourceStack(baseSource);
        for (int i = sources.length - 1; i >= 0; i--) {
            stack.addSource(sources[i]);
        }
        return this.resourceStack = stack;
    }

    /**
     * rebuilds the resource source from the given array of {@link ResourceSource}
     * @param sources the sources to rebuild the source stack from
     */
    private void rebuildResourceSourceStackFrom(ResourceSource... sources) {
        ResourceStack resultantStack = this.rebuildStackFrom(sources);
        this.resourceSource = this.rebuildResourceSourceFrom(resultantStack);
    }

    /**
     * Loads the resources for the first time, you must call this for the first load
     * otherwise subsequent calls to {@link ResourceManager#reloadResources()} will not
     * unload the loaded resources properly and will cause errors
     */
    public void initiallyLoadResources() {
        ResourceSource[] sources = this.sourceSupplier.get();
        this.rebuildResourceSourceStackFrom(sources);
        this.threadManager.startReload();

        this.loadRequired(true);
    }

    /**
     * reloads the resources, for initially loading resources, see {@link ResourceManager#initiallyLoadResources()}
     */
    public void reloadResources() {
        ResourceSource[] sources = this.sourceSupplier.get();
        this.rebuildResourceSourceStackFrom(sources);
        this.threadManager.startReload();

        this.unloadAll();
        this.loadRequired(false);
    }


    /**
     * unloads all resources
     */
    private void unloadAll() {
        this.threadManager.cancelRedundantComputes();
        for (Registry.Mutable<?, ?> resourceRegistry : this.resourceRegistries) {

            var ptr = resourceRegistry.findFirstWhere((ignored1, ignored2) -> true);
            if(!(ptr instanceof Resource<?>)) continue;

            resourceRegistry.forEach(((ignored, mutablePointer) -> {
                Unloadable u = ((Resource<?>) mutablePointer).get();
                if(u != null) u.unloadIfLoaded();
            }));
        }
    }

    /**
     * runs before a {@link serap.io.discovery.SearchItemStorage.SearchItem} is loaded for the first time.
     * It caches the registry if it's a {@link Registry.Mutable} of {@link Resource} so the resource manager
     * can unload the old resources when reloading
     * @param item the {@link serap.io.discovery.SearchItemStorage.SearchItem} about to be loaded
     */
    private void onFirstLoadOfSearchItem(@NotNull SearchItemStorage.SearchItem item) {
        ResourceType<?> type = item.getResourceType();
        if(!type.isRegistryIsMutable()) return;
        Registry.Mutable<?, ? extends MutablePointer<?>> registry = type.getRegistryAsMutable();

        if(this.resourceRegistries.contains(registry)) {
            this.resourceRegistries.add(registry);
        }
    }

    /**
     * polls and finalizes any resources that were loaded since this method was last called
     */
    public void pollResourceLoading() {
        if(this.threadManager.isReadyToMergeResults()) {
            this.threadManager.mergeResults();
        }
    }

    public boolean doneLoading() {
        return this.threadManager.isStable();
    }


    /**
     * Loads all resources that are required to always be loaded
     * @param firstLoad {@code true} if this is initial load for all the resources, {@code false} if it is not
     */
    private void loadRequired(boolean firstLoad) {
        this.searchItemStorage.forEachPath((item) -> {
            if(firstLoad) onFirstLoadOfSearchItem(item);
            if(!item.isAlwaysNeeded()) return;

            try {
                initialLoadResource(item);
            } catch (IOException e) {
                ResourceLib.error(new IllegalStateException(String.format("Failed to load resource %s", item.identifier().getNamespacedIdentifier()), e));
            }

        });
    }

    /**
     * Loads a given resource
     * @param identifier the {@link ResourceIdentifier} of the resource you want to load
     * @param opener the {@link ResourceOpener} it should use to open the resource
     */
    public void load(ResourceIdentifier identifier, @NotNull ResourceOpener opener) {
        ResourceAccessor accessor = this.getResourceSource().getAccessor(identifier);

        opener.runtimeOpen(this, this.threadManager, accessor);
    }

    /**
     * Loads a given resource
     * @param identifier the {@link ResourceIdentifier} of the resource you want to load
     * @param type the {@link ResourceType} of the resource being loaded
     */
    public void load(ResourceIdentifier identifier, @NotNull ResourceType<?> type) {
        this.load(identifier, type.getResourceOpener());
    }

    /**
     * Loads a given resource for the first time
     * @param item the item to be loaded
     * @throws IOException if the {@link ResourceOpener} fails to open the resource
     */
    private void initialLoadResource(SearchItemStorage.@NotNull SearchItem item) throws IOException{
        ResourceType<?> rType = item.getResourceType();
        ResourceOpener resourceOpener = rType.getResourceOpener();

        ResourceAccessor accessor = this.getResourceSource().getAccessor(item.identifier());
        resourceOpener.open(this.threadManager, accessor);
    }

    /**
     * rebuilds the {@link StackedResourceSource} from the given {@link ResourceStack}
     * @param stack the stack to build the {@link StackedResourceSource} from
     * @return the built {@link ResourceSource}
     */
    private @NotNull ResourceSource rebuildResourceSourceFrom(ResourceStack stack) {
         return new StackedResourceSource(stack);
    }

    public void cleanup() {
        this.threadManager.cleanup();
    }
}
