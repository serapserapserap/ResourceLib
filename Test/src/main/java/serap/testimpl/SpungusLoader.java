package serap.testimpl;

import org.jetbrains.annotations.NotNull;
import serap.core.registry.Registry;
import serap.io.loading.loader.Loader;
import serap.io.loading.loader.context.ResourceLoaderContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class SpungusLoader extends Loader<Spungus, ResourceLoaderContext<Spungus, Registry<Spungus>>> {
    protected SpungusLoader(ResourceLoaderContext<Spungus, Registry<Spungus>> context) {
        super(context);
    }

    @Override
    protected Spungus loadFromStream(@NotNull InputStream stream) {
        try {
            String res = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return new Spungus(res);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
