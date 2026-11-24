package serap.io.loading.loader.context;

import serap.io.resource.identifier.ResourceIdentifier;

public interface LoaderContext<T> {
    //TODO this is not currently thread safe (registries in general aren't thread safe)
    // make a system to pool up created objects and add them all to the registry on the main thread
    // or if that's too hard just make a thread safe registry (but that would be slower)
    void deposit(ResourceIdentifier identifier, T value);
}
