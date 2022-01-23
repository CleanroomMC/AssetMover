package com.cleanroommc.assetmover;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@InternalModAnnotation(modid = AssetMover.MODID, name = AssetMover.NAME, version = AssetMover.VERSION)
public class AssetMover {

    public static final String MODID = "assetmover";
    public static final String NAME = "AssetMover";
    public static final String VERSION = "1.0";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        AssetMoverAPI.clear();
    }

}
