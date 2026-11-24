package serap.io.resource.extension;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import serap.io.resource.type.ResourceType;

public interface FileExtension {
    /**
     * @return the string form of the extension
     */
    String getExtension();
    ResourceType<?> getResourceType();
    default boolean is(FileExtension other) {
        if(this == other) return true;
        //if the types the same we mark it as being the same
        //we do this because with glsl files, they can have the extension
        //.frag or .glsl and both function the same
        return this.getResourceType() == other.getResourceType();
    }

    @NotNull
    @Contract(pure = true)
    static FileExtension of(String extension, ResourceType<?> type) {
        return new FileExtension() {
            @Override
            public String getExtension() {
                return extension;
            }

            @Override
            public ResourceType<?> getResourceType() {
                return type;
            }
        };
    }
}

