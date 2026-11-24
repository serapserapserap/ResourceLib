package serap.io.loading.loader;

import org.jetbrains.annotations.NotNull;
import serap.io.loading.MergingThreadManager;
import serap.io.loading.loader.context.LoaderContext;
import serap.io.resource.accessor.ResourceAccessor;
import serap.io.resource.type.ResourceType;

import java.io.IOException;
import java.io.InputStream;

/**
 * a resource loader targeted at loading simple files that will never be reloaded and will not require dependencies
 * to be used
 * @param <C> the context type
 * @param <T> the object type
 */
public abstract class Loader<T, C extends LoaderContext<T>> implements ResourceOpener {
    protected Loader(C context) {
        this.loaderContext = context;
    }

    private final C loaderContext;

    protected C getLoaderContext() {
        return this.loaderContext;
    }

    protected abstract T loadFromStream(@NotNull InputStream stream);


    @Override
    public void open(MergingThreadManager mergingThreadManager, @NotNull ResourceAccessor accessor) {
        mergingThreadManager.submit(accessor.getIdentifier(), this.getLoaderContext(), () -> {
            try(InputStream stream = accessor.openAsStream()) {

                if(stream == null) {
                    throw new IllegalStateException("provided accessor does not supply a non-null input stream");
                }

                return this.loadFromStream(stream);

            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

}
