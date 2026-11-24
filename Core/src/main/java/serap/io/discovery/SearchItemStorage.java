package serap.io.discovery;

import lombok.Setter;
import serap.io.ResourceLib;
import serap.io.resource.Resource;
import serap.io.resource.extension.FileExtension;
import serap.io.resource.identifier.ResourceIdentifier;
import serap.io.resource.type.ResourceType;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

public interface SearchItemStorage {

    Iterable<SearchItem> allResources();
    Iterable<SearchItem> alwaysNeededResources();
    void forEachPath(Consumer<SearchItem> consumer);
    void beginSection(String namespace);
    void endSection();

    default void inSection(String namespace, Consumer<? super SearchItemStorage> consumer) {
        this.beginSection(namespace);
        consumer.accept(this);
        this.endSection();
    }

    /**
     * @return the total number of resources
     */
    int totalResources();

    /**
     * @return the total number of allResources that must be loaded at all times
     */
    int totalReloadItems();

    void modifyToAlwaysNeeded(SearchItem item);

    boolean isEditingSection();
    String getCurrentNamespaceSection();

    default void add(String identifier, boolean forceLoad, ResourceIdentifier... dependencies) {
        if(!isEditingSection()) {
            throw new IllegalStateException("Create a search section with ResourceSearchContext.beingSection(String) before adding a search path");
        }

        this.add(ResourceIdentifier.of(this.getCurrentNamespaceSection(), identifier), forceLoad, dependencies);
    }

    default void add(String identifier, ResourceIdentifier... dependencies) {
        this.add(identifier, false, dependencies);
    }

    default void addForceLoad(String identifier, ResourceIdentifier... dependencies) {
        this.add(identifier, true, dependencies);
    }

    default void addForceLoad(ResourceIdentifier identifier, ResourceIdentifier... dependencies) {
        this.add(identifier, true, dependencies);
    }

    void add(ResourceIdentifier identifier, boolean forceLoad, ResourceIdentifier... dependencies);

    default void add(ResourceIdentifier identifier, ResourceIdentifier... dependencies) {
        this.add(identifier, false, dependencies);
    }

    default void process(SearchItemProcessor processor) {
        for (SearchItem item : this.allResources()) {
            processor.process(this, item);
        }
    }


    default void debugPaths() {
        ResourceLib.info("---- ALL PATHS ----");
        for(SearchItem item : this.allResources()) {
            ResourceLib.info(item.toString());
        }
        ResourceLib.info("---- PATHS ALWAYS LOADED ----");
        for(SearchItem item : this.alwaysNeededResources()) {
            ResourceLib.info(item.toString());
        }
        ResourceLib.info("-------------------");
    }


    final class SearchItem {
        private final ResourceIdentifier identifier;
        @Setter
        private boolean isAlwaysNeeded;
        private final ResourceIdentifier[] dependencies;
        private FileExtension fileExtension;

        public ResourceType<?> getResourceType() {
            FileExtension fileExtension = this.getFileExtension();
            return fileExtension.getResourceType();
        }

        public FileExtension getFileExtension() {
            if(this.fileExtension != null) {
                return this.fileExtension;
            }

            return this.fileExtension = this.identifier.getFileExtension();
        }

        public SearchItem(ResourceIdentifier identifier, boolean isAlwaysNeeded, ResourceIdentifier... dependencies) {
            this.identifier = identifier;
            this.isAlwaysNeeded = isAlwaysNeeded;
            this.dependencies = dependencies;
        }

        public boolean hasDependencies() {
                return dependencies.length == 0;
            }

        public ResourceIdentifier identifier() {
            return identifier;
        }

        public boolean isAlwaysNeeded() {
            return isAlwaysNeeded;
        }

        public ResourceIdentifier[] dependencies() {
            return dependencies;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (SearchItem) obj;
            return Objects.equals(this.identifier, that.identifier) &&
                    this.isAlwaysNeeded == that.isAlwaysNeeded &&
                    Objects.equals(this.dependencies, that.dependencies);
        }

        @Override
        public int hashCode() {
            return Objects.hash(identifier, isAlwaysNeeded, Arrays.hashCode(dependencies));
        }

        @Override
        public String toString() {
            return "SearchItem[" +
                    "identifier=" + identifier + ", " +
                    "isAlwaysNeeded=" + isAlwaysNeeded + ", " +
                    "dependencies=" + Arrays.toString(dependencies) + ']';
        }

        }
}
