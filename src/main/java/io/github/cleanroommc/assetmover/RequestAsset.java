package io.github.cleanroommc.assetmover;

public @interface RequestAsset {

    String minecraftVersion() default "";

    String modURL() default "";

    String[] data();

}
