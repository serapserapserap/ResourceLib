package serap.io.resource.source;

import org.jetbrains.annotations.NotNull;
import serap.io.resource.accessor.ResourceAccessor;
import serap.io.resource.identifier.ResourceIdentifier;

public interface ResourceSource {
    /**
     * generates an accessor for the given identifier
     * @param identifier the identifier to generate an accessor for
     * @return the generated {@link ResourceAccessor}
     */
    ResourceAccessor getAccessor(ResourceIdentifier identifier);

    /**
     * checks if the source has a resource associated with the supplied identifier
     * @param identifier the identifier to check
     * @return {@code true} if the source has the resource
     * {@code false} if it does not
     */
    boolean hasResource(@NotNull ResourceIdentifier identifier);
}
