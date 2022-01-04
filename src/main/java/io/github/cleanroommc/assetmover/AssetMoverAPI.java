package io.github.cleanroommc.assetmover;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.launchwrapper.Launch;

import java.io.File;
import java.io.IOException;
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
    private static final Path parentPath = Launch.minecraftHome.toPath().resolve("assetmover");

    private static final Map<String, Path> mcFiles = new Object2ObjectOpenHashMap<>();
    private static final Map<String, Path> urlFiles = new Object2ObjectOpenHashMap<>();

    public static void fromMinecraft(String mcVersion, Map<String, String> assets) {
        if (hasDuplicates(assets)) {
            return;
        }
        for (Map.Entry<String, String> entry : assets.entrySet()) {
            String source = entry.getKey();
            String target = entry.getValue();
            Path existingPath = mcFiles.get(source);
            if (existingPath == null) {
                try {
                    String urlString = "https://github.com/InventivetalentDev/minecraft-assets/raw/" + mcVersion + "/" + source;
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

    public static void fromCurseForgeMod(String projectId, String fileId, Map<String, String> assets) {
        if (hasDuplicates(assets)) {
            return;
        }
        try {
            String name = projectId + "-" + fileId;
            $fromURL(new URL(String.format(curseUrl, projectId, fileId)), name, assets);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static void fromURL(URL url, String name, Map<String, String> assets) {
        if (hasDuplicates(assets)) {
            return;
        }
        $fromURL(url, name, assets);
    }

    private static boolean hasDuplicates(Map<String, String> assets) {
        return assets.values().stream().anyMatch(s -> Files.exists(parentPath.resolve(s)));
    }

    private static void $fromURL(URL url, String name, Map<String, String> assets) {
        String urlString = url.toString();
        Path tempFile = urlFiles.get(urlString);
        if (tempFile == null) {
            try {
                download(url, tempFile = Files.createTempFile(name, ".jar"));
                tempFile.toFile().deleteOnExit();
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
            URLConnection conn = downloadFrom.openConnection();
            Files.copy(conn.getInputStream(), downloadTo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void moveViaFilesystem(Path file, Map<String, String> assets) {
        try (FileSystem modFs = FileSystems.newFileSystem(file, null)) {
            for (Map.Entry<String, String> entry : assets.entrySet()) {
                move(modFs.getPath(entry.getKey()), parentPath.resolve(entry.getValue()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void move(Path from, Path to) {
        try {
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private AssetMoverAPI() { }

}
