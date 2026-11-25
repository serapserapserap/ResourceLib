# ResourceLib

A small game-oriented async resource loading library that I made to speed up the process of making games without a game engine

## Features
- Async resource loading (during initialization and runtime)
- Allows for custom defining of resources & resource loaders
- Resource reloading
- A resource "stack" system that allows you to setup multiple sources of resources and then it resolves where to get each asset from according to that
- Annotation resource definition system that auto generates an initializer for your project

## Requirements
- Java 25

## Getting Started

To get started first you have to add resource lib to your project

### Adding to Project

#### Gradle
In your build.gradle, add jitpack to your repositories.
```gradle
repositories {
    mavenCentral()
    maven { url = 'https://jitpack.io' }
}
```
Then add ResourceLib's core to your dependencies
```gradle
dependencies {
    implementation 'com.github.serapserapserap.ResourceLib:Core:v1.0.3'
}
```
#### Maven
In your pom.xml, add jitpack to your repositories.
```maven
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```
Then add ResourceLib's core to your dependencies
```maven
<dependencies>
    <dependency>
        <groupId>com.github.serapserapserap.ResourceLib</groupId>
        <artifactId>Core</artifactId>
        <version>v1.0.3</version>
    </dependency>
</dependencies>
```

### Additional Tools

ResourceLib also has additional tools that help with the registering of resources. To add them to your project you do the following
#### Gradle
```gradle
dependencies {
    implementation 'com.github.serapserapserap.ResourceLib:Core:v1.0.3'
    implementation 'com.github.serapserapserap.ResourceLib:Annotations:v1.0.3'
    annotationProcessor 'com.github.serapserapserap.ResourceLib:AnnotationProcessor:v1.0.3'
}
```

#### Maven
```maven
<dependencies>
    <dependency>
        <groupId>com.github.serapserapserap.ResourceLib</groupId>
        <artifactId>Core</artifactId>
        <version>v1.0.3</version>
    </dependency>
    <dependency>
        <groupId>com.github.serapserapserap.ResourceLib</groupId>
        <artifactId>Annotations</artifactId>
        <version>v1.0.3</version>
    </dependency>
    <dependency>
        <groupId>com.github.serapserapserap.ResourceLib</groupId>
        <artifactId>AnnotationProcessor</artifactId>
        <version>v1.0.3</version>
    </dependency>
</dependencies>
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>com.github.serapserapserap.ResourceLib</groupId>
                        <artifactId>AnnotationProcessor</artifactId>
                        <version>v1.0.3</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```
### Using In Your Project 

How to get started with ResourceLib vary a bit depending on if you're using the annotations & annotation processor or not

#### Adding a logging output
This is optional but recommended, as otherwise you will get no logging feedback from the library. You can do this by simply running
```java
//if you have your own logging system you can set these to use that instead!!
Consumer<Object> info = (infoObject) -> System.out.printf("[ResourceLib/INFO]%s%n", infoObject.toString());
Consumer<Object> debug = (debugObject) -> System.out.printf("[ResourceLib/DEBUG]%s%n", debugObject.toString());
Consumer<Object> warn = (warnObject) -> System.out.printf("[ResourceLib/WARN]%s%n", warnObject.toString());
Consumer<Object> error = (errorObject) -> System.out.printf("[ResourceLib/ERROR]%s%n", errorObject.toString());

ResourceLib.addLoggingOutput(
        info,
        debug,
        warn,
        error
);
```

#### Defining Your Own Resource Types

This is somewhat complicated to concisely explain here, so if you don't understand there is a module with example implementations that you can find
[here](Test/src/main/java/serap/Main.java)

To define your own `ResourceType<?>` you must write a 'ResourceOpener' for it.
```java
public class ExampleLoader extends Loader<Example, ResourceLoaderContext<Example, Registry<Example>>> {
    protected ExampleLoader(ResourceLoaderContext<Example, Registry<Example>> context) {
        super(context);
    }

    @Override
    protected Example loadFromStream(InputStream stream) {
        //you write your implementation for its loading here...
    }
}
```

In addition to needing your own loader you need your own registry for it

```java
final Registry<Example> EXAMPLE_REGISTRY = Registry.newRegistry("example");
```

All together it looks like this

```java
//create the registry
final Registry<Example> EXAMPLE_REGISTRY = Registry.newRegistry("example");

//create the loader
ExampleLoader loader = new ExampleLoader(new ResourceLoaderContext<>(EXAMPLE_REGISTRY));

//create the resource type using registry & loader we just created
final ResourceType<?> EXAMPLE_TYPE = ResourceType.registerType(new ResourceType<>(
        RegistryIdentifier.simpleIdOf("example_type"),
        loader,
        EXAMPLE_REGISTRY,
        ".example" //this is the file extension(s) the resource type is targeted at
));
```

#### Registering Resources/Assets

This is where the two methods diverge a bit, 

without the annotations & annotation processor you do as follows

```java 
//create the search item storage
SearchItemStorage storage = new SearchItemContext();

//beginning a section just makes any SearchItemStorage#Add calls use the namespace you provide as their namespace
storage.beginSection("your_namespace_name");
//so this would have "your_namespace_name" as it's namespace
storage.add("path/to/your/resource.example");

//alternatively you can explicitly specify the namespace like so
//this would have "other_namespace_name" as it's namespace, and not "your_namespace_name"
storage.add(ResourceIdentifier.of("other_namespace_name", "path/to/you/resource.example"));

//there are a few other params you can play around with
//the boolean specifies if it always needs to be loaded or not
storage.add("otherResource.example", false);

//and finally this gives the item a dependency
//if the dependency is not force load, but what you're adding it to IS force load, the dependency will have its forceLoad set to true
storage.add("anotherResource.example", true, ResourceIdentifier.of("your_namespace_name", "otherResource.example"));

//finally we're done adding stuff
storage.endSection();
```

WITH it it's a lot simpler

```java
@ProjectNamespace("example")
@EnsureLoaded("nonReloadable.example") //ensure loaded is for resources that cannot be reloaded, meaning they don't support dependencies
public class Main {
    @Asset( //asset is for resources which CAN be reloaded, which DO support dependencies
            identifier = "reloadable.example",
            alwaysLoaded = true,
            dependencies = {
                    @Dependency(
                            namespace = "example",
                            identifier = "file2.txt"
                    )
            }
    )
    private Resource<Example> exampleResource = EXAMPLE_REGISTRY.getOrRegisterIfAbsent(ResourceIdentifier.of("example", "reloadable.example"));

    static void main() {
        //resource type creation stuff listed earlier...
        SearchItemStorage storage = ExampleResourceInitializer.createStorageAndAddPaths();
        //ExampleResourceInitializer is auto generated according to the namespace entered in the @ProjectNamespace annotation
    }
}
```

a slight drawback is that when you first setup ResourceLib, the initializer won't exist. To fix this you run
`./gradlew jar` if using gradle, or run `compile` if you're using maven, from there on it should always exist.

#### Finally, Initialization And Use

There are a number of methods that relate to initialization and initializing ResourceLib, in the generated initializer (if using annotation processor)
and in the `ResourceLib` Class. I'm going to keep things simple here but you can look at them all [here](Core/src/main/java/serap/io/ResourceLib.java).

You need to initialize the library, which will return you a `ResourceManager`.
```java
ResourceManager resourceManager = ResourceLib.initResourceLib(storage);
//OR IF YOU'RE USING THE ANNOTATION PROCESSOR
ResourceManager resourceManager = ExampleResourceInitializer.initialize();
```

then you need to add a base source (a base source is a source that the resource manager can always rely on to supply a vaild resource).
there can be multiple base sources, but (when combined) all the base sources need to be able to always produce a valid resource for ANY given resource
at any given time
```java
resourceManager.addBaseSource(new ClasspathResourceSource(Main.class.getClassLoader()));
```

next you get the `ResourceManager` to load all the resources which need to be loaded on initialization
```java
resourceManager.initiallyLoadResources();
```

and finally you just have to poll the resouce manager in your loop
```java
while(isRunning) {
    if(resourceManager.doneLoading()) {
        //you can make sure it's done loading like this
    }

    //you gotta poll it at the END of every loop like so
    resourceManager.pollResourceLoading();
}
//and finally clean it up when your program finishes executing
resourceManager.cleanup();
```

And that's it! bye lol
