package io.github.cleanroommc.assetmover;

public @interface RequestAsset {

    MinecraftVersion minecraftVersion() default MinecraftVersion.NIL;

    String modURL() default "";

    String[] data();

}
