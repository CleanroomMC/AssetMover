package io.github.cleanroommc.assetmover;

public @interface RequestMinecraftAssets {

    MinecraftVersion version();

    String[] data();

}
