package serap.io.discovery;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import serap.io.resource.identifier.ResourceIdentifier;

import java.util.function.Consumer;

public class SearchItemContext implements SearchItemStorage {
    private final ObjectArrayList<SearchItem> allSearchItems = new ObjectArrayList<>();
    private final ObjectArrayList<SearchItem> alwaysRequiredSearchItems = new ObjectArrayList<>();
    @Getter
    private String currentNamespaceSection = null;

    public void beginSection(String namespace) {
        this.currentNamespaceSection = namespace;
    }

    public void endSection() {
        this.currentNamespaceSection = null;
    }

    @Override
    public int totalResources() {
        return allSearchItems.size();
    }

    @Override
    public int totalReloadItems() {
        return alwaysRequiredSearchItems.size();
    }

    @Override
    public void modifyToAlwaysNeeded(SearchItem item) {
        if(item.isAlwaysNeeded()) {
            return;
        }
        item.setAlwaysNeeded(true);
        this.alwaysRequiredSearchItems.add(item);
    }

    public boolean isEditingSection() {
        return this.currentNamespaceSection != null;
    }

    @Override
    public void add(ResourceIdentifier identifier, boolean forceLoad, ResourceIdentifier... dependencies) {
        SearchItem item;
        this.allSearchItems.add(item = new SearchItem(identifier, forceLoad, dependencies));
        if(forceLoad) {
            this.alwaysRequiredSearchItems.add(item);
        }
    }


    @Override
    public Iterable<SearchItem> allResources() {
        return allSearchItems;
    }

    @Override
    public Iterable<SearchItem> alwaysNeededResources() {
        return alwaysRequiredSearchItems;
    }

    @Override
    public void forEachPath(Consumer<SearchItem> identifierConsumer) {
        for (SearchItem loadIdentifier : this.allSearchItems) {
            identifierConsumer.accept(loadIdentifier);
        }
    }
}
