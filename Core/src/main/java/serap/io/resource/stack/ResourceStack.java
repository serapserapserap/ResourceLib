package serap.io.resource.stack;

import serap.io.resource.source.ResourceSource;

public interface ResourceStack extends Iterable<ResourceSource> {
    /**
     * Adds a resource source to the resource stack
     * @param resourceSource the source to be added
     */
    void addSource(ResourceSource resourceSource);
}
