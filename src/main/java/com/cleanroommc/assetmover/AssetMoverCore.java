package com.cleanroommc.assetmover;

import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.ModContainerFactory;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.objectweb.asm.Type;

import javax.annotation.Nullable;
import java.util.Map;

@IFMLLoadingPlugin.Name("AssetMover|Core")
@IFMLLoadingPlugin.MCVersion(ForgeVersion.mcVersion)
public class AssetMoverCore implements IFMLLoadingPlugin {

    public AssetMoverCore() {
        ModContainerFactory.instance().registerContainerType(Type.getType(com.cleanroommc.assetmover.InternalModAnnotation.class), AssetMoverModContainer.class);
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) { }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

}
