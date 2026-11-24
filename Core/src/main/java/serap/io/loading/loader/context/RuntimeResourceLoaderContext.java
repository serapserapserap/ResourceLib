package serap.io.loading.loader.context;

import serap.core.registry.Registry;
import serap.io.resource.Resource;
import serap.io.resource.Unloadable;
import serap.io.resource.identifier.ResourceIdentifier;

public class RuntimeResourceLoaderContext<T extends Unloadable> extends RegistryLoaderContext<Resource<T>, T, Registry.Mutable<T, Resource<T>>> {
    public RuntimeResourceLoaderContext(Registry.Mutable<T, Resource<T>> registry) {
        super(registry);
    }

    public Resource<T> get(ResourceIdentifier i) {
        return this.getRegistry().get(i);
    }

    @Override
    public void deposit(ResourceIdentifier identifier, T value) {
        this.getRegistry().deposit(identifier, value);
    }
}

