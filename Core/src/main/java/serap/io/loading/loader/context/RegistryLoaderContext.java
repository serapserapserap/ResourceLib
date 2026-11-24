package serap.io.loading.loader.context;

import lombok.Getter;
import serap.core.registry.Registry;

@Getter
abstract class RegistryLoaderContext<T, T1, R extends Registry<T>> implements LoaderContext<T1> {
    private final R registry;
    RegistryLoaderContext(R registry) {
        this.registry = registry;
    }
}
