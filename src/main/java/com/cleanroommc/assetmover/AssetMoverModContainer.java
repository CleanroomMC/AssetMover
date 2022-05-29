package com.cleanroommc.assetmover;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import java.util.Collections;

public class AssetMoverModContainer extends DummyModContainer {

    private static final ModMetadata modMetadata = new ModMetadata();

    static {
        modMetadata.modId = "assetmover";
        modMetadata.name = "AssetMover";
        modMetadata.description = "Allows acquiring of vanilla/mod assets at runtime without potentially violating licenses.";
        modMetadata.version = "1.1";
        modMetadata.url = "https://github.com/CleanroomMC/AssetMover";
        modMetadata.authorList = Collections.singletonList("CleanroomMC");
        modMetadata.credits = "Rongmario";
        modMetadata.logoFile = "/asset_mover_icon.png";
    }

    public AssetMoverModContainer() {
        super(modMetadata);
    }

    @Override
    public Class<?> getCustomResourcePackClass() {
        return InternalResourcePack.class;
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller) {
        bus.register(this);
        return true;
    }

    @Subscribe
    public void init(FMLInitializationEvent event) {
        AssetMoverAPI.clear();
    }

}
