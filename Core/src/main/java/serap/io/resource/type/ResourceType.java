package serap.io.resource.type;

import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import serap.core.pointer.MutablePointer;
import serap.core.registry.Registry;
import serap.core.registry.RegistryIdentifier;
import serap.io.loading.loader.ResourceOpener;
import serap.io.resource.extension.FileExtension;

public final class ResourceType<L extends ResourceOpener> {
    public static final Registry<ResourceType<?>> REGISTRY = Registry.newRegistry("ResourceType");

    public static FileExtension matchExtension(String extensionString) {

        int hash = extensionString.toLowerCase().hashCode();
        ResourceType<?> type = ResourceType.REGISTRY.findFirstWhere((id, resourceType) -> resourceType.hasHash(hash));

        if(type == null) {
            throw new IllegalStateException("No resource type in type registry contained the extension '" + extensionString + "'");
        }

        return type.fromHash(hash);
    }

    private final FileExtension[] extensions;
    private final int[] extensionHashes;
    private final RegistryIdentifier identifier;
    @Getter
    private final L resourceOpener;
    @Getter
    private final Registry<?> registry;
    @Getter
    private final boolean registryIsMutable;

    @SuppressWarnings("unchecked")
    public Registry.Mutable<?, ? super MutablePointer<?>> getRegistryAsMutable() {
        if(!this.isRegistryIsMutable()) throw new IllegalStateException("Cannot get registry " + this.getRegistry().getTag() + " as mutable as it's not a mutable registry");

        return (Registry.Mutable<?, ? super MutablePointer<?>>) this.getRegistry();
    }

    public static ResourceType<?> registerType(ResourceType<?> type) {
        ResourceType.REGISTRY.register(type.identifier, type);
        return type;
    }

    public ResourceType(RegistryIdentifier identifier, L loader, Registry<?> registry, @NotNull String @NotNull ... validExtensions) {
        this.resourceOpener = loader;
        this.registryIsMutable = registry instanceof Registry.Mutable<?,?>;
        this.registry = registry;
        this.identifier = identifier;
        this.extensions = new FileExtension[validExtensions.length];
        this.extensionHashes = new int[validExtensions.length];
        for (int i = 0; i < validExtensions.length; i++) {
            this.extensions[i] = FileExtension.of(validExtensions[i].toLowerCase(), this);
            this.extensionHashes[i] = validExtensions[i].toLowerCase().hashCode();
        }
    }

    /**
     * checks if the supplied extension is for this resource type
     * @param extension the extension to test
     * @return if this resource type is the correct one for the supplied file extension
     */
    public boolean isCorrectType(@NotNull FileExtension extension) {
        return extension.getResourceType() == this;
    }

    @Contract(pure = true)
    public boolean containsExtension(@NotNull String extension) {
        int hash = extension.hashCode();
        return this.hasHash(hash);
    }

    private boolean hasHash(int hash) {
        for (int extensionHash : this.extensionHashes) {
            if(hash == extensionHash) return true;
        }
        return false;
    }

    @NotNull
    private FileExtension fromHash(int hash) {
        for (int i = 0; i < this.extensionHashes.length; i++) {
            if(this.extensionHashes[i] == hash) return this.extensions[i];
        }
        throw new IllegalStateException("hash provided was not present in hash list");
    }
}
