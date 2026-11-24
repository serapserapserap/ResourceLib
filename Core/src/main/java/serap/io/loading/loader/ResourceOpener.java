package serap.io.loading.loader;

import serap.io.ResourceLib;
import serap.io.loading.MergingThreadManager;
import serap.io.ResourceManager;
import serap.io.resource.accessor.ResourceAccessor;

import java.io.IOException;

public interface ResourceOpener {
    /**
     * opens the provided accessor. for opening a accessor during runtime see {@link ResourceOpener#runtimeOpen(ResourceManager, MergingThreadManager, ResourceAccessor)}
     * @param accessor the accessor to open
     * @throws IOException if opening the accessor failed
     */
    void open(MergingThreadManager mergingThreadManager, ResourceAccessor accessor) throws IOException;

    /**
     * opens the provided {@link ResourceAccessor}, catching and logging and errors encountered
     * @param resourceManager the {@link ResourceManager}
     * @param mergingThreadManager the {@link MergingThreadManager} that should merge the resource into its storage
     * @param accessor the {@link ResourceAccessor} for the given resource
     */
    default void runtimeOpen(ResourceManager resourceManager, MergingThreadManager mergingThreadManager, ResourceAccessor accessor) {
        try {
            this.open(mergingThreadManager, accessor);
        } catch (IOException e) {
            ResourceLib.error(new IllegalStateException("Failed to runtime open resource " + accessor.getIdentifier(), e));
        }

    }
}
