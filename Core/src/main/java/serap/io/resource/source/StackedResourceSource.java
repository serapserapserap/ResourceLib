package serap.io.resource.source;

import org.jetbrains.annotations.NotNull;
import serap.io.resource.accessor.ResourceAccessor;
import serap.io.resource.identifier.ResourceIdentifier;
import serap.io.resource.stack.ResourceStack;

public final class StackedResourceSource implements ResourceSource {
    private final ResourceStack resourceStack;

    public StackedResourceSource(ResourceStack stack) {
        this.resourceStack = stack;
    }

    @Override
    public ResourceAccessor getAccessor(ResourceIdentifier identifier) {
        for(ResourceSource source : resourceStack) {
            if(!source.hasResource(identifier)) continue;

            return source.getAccessor(identifier);
        }

        return null;
    }

    @Override
    public boolean hasResource(@NotNull ResourceIdentifier identifier) {
        for(ResourceSource source : resourceStack) {
            if(source.hasResource(identifier)) {
                return true;
            }
        }
        return false;
    }
}
