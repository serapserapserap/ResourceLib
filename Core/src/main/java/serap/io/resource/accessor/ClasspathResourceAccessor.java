package serap.io.resource.accessor;

import org.jetbrains.annotations.NotNull;
import serap.io.ResourceLib;
import serap.io.resource.identifier.ResourceIdentifier;
import serap.io.resource.source.ClasspathResourceSource;

import java.io.InputStream;

public final class ClasspathResourceAccessor implements ResourceAccessor {
    private final ResourceIdentifier identifier;
    private final ClassLoader targetClassLoader;

    private ClasspathResourceAccessor(ResourceIdentifier identifier, ClassLoader classLoader) {
        this.identifier = identifier;
        this.targetClassLoader = classLoader;
    }

    public ClasspathResourceAccessor(ResourceIdentifier identifier, @NotNull ClasspathResourceSource source) {
        this(identifier, source.targetClassLoader());
    }

    @Override
    public InputStream openAsStream() {
        return this.targetClassLoader.getResourceAsStream(this.identifier.toClasspathString());
    }

    @Override
    public ResourceIdentifier getIdentifier() {
        return this.identifier;
    }
}
