package serap.io.resource.identifier;

import serap.core.registry.RegistryIdentifier;
import serap.io.resource.extension.FileExtension;
import serap.io.resource.type.ResourceType;

import java.io.File;
import java.nio.file.Path;
import java.util.regex.Pattern;

public interface ResourceIdentifier extends RegistryIdentifier {

    /**
     * the separator for paths
     */
    String IDENTIFIER_PATH_SEPARATOR = "/";
    /**
     * the separator between the namespace and the identifier {@code namespace::path/to/asset.png}
     */
    String NAMESPACE_IDENTIFIER_SEPARATOR = "::";

    String getNamespace();
    String getIdentifier();

    default FileExtension getFileExtension() {
        return ResourceType.matchExtension(this.getFileExtensionString());
    }

    default String getFileExtensionString() {
        return this.getIdentifier().substring(this.getIdentifier().lastIndexOf('.'));
    }

    default String getNamespacedIdentifier()  {
        return String.format("%s%s%s", this.getNamespace(), NAMESPACE_IDENTIFIER_SEPARATOR, this.getIdentifier());
    }

    /**
     * @return the result of {@link ResourceIdentifier#toClasspathString()} just starting with a {@code /}
     */
    default String toAbsoluteClasspathString() {
        return '/' + toClasspathString();
    }

    @SuppressWarnings("all")
    default String toClasspathString() {
        return (IDENTIFIER_PATH_SEPARATOR == "/" ? this.getIdentifier() : this.getIdentifier().replace(IDENTIFIER_PATH_SEPARATOR, "/"));
    }

    default String toPathString() {
        return this.getIdentifier().replace(IDENTIFIER_PATH_SEPARATOR, File.pathSeparator);
    }

    default Path asPath(Path source) {
        String[] subPaths = this.getIdentifier().split(Pattern.quote(IDENTIFIER_PATH_SEPARATOR));

        for (String subPath : subPaths) {
            source = source.resolve(subPath);
        }

        return source;
    }

    static HashResourceIdentifier of(String namespace, String identifier) {
        return new HashResourceIdentifier(namespace, identifier);
    }
}
