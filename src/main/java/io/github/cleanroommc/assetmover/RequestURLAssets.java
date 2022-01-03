package io.github.cleanroommc.assetmover;

// WIP
public @interface RequestURLAssets {

    String url();

    String sourceNamespace();

    String targetNamespace();

    String[] data();

}
