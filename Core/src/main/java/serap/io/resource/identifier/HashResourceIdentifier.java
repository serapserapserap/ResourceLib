package serap.io.resource.identifier;

import org.jetbrains.annotations.NotNull;
import serap.core.registry.RegistryIdentifier;

public class HashResourceIdentifier implements ResourceIdentifier {
    private final String resourceNamespace;
    private final String resourceIdentifier;
    private final int uniqueHash;

    public HashResourceIdentifier(String namespace, @NotNull String identifier) {
        this.resourceNamespace = namespace;
        this.resourceIdentifier = identifier.toLowerCase();
        this.uniqueHash = (this.resourceNamespace + this.resourceIdentifier).hashCode();
    }

    public String getIdentifier() {
        return this.resourceIdentifier;
    }

    public String getNamespace() {
        return this.resourceNamespace;
    }

    private boolean hashesMatch(HashResourceIdentifier identifier) {
        return identifier.uniqueHash == this.uniqueHash;
    }

    @Override
    public boolean is(RegistryIdentifier registryIdentifier) {
        if(this == registryIdentifier) return true;
        if(registryIdentifier instanceof HashResourceIdentifier identifier) return identifier.hashesMatch(this);
        return false;
    }

    @Override
    public String toString() {
        return String.format(
                "ResourceIdentifier[namespace=%s, identifier=%s]",
                this.resourceNamespace,
                this.resourceIdentifier
        );
    }
}

