package serap;

import resourcelib.generated.ExampleResourceInitializer;
import serap.annotations.Asset;
import serap.annotations.Dependency;
import serap.annotations.EnsureLoaded;
import serap.annotations.ProjectNamespace;
import serap.io.ResourceLib;
import serap.io.ResourceManager;
import serap.io.resource.Resource;
import serap.io.resource.identifier.ResourceIdentifier;
import serap.io.resource.source.ClasspathResourceSource;
import serap.testimpl.Spungus;
import serap.testimpl.TestUtils;
import serap.testimpl.UnloadableString;

import java.util.function.Consumer;

@ProjectNamespace("example")
@EnsureLoaded("different/file3.spungus")
public class Main {

    private static ResourceManager resourceManager;

    @Asset(
            identifier = "file1.txt",
            dependencies = {
                    @Dependency(
                            namespace = "example",
                            identifier = "file2.txt"
                    )
            }
    )
    private int i0 = 1;

    @Asset(
            identifier = "file2.txt",
            alwaysLoaded = false
    )
    private int i2 = 1;

    private int i1 = 1;

    static void main() {
        Consumer<Object> out = IO::println;
        ResourceLib.addLoggingOutput(out, out, out, out);

        //make the stupid static block run
        new TestUtils();

        //initializer is automatically generated according to the annotations
        resourceManager = ExampleResourceInitializer.initialize();
        resourceManager.addBaseSource(new ClasspathResourceSource(Main.class.getClassLoader()));
        resourceManager.initiallyLoadResources();

        boolean isRunning = true;

        Resource<UnloadableString> resource = TestUtils.RELOADABLE_STRING_REGISTRY.getOrRegisterIfAbsent(ResourceIdentifier.of("example", "file1.txt"));
        var resource1 = TestUtils.RELOADABLE_STRING_REGISTRY.getOrRegisterIfAbsent(ResourceIdentifier.of("example", "file2.txt"));

        boolean printedHello = false;
        while(isRunning) {
            label: {
                if(resourceManager.doneLoading()) {
                    if(!printedHello) {
                        UnloadableString us = resource.get();
                        if(us == null) {
                            break label;
                        }
                        String s = us.value;
                        ResourceLib.info(s);

                        printedHello = true;
                    } else {
                        UnloadableString us = resource1.get();
                        if(us == null) {
                            break label;
                        }
                        String s = us.value;
                        ResourceLib.info(s);
                        us.unload();
                        ResourceLib.info(us.value);
                        isRunning = false;

                        Spungus spungus = TestUtils.SPUNGUS_REGISTRY.get(ResourceIdentifier.of("example", "different/file3.spungus"));
                        if(spungus == null) {
                            throw new IllegalStateException("the spungus is null");
                        }

                        ResourceLib.info(spungus.value);
                    }
                }
            }

            resourceManager.pollResourceLoading();
        }
        //cleanup after we're done with it
        resourceManager.cleanup();
        ResourceLib.info("main execution finished");
    }
}
