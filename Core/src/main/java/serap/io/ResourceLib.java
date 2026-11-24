package serap.io;


import lombok.Getter;
import serap.io.discovery.ResourceDependencyProcessor;
import serap.io.discovery.SearchItemDependencyProcessor;
import serap.io.discovery.SearchItemProcessor;
import serap.io.discovery.SearchItemStorage;
import serap.io.loading.IOThreadHandler;
import serap.io.loading.MergingThreadManager;
import serap.io.resource.source.ResourceSource;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class ResourceLib {
    private static Consumer<Object> infoOutput, debugOutput, warnOutput, errorOutput;
    private static Thread mainThread;
    private static Supplier<ResourceSource[]> lazyResourceSourceSupplier = () -> new ResourceSource[0];

    public static void setResourceSourceSupplier(Supplier<ResourceSource[]> resourceSourceSupplier) {
        ResourceLib.lazyResourceSourceSupplier = resourceSourceSupplier;
    }

    @Getter
    private static ResourceManager resourceManager;

    public static boolean isMainThread() {
        return Thread.currentThread() == mainThread;
    }

    public static ResourceManager initResourceLib(Thread mainThread, ExecutorService service, SearchItemStorage searchItemStorage) {
        return initResourceLib(
                mainThread,
                searchItemStorage,
                new IOThreadHandler(service, searchItemStorage),
                ResourceLib.lazyResourceSourceSupplier,
                ResourceDependencyProcessor.createNew(),
                SearchItemDependencyProcessor.createNew()
        );
    }

    public static ResourceManager initResourceLib(ExecutorService service, SearchItemStorage searchItemStorage) {
        return initResourceLib(
                Thread.currentThread(),
                service,
                searchItemStorage
        );
    }

    public static ResourceManager initResourceLib(SearchItemStorage searchItemStorage) {
        return initResourceLib(
                Thread.currentThread(),
                Executors.newFixedThreadPool(10),
                searchItemStorage
        );
    }
    /**
     * initializes resource lib according to the specified params. Using the thread you call this on as the {@code mainThread} param of {@link ResourceLib#initResourceLib(Thread, SearchItemStorage, MergingThreadManager, Supplier, SearchItemProcessor...)}
     * @param searchItemStorage the search item storage containing all the identifiers of the resources it must load
     * @param threadManager the {@link MergingThreadManager} to use
     * @param sourceSupplier a supplier that gives resource lib a number of resource sources that it can search for possible asset overrides (think like minecraft)
     * @param additionalProcessors additional processors
     * @return the created {@link ResourceManager}
     */
    public static ResourceManager initResourceLib(SearchItemStorage searchItemStorage, MergingThreadManager threadManager, Supplier<ResourceSource[]> sourceSupplier, SearchItemProcessor... additionalProcessors) {
        return initResourceLib(
                Thread.currentThread(), searchItemStorage, threadManager, sourceSupplier, additionalProcessors);
    }

    /**
     * initializes resource lib according to the specified params. All overloads stem off this function
     * @param mainThread the thread that resource lib will recognise as the main thread
     * @param searchItemStorage the search item storage containing all the identifiers of the resources it must load
     * @param threadManager the {@link MergingThreadManager} to use
     * @param sourceSupplier a supplier that gives resource lib a number of resource sources that it can search for possible asset overrides (think like minecraft)
     * @param additionalProcessors additional processors
     * @return the created {@link ResourceManager}
     */
    public static ResourceManager initResourceLib(Thread mainThread, SearchItemStorage searchItemStorage, MergingThreadManager threadManager, Supplier<ResourceSource[]> sourceSupplier, SearchItemProcessor... additionalProcessors) {
        ResourceLib.mainThread = mainThread;
        return ResourceLib.resourceManager = new ResourceManager(
                searchItemStorage,
                threadManager,
                sourceSupplier,
                additionalProcessors
        );
    }

    static {
        clearLoggingOutputs();
    }

    private static void blankLoggingOutput(Object in){}

    /**
     * clears the logging outputs
     */
    public static void clearLoggingOutputs() {
        infoOutput = ResourceLib::blankLoggingOutput;
        debugOutput = ResourceLib::blankLoggingOutput;
        warnOutput = ResourceLib::blankLoggingOutput;
        errorOutput = ResourceLib::blankLoggingOutput;
    }

    /**
     * adds additional logging outputs for the library
     * @param infoOutput the consumer for info calls
     * @param debugOutput the consumer for debug calls
     * @param warnOutput the consumer for warn calls
     * @param errorOutput the consumer for error calls
     */
    public static void addLoggingOutput(Consumer<Object> infoOutput, Consumer<Object> debugOutput, Consumer<Object> warnOutput, Consumer<Object> errorOutput) {
        var oldInfo = ResourceLib.infoOutput;
        var oldDebug = ResourceLib.debugOutput;
        var oldWarn = ResourceLib.warnOutput;
        var oldError = ResourceLib.errorOutput;
        ResourceLib.infoOutput = (obj) -> {
            oldInfo.accept(obj);
            infoOutput.accept(obj);
        };
        ResourceLib.debugOutput = (obj) -> {
            oldDebug.accept(obj);
            debugOutput.accept(obj);
        };
        ResourceLib.warnOutput = (obj) -> {
            oldWarn.accept(obj);
            warnOutput.accept(obj);
        };
        ResourceLib.errorOutput = (obj) -> {
            oldError.accept(obj);
            errorOutput.accept(obj);
        };
    }

    public static void info(Object o) {
        ResourceLib.infoOutput.accept(o);
    }

    public static void debug(Object o) {
        ResourceLib.debugOutput.accept(o);
    }

    public static void warn(Object o) {
        ResourceLib.warnOutput.accept(o);
    }

    public static void error(Object o) {
        ResourceLib.errorOutput.accept(o);
    }
}
