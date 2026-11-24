package serap.processor;

import serap.annotations.Dependency;
import serap.annotations.EnsureLoaded;
import serap.annotations.ProjectNamespace;
import serap.annotations.Asset;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;
@SupportedAnnotationTypes({
        "serap.annotations.EnsureLoaded",
        "serap.annotations.Resource",
        "serap.annotations.ProjectNamespace"
})
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class ResourceLibAnnotationProcessor extends AbstractProcessor {

    private boolean generated = false;
    private String namespace;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if(generated) return false;
        Set<? extends Element> namespaceAnnotations =  roundEnv.getElementsAnnotatedWith(ProjectNamespace.class);
        boolean setNamespace = false;
        for (Element namespaceAnnotationElement : namespaceAnnotations) {
            if(setNamespace) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.WARNING, "Project namespace is already set as " + this.namespace + ", skipping", namespaceAnnotationElement
                );
            }
            setNamespace = true;
            this.namespace = namespaceAnnotationElement.getAnnotation(ProjectNamespace.class).value();
        }

        if(!setNamespace) return false;

        Set<? extends Element> resources = roundEnv.getElementsAnnotatedWith(Asset.class);
        Set<? extends Element> ensureLoads = roundEnv.getElementsAnnotatedWith(EnsureLoaded.class);

        String fileName = this.getFileName();
        String packageName = "resourcelib.generated";

        try {
            JavaFileObject file = processingEnv.getFiler().createSourceFile(packageName + "." + fileName);

            Writer writer = file.openWriter();
            String fileContents = this.generateJarSource(fileName, packageName, resources, ensureLoads);
            writer.write(fileContents);
            writer.close();

        } catch (IOException e) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR, "failed to generate initializer: " + fileName +"\n" + e.getMessage()
            );
        }
        return this.generated = true;
    }

    private String generateJarSource(String fileName, String pak, Set<? extends Element> resources, Set<? extends Element> ensureLoads) {
        String pckg = "package " + pak + ";\n\n";
        String top = """
                import serap.io.ResourceLib;
                import serap.io.discovery.SearchItemContext;
                import serap.io.discovery.SearchItemStorage;
                import serap.io.ResourceManager;
                import serap.io.resource.identifier.ResourceIdentifier;
                
                public class""" + " " + fileName + " " +
                """ 
                        {
                            /**
                             * adds the identifiers collected from the {@link serap.annotations.Resource} and {@link serap.annotations.EnsureLoaded}
                             * annotations to the supplied {@link SearchItemStorage}
                             * @param storage the storage to add the identifiers to
                             */
                            public static void addPathsToStorage(SearchItemStorage storage) {
                                storage.beginSection(\"""" + this.namespace + "\");" ;
        String bottom = """
                \n    }
                
                    /**
                     * creates a {@link SearchItemStorage} and adds the identifiers collected from the {@link serap.annotations.Resource}
                     * and {@link serap.annotations.EnsureLoaded} annotations
                     * @return the created {@link SearchItemStorage}
                     */
                    public static SearchItemStorage createStorageAndAddPaths() {
                        SearchItemStorage storage = new SearchItemContext();
                        addPathsToStorage(storage);
                        return storage;
                    }
                
                    /**
                     * Registers resources to be loaded, and initializes resource lib with the created {@link SearchItemStorage}
                     * @return the created {@link  ResourceManager}
                     */
                    public static ResourceManager initialize() {
                        SearchItemStorage storage = createStorageAndAddPaths();
                        return ResourceLib.initResourceLib(storage);
                    }
                
                    /**
                     * adds the identifiers collected from the {@link serap.annotations.Resource} and {@link serap.annotations.EnsureLoaded}
                     * annotations to the supplied {@link SearchItemStorage} then initializes resource lib with that {@link SearchItemStorage}
                     * @param storage the storage to add the identifiers to and initialize with
                     * @return the created {@link ResourceManager}
                     */
                    public static ResourceManager initializeWith(SearchItemStorage storage) {
                        addPathsToStorage(storage);
                        return ResourceLib.initResourceLib(storage);
                    }
                }
                """;
        String addCalls = "";
        for (Element resource : resources) {
            Asset r = resource.getAnnotation(Asset.class);
            Dependency[] dependencies = r.dependencies();
            String dependencyString = "";
            for (Dependency dependency : dependencies) {
                dependencyString += String.format(", ResourceIdentifier.of(\"%s\", \"%s\")", dependency.namespace(), dependency.identifier());
            }
            addCalls += String.format("\n        storage.add(\"%s\", %s%s);", r.identifier(), r.alwaysLoaded(), dependencyString);
        }
        for (Element ensureLoad : ensureLoads) {
            EnsureLoaded r = ensureLoad.getAnnotation(EnsureLoaded.class);
            addCalls += String.format("\n        storage.add(\"%s\", true);", r.value());
        }

        return pckg + top + addCalls + bottom;
    }

    private String getFileName() {
        String firstChar = String.valueOf(this.namespace.charAt(0));
        return this.namespace.toLowerCase().replaceFirst(firstChar.toLowerCase(), firstChar.toUpperCase()) + "ResourceInitializer";
    }

}
