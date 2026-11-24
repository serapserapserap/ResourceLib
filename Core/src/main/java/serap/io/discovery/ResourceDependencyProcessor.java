package serap.io.discovery;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import serap.core.pointer.MutablePointer;
import serap.core.registry.Registry;
import serap.io.ResourceLib;
import serap.io.resource.Resource;
import serap.io.resource.extension.FileExtension;
import serap.io.resource.identifier.ResourceIdentifier;
import serap.io.resource.type.ResourceType;

public class ResourceDependencyProcessor implements SearchItemProcessor {
    @Contract(value = " -> new", pure = true)
    public static @NotNull SearchItemProcessor createNew() {
        return new ResourceDependencyProcessor();
    }

    @Contract("_ -> new")
    private static Resource.@NotNull Dependency getDependencyInformation(@NotNull ResourceIdentifier resourceIdentifier) throws UnsupportedOperationException {
        FileExtension extension = resourceIdentifier.getFileExtension();
        ResourceType<?> type = extension.getResourceType();

        if(!type.isRegistryIsMutable()) {
            throw new UnsupportedOperationException(
                    "Resource " + resourceIdentifier.getNamespacedIdentifier() + " does not support being added as a dependency for another resource"
            );
        }

        Registry.Mutable<?, ?> registry = type.getRegistryAsMutable();
        MutablePointer<?> pointer = registry.getOrRegisterIfAbsent(resourceIdentifier);
        Resource<?> r;
        try {
            r = (Resource<?>) pointer;
        } catch (ClassCastException e) {
            throw new UnsupportedOperationException(
                    "Resource " + resourceIdentifier.getNamespacedIdentifier() + " does not support being added as a dependency for another resource"
            );
        }
        return new Resource.Dependency(r, resourceIdentifier, extension);
    }

    @Override
    public void process(SearchItemStorage storage, SearchItemStorage.@NotNull SearchItem item) throws UnsupportedOperationException {
        ResourceIdentifier[] dependencies = item.dependencies();
        boolean alwaysNeeded = item.isAlwaysNeeded();
        ResourceIdentifier resourceIdentifier = item.identifier();

        boolean dependenciesRequested = dependencies.length != 0;

        ResourceType<?> type = item.getResourceType();

        if(!type.isRegistryIsMutable()) {
            if(dependenciesRequested) {
                ResourceLib.warn(
                        "Resource dependency loading skipped for resource " + resourceIdentifier.getNamespacedIdentifier() +
                                " as it does not support resource dependencies"
                );
            }
            return;
        }

        Registry.Mutable<?, ?> resourceRegistry = type.getRegistryAsMutable();
        MutablePointer<?> pointer = resourceRegistry.getOrRegisterIfAbsent(resourceIdentifier);
        if(!alwaysNeeded && !dependenciesRequested) return;
        try {
            Resource<?> resource = (Resource<?>) pointer;

            if(alwaysNeeded) {
                resource.setAlwaysNeeded(true);
            }

            if(dependenciesRequested) {

                for(ResourceIdentifier identifier : dependencies) {
                    Resource.Dependency dependency = getDependencyInformation(identifier);

                    resource.dependsOn(dependency);
                    if(alwaysNeeded) {
                        //if the root resource is always needed, that means that this is always needed
                        dependency.resource().setAlwaysNeeded(true);
                    }
                }

            }

        } catch (ClassCastException e) {
            String baseMessage = "Resource" + resourceIdentifier.getNamespacedIdentifier() + " does not support ";
            String errorMessage = "";

            if(alwaysNeeded) {
                errorMessage += baseMessage + "being marked as always needed";
            }

            if(dependenciesRequested) {
                errorMessage += baseMessage + "dependencies";
            }

            throw new UnsupportedOperationException(errorMessage, e);
        }
    }
}
