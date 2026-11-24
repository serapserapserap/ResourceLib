package serap.io.loading;

import serap.io.loading.loader.context.LoaderContext;
import serap.io.resource.identifier.ResourceIdentifier;

import java.util.function.Supplier;

public interface MergingThreadManager {
    /**
     * submits a load request to the thread manager
     * @param identifier the identifier of the resource being loaded
     * @param loaderContext the loading context of the resource being loaded
     * @param loadFunction the function to load the resource
     * @param <T> the type of the resource
     */
    <T> void submit(ResourceIdentifier identifier, LoaderContext<T> loaderContext, Supplier<T> loadFunction);

    /**
     * starts a reload
     */
    void startReload();

    /**
     * @return if the thread manager is ready to merge its results into their respective registries
     */
    boolean isReadyToMergeResults();

    /**
     * merges the results into their respective registries
     */
    void mergeResults();

    void cancelRedundantComputes();

    /**
     * @return if the thread manager is in a stable state.  {@code true} if the thread manager isn't currently initializing or reloading, {@code false}
     * if it is reloading or initializing
     */
    boolean isStable();

    void cleanup();
}
