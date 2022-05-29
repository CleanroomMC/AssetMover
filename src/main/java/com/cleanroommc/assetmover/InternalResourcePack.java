package com.cleanroommc.assetmover;

import com.google.gson.JsonObject;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.FolderResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.common.FMLContainerHolder;
import net.minecraftforge.fml.common.ModContainer;

import java.awt.image.BufferedImage;
import java.io.*;

public class InternalResourcePack extends FolderResourcePack implements FMLContainerHolder {

    private final ModContainer mc;

    public InternalResourcePack(ModContainer mc) {
        super(new File(Launch.minecraftHome, "assetmover"));
        this.mc = mc;
    }

    @Override
    public ModContainer getFMLContainer() {
        return mc;
    }

    @Override
    public String getPackName() {
        return "AssetMoverPack";
    }

    @Override
    public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer metadataSerializer, String metadataSectionName) {
        JsonObject metadata = new JsonObject();
        JsonObject packObj = new JsonObject();
        metadata.add("pack", packObj);
        packObj.addProperty("description", "Includes assets moved by AssetMover.");
        packObj.addProperty("pack_format", 2);
        return metadataSerializer.parseMetadataSection(metadataSectionName, metadata);
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        return TextureUtil.readBufferedImage(getClass().getResourceAsStream("/asset_mover_icon.png"));
    }

}
