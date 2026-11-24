package serap.testimpl;

import serap.core.registry.Registry;
import serap.core.registry.RegistryIdentifier;
import serap.io.loading.loader.context.ResourceLoaderContext;
import serap.io.loading.loader.context.RuntimeResourceLoaderContext;
import serap.io.resource.Resource;
import serap.io.resource.type.ResourceType;

public class TestUtils {
    public static final ResourceType<?> RELOADABLE_STRING_RESOURCE_TYPE;
    public static final ResourceType<?> SPUNGUS_RESOURCE_TYPE;
    public static final Registry.Mutable<UnloadableString, Resource<UnloadableString>> RELOADABLE_STRING_REGISTRY = Registry.newMutableRegistry(
            "string",
            Resource::of
    );
    public static final Registry<Spungus> SPUNGUS_REGISTRY = Registry.newRegistry("spungus");

    static {
        ResourceType.REGISTRY.lock();
        UnloadableStringLoader stringOpener = new UnloadableStringLoader(new RuntimeResourceLoaderContext<>(RELOADABLE_STRING_REGISTRY));
        RELOADABLE_STRING_RESOURCE_TYPE = ResourceType.registerType(new ResourceType<>(
                RegistryIdentifier.simpleIdOf("reloadable_strings"),
                stringOpener,
                RELOADABLE_STRING_REGISTRY,
                ".txt"
        ));
        SpungusLoader spungusOpener = new SpungusLoader(new ResourceLoaderContext<>(SPUNGUS_REGISTRY));
        SPUNGUS_RESOURCE_TYPE = ResourceType.registerType(new ResourceType<>(
                RegistryIdentifier.simpleIdOf("spungus"),
                spungusOpener,
                SPUNGUS_REGISTRY,
                ".spungus"
        ));
    }
}
