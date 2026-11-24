package serap.testimpl;

import serap.io.loading.loader.Loader;
import serap.io.loading.loader.context.RuntimeResourceLoaderContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class UnloadableStringLoader extends Loader<UnloadableString, RuntimeResourceLoaderContext<UnloadableString>> {
    protected UnloadableStringLoader(RuntimeResourceLoaderContext<UnloadableString> context) {
        super(context);
    }

    @Override
    protected UnloadableString loadFromStream( InputStream stream) {
        try {
            String res = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return new UnloadableString(res);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
