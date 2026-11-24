package serap.testimpl;

import serap.io.resource.Unloadable;

public class UnloadableString implements Unloadable {

    public UnloadableString(String s) {
        this.value = s;
    }

    public String value = null;

    @Override
    public boolean isLoaded() {
        return this.value != null;
    }

    @Override
    public void unload() {
        this.value = null;
    }
}
