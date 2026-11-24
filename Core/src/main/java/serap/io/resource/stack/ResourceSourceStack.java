package serap.io.resource.stack;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import serap.io.resource.source.ResourceSource;

import java.util.Arrays;
import java.util.Iterator;

public class ResourceSourceStack implements ResourceStack {
    private final ObjectArrayList<ResourceSource> sources = new ObjectArrayList<>();

    public ResourceSourceStack(ResourceSource... initialSources) {
        sources.addAll(Arrays.asList(initialSources));
    }

    @Override
    public void addSource(ResourceSource resourceSource) {
        sources.add(resourceSource);
    }

    @NotNull
    @Override
    public Iterator<ResourceSource> iterator() {
        return sources.iterator();
    }
}
