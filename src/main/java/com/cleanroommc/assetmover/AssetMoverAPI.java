/*
 * Copyright (c) 2022-2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.assetmover;

import com.cleanroommc.assetmover.api.ModSite;
import com.cleanroommc.assetmover.api.MoveAssetEvent;

import org.jetbrains.annotations.ApiStatus;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoaderState;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The 2.x entry point. Each call is queued and replayed against {@link com.cleanroommc.assetmover.api.AssetMoverAPI}
 * when {@link MoveAssetEvent} fires.
 *
 * @deprecated listen to {@link MoveAssetEvent} instead.
 */
@Deprecated
public final class AssetMoverAPI {

    public static final String VERSION = "2.5";

    private static final List<Consumer<com.cleanroommc.assetmover.api.AssetMoverAPI>> LEGACY_REQUESTS = new ArrayList<>();

    private static boolean eventFired;

    private AssetMoverAPI() { }

    /**
     * Queues assets from a Minecraft version.
     *
     * @deprecated use {@link com.cleanroommc.assetmover.api.AssetMoverAPI#fromJava(String)}
     */
    @Deprecated
    public static void fromMinecraft(String version, Map<String, String> assets) {
        Map<String, String> copy = new HashMap<>(assets);
        queue(api -> api.fromJava(version).move(copy));
    }

    /**
     * Queues assets from a CurseForge file.
     *
     * @deprecated use {@link com.cleanroommc.assetmover.api.AssetMoverAPI#fromModSite(ModSite, String, String)}
     */
    @Deprecated
    public static void fromCurseForgeMod(String projectId, String fileId, Map<String, String> assets) {
        Map<String, String> copy = new HashMap<>(assets);
        queue(api -> api.fromModSite(ModSite.CURSEFORGE, projectId, fileId).move(copy));
    }

    /**
     * Queues assets from an archive url.
     *
     * @deprecated use {@link com.cleanroommc.assetmover.api.AssetMoverAPI#fromUrl(String)}
     */
    @Deprecated
    public static void fromUrlFile(String url, Map<String, String> assets) {
        Map<String, String> copy = new HashMap<>(assets);
        queue(api -> api.fromUrl(url).move(copy));
    }

    /**
     * Queues assets from an archive url.
     *
     * @deprecated use {@link com.cleanroommc.assetmover.api.AssetMoverAPI#fromUrl(String)}
     */
    @Deprecated
    public static void fromUrlFile(URL url, Map<String, String> assets) {
        fromUrlFile(url.toString(), assets);
    }

    @ApiStatus.Internal
    @SubscribeEvent
    public static void onMoveAssets(MoveAssetEvent event) {
        eventFired = true;
        LEGACY_REQUESTS.forEach(request -> request.accept(event.getAssetMover()));
        LEGACY_REQUESTS.clear();
    }

    private static void validateLoadingState() {
        if (Loader.instance().hasReachedState(LoaderState.PREINITIALIZATION)) {
            throw new RuntimeException(
                "AssetMover operations can only be performed during FMLConstructionEvent or earlier!"
            );
        }
    }

    private static void queue(Consumer<com.cleanroommc.assetmover.api.AssetMoverAPI> request) {
        if (eventFired) {
            ModContainer mod = Loader.instance().activeModContainer();
            AssetMover.LOGGER.error(
                "{} called the 2.x AssetMover API after MoveAssetEvent fired, so its assets are not moved this launch. " +
                    "Update it to AssetMover 3.x and queue assets in MoveAssetEvent.",
                mod == null ? "A mod" : mod.getModId(), new Throwable("Caller")
            );
            return;
        }
        validateLoadingState();
        LEGACY_REQUESTS.add(request);
    }

}
