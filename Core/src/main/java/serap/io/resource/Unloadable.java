package serap.io.resource;

/**
 * the purpose of this interface is to represent a class that may or may not require freeing (similar to {@link AutoCloseable})

 * in most cases it's not required, but for cases like gl/vk objects where you have to manually free it

 * this should be used to control that, to ensure that you properly free it (and also to ensure that it's been properly created before use)
 */
public interface Unloadable {
    boolean isLoaded();
    void unload();
    default void unloadIfLoaded() {
        if(this.isLoaded()) this.unload();
    }
}
