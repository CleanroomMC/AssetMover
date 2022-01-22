package com.cleanroommc.assetmover;

import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.discovery.ModCandidate;

import java.util.Map;

public class AssetMoverModContainer extends FMLModContainer {

    public AssetMoverModContainer(String className, ModCandidate container, Map<String, Object> modDescriptor) {
        super(className, container, modDescriptor);
    }

    @Override
    public Class<?> getCustomResourcePackClass() {
        return InternalResourcePack.class;
    }

}
