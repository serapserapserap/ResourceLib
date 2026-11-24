package serap.io.loading.loader.context;

import serap.io.resource.identifier.ResourceIdentifier;

public interface LoaderContext<T> {
    void deposit(ResourceIdentifier identifier, T value);
}
