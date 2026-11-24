package serap.io.resource.source;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import serap.io.resource.accessor.ClasspathResourceAccessor;
import serap.io.resource.accessor.ResourceAccessor;
import serap.io.resource.identifier.ResourceIdentifier;

public record ClasspathResourceSource(ClassLoader targetClassLoader) implements ResourceSource {

    @NotNull
    @Contract("_ -> new")
    @Override
    public ResourceAccessor getAccessor(ResourceIdentifier identifier) {
        return new ClasspathResourceAccessor(identifier, this);
    }

    public boolean hasResource(@NotNull ResourceIdentifier identifier) {
        return ClasspathResourceSource.class.getResource(identifier.toAbsoluteClasspathString()) != null;
    }
}
