package com.cleanroommc.assetmover;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import java.util.Collections;

public class AssetMoverModContainer extends DummyModContainer {

    public AssetMoverModContainer() {
        super(new ModMetadata());
        ModMetadata metadata = this.getMetadata();
        metadata.modId = "assetmover";
        metadata.name = "AssetMover";
        metadata.description = "Allows acquiring of vanilla/mod assets at runtime without potentially violating licenses.";
        metadata.version = "2.0";
        metadata.url = "https://github.com/CleanroomMC/AssetMover";
        metadata.authorList = Collections.singletonList("CleanroomMC");
        metadata.credits = "Rongmario";
        metadata.logoFile = "/asset_mover_icon.png";
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
        AssetMoverHelper.clear();
    }

}
