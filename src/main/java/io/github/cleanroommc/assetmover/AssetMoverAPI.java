package io.github.cleanroommc.assetmover;

import com.google.common.base.Charsets;
import com.google.gson.JsonParser;
import io.netty.util.internal.UnstableApi;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.util.Map;

public class AssetMoverAPI {

    static void clear() {
        mcFiles.clear();
        urlFiles.values().stream().map(Path::toFile).forEach(File::delete);
    }

    private static final String curseUrl = "https://addons-ecs.forgesvc.net/api/v2/addon/%s/file/%s";
    private static final String mcAssetRepoUrl = "https://github.com/InventivetalentDev/minecraft-assets/raw/%s/%s";
    private static final Path parentPath = FMLLaunchHandler.isDeobfuscatedEnvironment() ? Paths.get("").resolve("assetmover") : Launch.minecraftHome.toPath().resolve("assetmover");

    private static final Map<String, Path> mcFiles = new Object2ObjectOpenHashMap<>();
    private static final Map<String, Path> urlFiles = new Object2ObjectOpenHashMap<>();

    public static void fromMinecraft(String mcVersion, Map<String, String> assets) {
        for (Map.Entry<String, String> entry : assets.entrySet()) {
            String target = entry.getValue();
            if (!needsUpdating(target)) {
                continue;
            }
            String source = entry.getKey();
            Path existingPath = mcFiles.get(source);
            if (existingPath == null) {
                try {
                    String urlString = String.format(mcAssetRepoUrl, mcVersion, source);
                    URL url = new URL(urlString);
                    Path targetPath = parentPath.resolve(target);
                    download(url, targetPath);
                    mcFiles.put(source, targetPath);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            } else {
                move(existingPath, parentPath.resolve(target));
            }
        }
    }

    @UnstableApi
    public static void fromCurseForgeMod(String projectId, String fileId, Map<String, String> assets) {
        if (!needsUpdating(assets)) {
            return;
        }
        try {
            String name = projectId + "-" + fileId;
            URL metaUrl = new URL(String.format(curseUrl, projectId, fileId));
            BufferedReader br = new BufferedReader(new InputStreamReader(metaUrl.openStream(), Charsets.UTF_8));
            URL curseForgeUrl = new URL(new JsonParser().parse(br).getAsJsonObject().get("downloadUrl").getAsString());
            $fromURL(curseForgeUrl, name, assets);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void fromURL(URL url, String name, Map<String, String> assets) {
        if (!needsUpdating(assets)) {
            return;
        }
        $fromURL(url, name, assets);
    }

    private static boolean needsUpdating(String asset) {
        return !Files.exists(parentPath.resolve(asset));
    }

    private static boolean needsUpdating(Map<String, String> asset) {
        return asset.values().stream().anyMatch(s -> !Files.exists(parentPath.resolve(s)));
    }

    private static void $fromURL(URL url, String name, Map<String, String> assets) {
        String urlString = url.toString();
        Path tempFile = urlFiles.get(urlString);
        if (tempFile == null) {
            try {
                download(url, tempFile = Files.createTempFile(name, ".jar"));
                tempFile.toFile().deleteOnExit();
                urlFiles.put(urlString, tempFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (tempFile == null) {
                throw new RuntimeException("Unable to discover " + urlString + " for " + name);
            }
        }
        moveViaFilesystem(tempFile, assets);
    }

    private static void download(URL downloadFrom, Path downloadTo) {
        try {
            downloadTo.toFile().getParentFile().mkdirs();
            URLConnection conn = downloadFrom.openConnection();
            Files.copy(conn.getInputStream(), downloadTo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void moveViaFilesystem(Path file, Map<String, String> assets) {
        try (FileSystem modFs = FileSystems.newFileSystem(file, null)) {
            for (Map.Entry<String, String> entry : assets.entrySet()) {
                Path path = parentPath.resolve(entry.getValue());
                move(modFs.getPath(entry.getKey()), path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void move(Path from, Path to) {
        try {
            to.toFile().getParentFile().mkdirs();
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private AssetMoverAPI() { }

}
