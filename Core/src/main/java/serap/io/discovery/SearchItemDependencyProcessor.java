package serap.io.discovery;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import serap.core.pointer.MutablePointer;
import serap.core.registry.Registry;
import serap.io.ResourceLib;
import serap.io.resource.Resource;
import serap.io.resource.identifier.ResourceIdentifier;
import serap.io.resource.type.ResourceType;

public class SearchItemDependencyProcessor implements SearchItemProcessor {

    @Contract(value = " -> new", pure = true)
    public static @NotNull SearchItemProcessor createNew() {
        return new SearchItemDependencyProcessor();
    }

    @Override
    public void process(SearchItemStorage storage, SearchItemStorage.SearchItem item) throws UnsupportedOperationException {
        boolean alwaysNeeded = item.isAlwaysNeeded();

        if(alwaysNeeded) return;

        ResourceIdentifier resourceIdentifier = item.identifier();


        ResourceType<?> type = item.getResourceType();

        if(!type.isRegistryIsMutable()) {
            return;
        }

        Registry.Mutable<?, ?> resourceRegistry = type.getRegistryAsMutable();
        MutablePointer<?> pointer = resourceRegistry.getOrRegisterIfAbsent(resourceIdentifier);

        if(pointer instanceof Resource<?> resource && resource.isAlwaysNeeded()) {
            storage.modifyToAlwaysNeeded(item);
        }
    }
}
