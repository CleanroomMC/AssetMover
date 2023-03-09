package com.cleanroommc.assetmover;

import com.google.common.eventbus.EventBus;
import net.minecraftforge.fml.common.*;

import java.util.Collections;

public class AssetMoverModContainer extends DummyModContainer {

    public AssetMoverModContainer() {
        super(new ModMetadata());
        ModMetadata metadata = this.getMetadata();
        metadata.modId = "assetmover";
        metadata.name = "AssetMover";
        metadata.description = "Allows acquiring of vanilla/mod assets at runtime without potentially violating licenses.";
        metadata.version = AssetMoverAPI.VERSION;
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

}
