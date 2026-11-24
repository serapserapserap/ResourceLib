package serap;

import serap.annotations.Dependency;
import serap.annotations.Asset;


public class Main {
    @Asset(
            identifier = "assets/img/texture.png",
            alwaysLoaded = false,
            dependencies = {
                    @Dependency(
                            namespace = "example",
                            identifier = "shaders/generic.pgrm"
                    ),
                    @Dependency(
                            namespace = "example",
                            identifier = "models/player.gltf"
                    )
            }
    )
    private int resource;

    private int loadedAsset;
    public static void main(String[] args) {
        System.out.println("Hello world!");
    }
}