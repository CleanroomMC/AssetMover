package com.cleanroommc.assetmover;

import io.netty.util.internal.UnstableApi;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoaderState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Iterator;
import java.util.Map;

public class AssetMoverAPI {

    public static final String VERSION = "2.4";

    static final Logger LOGGER = LogManager.getLogger("AssetMover");
    static final Path PARENT_PATH = Paths.get("").resolve("assetmover");

    public static void fromMinecraft(String version, Map<String, String> assets) {
        validateLoadingState();
        assets = new Object2ObjectOpenHashMap<>(assets);
        if (isUpdated(assets)) {
            return;
        }
        try {
            AssetMoverHelper.fromMinecraftVersion(version, assets);
        } catch (SSLHandshakeException e) {
            LOGGER.fatal("Unexpected error occurred, perhaps update your Java version?", e);
        } catch (IOException e) {
            LOGGER.fatal("Unexpected error occurred", e);
        }
    }

    @UnstableApi
    public static void fromCurseForgeMod(String projectId, String fileId, Map<String, String> assets) {
        validateLoadingState();
        assets = new Object2ObjectOpenHashMap<>(assets);
        if (isUpdated(assets)) {
            return;
        }
        try {
            Path file = AssetMoverHelper.getCurseForgeMod(projectId, fileId);
            AssetMoverHelper.moveViaFilesystem(file, assets);
        } catch (SSLHandshakeException e) {
            LOGGER.fatal("Unexpected error occurred, perhaps update your Java version?", e);
        } catch (IOException | URISyntaxException e) {
            LOGGER.fatal("Unexpected error occurred", e);
        }
    }

    public static void fromUrlFile(String url, Map<String, String> assets) {
        validateLoadingState();
        try {
            fromUrlFile(new URL(url), assets);
        } catch (MalformedURLException e) {
            LOGGER.fatal("Unexpected error occurred", e);
        }
    }

    public static void fromUrlFile(URL url, Map<String, String> assets) {
        validateLoadingState();
        assets = new Object2ObjectOpenHashMap<>(assets);
        if (isUpdated(assets)) {
            return;
        }
        try {
            Path file = AssetMoverHelper.getFileFromUrl(url, url.getFile());
            AssetMoverHelper.moveViaFilesystem(file, assets);
        } catch (SSLHandshakeException e) {
            LOGGER.fatal("Unexpected error occurred, perhaps update your Java version?", e);
        } catch (IOException | URISyntaxException e) {
            LOGGER.fatal("Unexpected error occurred", e);
        }
    }

    private static void validateLoadingState() {
        if (Loader.instance().hasReachedState(LoaderState.PREINITIALIZATION)) {
            throw new RuntimeException("AssetMover operations can only be performed during FMLConstructionEvent or earlier!");
        }
    }

    private static boolean isUpdated(Map<String, String> assets) {
        boolean allUpdated = true;
        Iterator<Map.Entry<String, String>> iter = assets.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, String> entry = iter.next();
            if (Files.exists(PARENT_PATH.resolve(entry.getValue()))) {
                iter.remove();
            } else {
                allUpdated = false;
            }
        }
        return allUpdated;
    }

    private AssetMoverAPI() { }

}
