package serap.io.loading.loader;

import serap.io.loading.MergingThreadManager;
import serap.io.ResourceManager;
import serap.io.loading.loader.context.RuntimeResourceLoaderContext;
import serap.io.resource.Resource;
import serap.io.resource.Unloadable;
import serap.io.resource.accessor.ResourceAccessor;
import serap.io.resource.source.ResourceSource;
import serap.io.resource.type.ResourceType;

public abstract class RuntimeResourceLoader<T extends Unloadable, C extends RuntimeResourceLoaderContext<T>> extends Loader<T, C> {
    protected RuntimeResourceLoader(C context) {
        super(context);
    }

    @Override
    public void runtimeOpen(ResourceManager reloadManager, MergingThreadManager mergingThreadManager,  ResourceAccessor accessor) {
        C context = this.getLoaderContext();

        Resource<?> resource = context.get(accessor.getIdentifier());

        if(resource.isReady()) return;

        if(resource.hasDependencies()) {
            for (Resource.Dependency dependency : resource.getDependencies()) {
                if(dependency.isReady()) continue;
                ResourceType<?> type = dependency.getResourceType();
                ResourceOpener opener = type.getResourceOpener();
                ResourceSource source = reloadManager.getResourceSource();
                ResourceAccessor dependencyAccessor = source.getAccessor(dependency.identifier());

                if(dependencyAccessor == null) {
                    throw new IllegalStateException(
                            "Resource contains dependency that doesn't exist" +
                            "[Resource=" + accessor.getIdentifier() + ", Dependency=" + dependency.identifier() + "]"
                    );
                }

                opener.runtimeOpen(reloadManager, mergingThreadManager, dependencyAccessor);
            }
        }

        super.runtimeOpen(reloadManager, mergingThreadManager, accessor);
    }
}
