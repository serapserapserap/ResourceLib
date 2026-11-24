package serap.io.loading.loader.context;

import serap.core.registry.Registry;
import serap.io.resource.identifier.ResourceIdentifier;

public class ResourceLoaderContext<T, R extends Registry<T>> extends RegistryLoaderContext<T, T, R> {
    public ResourceLoaderContext(R registry) {
        super(registry);
    }

    @Override
    public void deposit(ResourceIdentifier identifier, T value) {
        this.getRegistry().register(identifier, value);
    }
}
