package serap.testimpl;

import serap.io.resource.Unloadable;

public class Spungus implements Unloadable {
    public Spungus(String s) {
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
