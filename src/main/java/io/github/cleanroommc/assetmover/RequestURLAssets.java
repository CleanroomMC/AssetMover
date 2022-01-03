package io.github.cleanroommc.assetmover;

public @interface RequestURLAssets {

    String url();

    String sourceNamespace();

    String targetNamespace();

    String[] data();

}
