/*
 * Copyright (c) 2022-2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.assetmover.api;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Fired once per launch on {@link MinecraftForge#EVENT_BUS} on the client right before resource packs are
 * queried for the first time.
 * <p>Queue everything you need through {@link #getAssetMover()}.
 * AssetMover blocks the game until all assets has been downloaded and moved.
 * Assets will then be guaranteed to be in place by the time anything can read them.
 * <p>The client fires this between mod {@link net.minecraftforge.fml.common.LoaderState#CONSTRUCTING construction}
 * and {@link net.minecraftforge.fml.common.LoaderState#PREINITIALIZATION pre-initialization}.
 * <b>Register your listener no later than {@code FMLConstructionEvent}</b> with
 * the constructor of your {@code @Mod} class being the other option.
 *
 * <pre>
 * {@literal @}Mod.EventHandler
 * public void onConstruction(FMLConstructionEvent event) {
 *     MinecraftForge.EVENT_BUS.register(this);
 * }
 *
 * {@literal @}SubscribeEvent
 * public void onMoveAssets(MoveAssetEvent event) {
 *     AssetMoverAPI api = event.getAssetMover();
 *     api.fromJava("1.20.1")
 *         .moveAs("assets/minecraft/textures/block/dirt.png", "assets/mymod/textures/block/dirt.png");
 *     api.fromModSite(ModSite.MODRINTH, "jei", "AAAAAAAA")
 *         .move("assets/jei/textures/gui/icons/recipe_transfer.png");
 * }
 * </pre>
 */
public class MoveAssetEvent extends Event {

    private final AssetMoverAPI assetMover;

    public MoveAssetEvent(AssetMoverAPI assetMover) {
        this.assetMover = assetMover;
    }

    public AssetMoverAPI getAssetMover() {
        return this.assetMover;
    }

}
