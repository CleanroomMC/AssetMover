package com.cleanroommc.assetmover;

import io.netty.util.internal.UnstableApi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Map;

public class AssetMoverAPI {

    public static final String VERSION = "2.0";

    static final Logger LOGGER = LogManager.getLogger("AssetMover");
    static final Path PARENT_PATH = Paths.get("").resolve("assetmover");

    public static void fromMinecraft(String version, Map<String, String> assets) {
        if (!needsUpdating(assets)) {
            return;
        }
        try {
            AssetMoverHelper.getMinecraftVersion(version, assets);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @UnstableApi
    public static void fromCurseForgeMod(String projectId, String fileId, Map<String, String> assets) {
        if (!needsUpdating(assets)) {
            return;
        }
        try {
            Path file = AssetMoverHelper.getCurseForgeMod(projectId, fileId);
            AssetMoverHelper.moveViaFilesystem(file, assets);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void fromUrlMod(String url, Map<String, String> assets) {
        try {
            fromUrlMod(new URL(url), assets);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static void fromUrlMod(URL url, Map<String, String> assets) {
        if (!needsUpdating(assets)) {
            return;
        }
        try {
            Path file = AssetMoverHelper.getUrlMod(url, url.getFile());
            AssetMoverHelper.moveViaFilesystem(file, assets);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private static boolean needsUpdating(String asset) {
        return !Files.exists(PARENT_PATH.resolve(asset));
    }

    private static boolean needsUpdating(Map<String, String> asset) {
        return asset.values().stream().anyMatch(s -> !Files.exists(PARENT_PATH.resolve(s)));
    }

    private AssetMoverAPI() { }

}
