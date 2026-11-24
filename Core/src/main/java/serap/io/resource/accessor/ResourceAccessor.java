package serap.io.resource.accessor;

import serap.io.resource.identifier.ResourceIdentifier;

import java.io.InputStream;

public interface ResourceAccessor {
    InputStream openAsStream();
    ResourceIdentifier getIdentifier();
}
