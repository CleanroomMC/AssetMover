/*
 * Copyright (c) 2022-2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.assetmover;

import com.google.common.eventbus.EventBus;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModMetadata;

import java.util.Collections;

public class AssetMoverModContainer extends DummyModContainer {

    public AssetMoverModContainer() {
        super(new ModMetadata());
        ModMetadata metadata = this.getMetadata();
        metadata.modId = "assetmover";
        metadata.name = "AssetMover";
        metadata.description = "Allows acquiring of vanilla/mod assets at runtime without potentially violating licenses.";
        metadata.version = "@{VERSION}";
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
        MinecraftForge.EVENT_BUS.register(AssetMoverAPI.class);
        return true;
    }

}
