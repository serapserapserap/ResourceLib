package serap.io.resource;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import serap.core.pointer.MutablePointer;
import serap.io.ResourceManager;
import serap.io.resource.extension.FileExtension;
import serap.io.resource.identifier.ResourceIdentifier;
import serap.io.resource.type.ResourceType;

import java.util.Arrays;

public class Resource<T extends Unloadable> implements MutablePointer<T> {

    //the stored resource
    protected T resource = null;
    @Setter
    @Getter
    protected boolean alwaysNeeded = false;
    //the resources that this depends on
    @Getter
    protected Dependency[] dependencies = null;

    public static <T extends Unloadable> Resource<T> of(T val) {
        return new Resource<>(val);
    }

    public static <T extends Unloadable> Resource<T> of() {
        return new Resource<>();
    }

    /**
     * creates a new resource
     * @param initialValue the value that the resource should hold before it's actual value is loaded
     */
    protected Resource(@Nullable T initialValue) {
        this.resource = initialValue;
    }

    /**
     * creates a new resource
     */
    protected Resource() {
        this(null);
    }

    /**
     * @return {@code true} if the resource depends on other resources to work, {@code false} if it doesn't
     */
    public boolean hasDependencies() {
        return this.dependencies != null;
    }

    /**
     * updates the resource in use
     * @param newResource the resource to start using
     */
    protected void updateResource(T newResource) {
        this.resource = newResource;
    }

    /**
     * marks the resource as depending on another resource
     * @param resource the resource that this resource depends on
     * @param identifier the identifier of the resource it depends on
     * @param extension the file extension of the resource it depends on
     */
    public void dependsOn(Resource<?> resource, ResourceIdentifier identifier, FileExtension extension) {
        this.dependsOn(new Dependency(resource, identifier, extension));
    }

    public void dependsOn(Dependency dependency) {
        if(!this.hasDependencies()) {
            this.dependencies = new Dependency[1];
            this.dependencies[0] = dependency;
            return;
        }

        int putIndex = this.dependencies.length;
        this.dependencies = Arrays.copyOf(
                this.dependencies,
                putIndex + 1
        );
        this.dependencies[putIndex] = dependency;
    }

    @Override
    @Nullable
    public T get() {
        return this.resource;
    }

    @Override
    public void pointTo(T neu) {
        this.resource = neu;
    }

    public boolean isReady() {

        if(this.hasDependencies()) {
            for (Dependency dependency : this.dependencies) {
                if(!dependency.isReady()) {
                    return false;
                }
            }
        }

        if(this.resource == null) {
            return false;
        }

        return this.resource.isLoaded();
    }

    public record Dependency(Resource<?> resource, ResourceIdentifier identifier, FileExtension extension) {
        public boolean isReady() {
            return resource.isReady();
        }

        public ResourceType<?> getResourceType() {
            return extension.getResourceType();
        }
    }
}
