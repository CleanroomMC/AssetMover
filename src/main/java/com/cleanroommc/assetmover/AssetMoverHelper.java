package com.cleanroommc.assetmover;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.*;

public enum AssetMoverHelper {

    ;

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36 Edg/107.0.1418.42";
    private static final String VERSIONS_MANIFEST = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    private static final String RESOURCES_URL = "http://resources.download.minecraft.net/";

    private static ExecutorService EXECUTOR;
    private static Map<String, Path> URL_FILES = new Object2ObjectOpenHashMap<>();
    private static Map<String, VersionAssetsInfo> VERSION_ASSET_INFO = new Object2ObjectOpenHashMap<>();
    private static Map<String, Map<String, String>> ASSET_MAPPING = new Object2ObjectOpenHashMap<>();
    private static JsonArray VERSIONS_ARRAY;

    static void haltAndFlush() {
        if (EXECUTOR != null) {
            boolean successfullyTerminated;
            InterruptedException exception = null;
            try {
                EXECUTOR.shutdown();
                successfullyTerminated = EXECUTOR.awaitTermination(1L, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                successfullyTerminated = false;
                exception = e;
            }
            if (!successfullyTerminated) {
                AssetMoverAPI.LOGGER.fatal("Executor interrupted!", exception);
            } else {
                AssetMoverAPI.LOGGER.info("Downloading and moving of assets complete!");
            }
            EXECUTOR = null;
        }
        AssetMoverAPI.LOGGER.info("Clearing cache...");

        URL_FILES.clear();
        URL_FILES = null;

        VERSION_ASSET_INFO.clear();
        VERSION_ASSET_INFO = null;

        ASSET_MAPPING.clear();
        ASSET_MAPPING = null;

        VERSIONS_ARRAY = null;
    }

    static void fromMinecraftVersion(String version, Map<String, String> assets) throws IOException {
        File mcFolder = getMinecraftDirectory();
        File versionsFolder = new File(mcFolder, "versions");
        JsonObject versionObject = getVersionJson(version, versionsFolder);
        scanAndMoveMinecraftAssets(version, mcFolder, versionsFolder, versionObject, assets);
    }

    static Path getCurseForgeMod(String projectId, String fileId) throws IOException, URISyntaxException {
        Path file = URL_FILES.get(projectId + "-" + fileId);
        if (file != null) {
            return file;
        }
        long start = System.currentTimeMillis();
        URL url = new URL(String.format("https://api.curse.tools/v1/cf/mods/%s/files/%s", projectId, fileId));
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("User-Agent", USER_AGENT);
        JsonObject reqData;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), Charsets.UTF_8))) {
            reqData = new JsonParser().parse(br).getAsJsonObject().getAsJsonObject("data");
        }
        URL downloadUrl = new URL(reqData.get("downloadUrl").getAsString());
        con = (HttpURLConnection) downloadUrl.openConnection();
        con.setRequestProperty("User-Agent", USER_AGENT);
        file = downloadToTempJar(con.getInputStream(), projectId + "-" + fileId);
        URL_FILES.put(downloadUrl.toURI().toString(), file);
        AssetMoverAPI.LOGGER.info("CurseForge mod downloaded in {}", getTime(System.currentTimeMillis() - start));
        return file;
    }

    static Path getFileFromUrl(URL url, String tempFileName) throws IOException, URISyntaxException {
        long start = System.currentTimeMillis();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("User-Agent", USER_AGENT);
        Path file = downloadToTempJar(con.getInputStream(), tempFileName);
        AssetMoverAPI.LOGGER.info("File downloaded in {}", getTime(System.currentTimeMillis() - start));
        URL_FILES.put(url.toURI().toString(), file);
        return file;
    }

    private static JsonObject getVersionJson(String version, File versionsFolder) throws IOException {
        VersionAssetsInfo info = VERSION_ASSET_INFO.get(version);
        JsonObject versionObject = null;
        if (info != null && info.versionJson != null) {
            versionObject = info.versionJson;
        }
        if (versionObject == null) {
            File versionFile = new File(versionsFolder, version + ".json");
            if (versionFile.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(versionFile))) {
                    versionObject = new JsonParser().parse(br).getAsJsonObject();
                } catch (IOException e) {
                    AssetMoverAPI.LOGGER.fatal("Unexpected error occurred while reading {}", version + ".json");
                    AssetMoverAPI.LOGGER.fatal("Attempting to Re-Download {} from Mojang", version + ".json", e);
                }
            }
            if (versionObject == null) {
                if (VERSIONS_ARRAY == null) {
                    long start = System.currentTimeMillis();
                    URL manifestUrl = new URL(VERSIONS_MANIFEST);
                    HttpURLConnection con = (HttpURLConnection) manifestUrl.openConnection();
                    con.setRequestProperty("User-Agent", USER_AGENT);
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), Charsets.UTF_8))) {
                        VERSIONS_ARRAY = new JsonParser().parse(br).getAsJsonObject().getAsJsonArray("versions");
                        AssetMoverAPI.LOGGER.info("Manifest downloaded in {}", getTime(System.currentTimeMillis() - start));
                    } catch (IOException e) {
                        AssetMoverAPI.LOGGER.fatal("Unexpected error occurred while downloading versions manifest", e);
                    }
                }
                if (VERSIONS_ARRAY != null) {
                    URL versionUrl = null;
                    for (JsonElement versionElement : VERSIONS_ARRAY) {
                        if (versionElement instanceof JsonObject) {
                            JsonObject versionObj = versionElement.getAsJsonObject();
                            String id = versionObj.get("id").getAsString();
                            if (version.equals(id)) {
                                versionUrl = new URL(versionObj.get("url").getAsString());
                                break;
                            }
                        }
                    }
                    if (versionUrl == null) {
                        AssetMoverAPI.LOGGER.fatal("Invalid version detetected, {} is not present in the manifest json", version);
                    } else {
                        long start = System.currentTimeMillis();
                        HttpURLConnection con = (HttpURLConnection) versionUrl.openConnection();
                        con.setRequestProperty("User-Agent", USER_AGENT);
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), Charsets.UTF_8))) {
                            versionObject = new JsonParser().parse(br).getAsJsonObject();
                            AssetMoverAPI.LOGGER.info("Manifest of {} downloaded in {}", version, getTime(System.currentTimeMillis() - start));
                        } catch (IOException e) {
                            AssetMoverAPI.LOGGER.fatal("Unexpected error occurred while downloading versions json", e);
                        }
                    }
                }
            }
            if (versionObject != null) {
                info = new VersionAssetsInfo();
                info.versionJson = versionObject;
                VERSION_ASSET_INFO.put(version, info);
            }
        }
        return versionObject;
    }

    private static void scanAndMoveMinecraftAssets(String version, File mcFolder, File versionFolder, JsonObject versionObject, Map<String, String> assets) throws IOException {
        JsonObject assetIndexObject = versionObject.getAsJsonObject("assetIndex");
        String assetVersion = assetIndexObject.get("id").getAsString();
        Map<String, String> objects = ASSET_MAPPING.get(assetVersion);
        VersionAssetsInfo info = VERSION_ASSET_INFO.get(version);
        if (info == null) {
            info = new VersionAssetsInfo();
        }
        if (objects == null) {
            File indexesFolder = new File(mcFolder, "assets/indexes");
            JsonObject versionIndexJsonObject = null;
            if (indexesFolder.exists()) {
                File versionIndexJson = new File(indexesFolder, assetVersion + ".json");
                if (versionIndexJson.exists()) {
                    try (BufferedReader br = new BufferedReader(new FileReader(versionIndexJson))) {
                        versionIndexJsonObject = new JsonParser().parse(br).getAsJsonObject().getAsJsonObject("objects");
                    } catch (IOException e) {
                        AssetMoverAPI.LOGGER.fatal("Unexpected error occurred while reading asset index json for version {} {}", assetVersion, e);
                    }
                }
                info.assetIndexAvailableLocally = true;
            }
            if (versionIndexJsonObject == null) {
                long start = System.currentTimeMillis();
                URL assetIndexUrl = new URL(assetIndexObject.get("url").getAsString());
                HttpURLConnection con = (HttpURLConnection) assetIndexUrl.openConnection();
                con.setRequestProperty("User-Agent", USER_AGENT);
                try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), Charsets.UTF_8))) {
                    versionIndexJsonObject = new JsonParser().parse(br).getAsJsonObject().getAsJsonObject("objects");
                    AssetMoverAPI.LOGGER.info("Asset index for {} downloaded in {}", assetVersion, getTime(System.currentTimeMillis() - start));
                } catch (IOException e) {
                    AssetMoverAPI.LOGGER.fatal("Unexpected error occurred while downloading asset index json for version {} {}", assetVersion, e);
                }
                info.assetIndexAvailableLocally = false;
            }
            objects = new Object2ObjectOpenHashMap<>(versionIndexJsonObject.size());
            ASSET_MAPPING.put(assetVersion, objects);
            for (Entry<String, JsonElement> elements : versionIndexJsonObject.entrySet()) {
                objects.put(elements.getKey(), elements.getValue().getAsJsonObject().get("hash").getAsString());
            }
        }
        if (EXECUTOR == null) {
            AssetMoverAPI.LOGGER.info("Building Executor...");
            EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setPriority(3).setNameFormat("AssetMover Downloader #%d").setDaemon(true).build());
        }
        final Map<String, String> finalizedObjects = objects;
        final VersionAssetsInfo finalizedInfo = info;
        EXECUTOR.execute(() -> {
            Iterator<Entry<String, String>> iter = assets.entrySet().iterator();
            while (iter.hasNext()) {
                Entry<String, String> entry = iter.next();
                String hash = finalizedObjects.get(entry.getKey());
                if (hash == null) {
                    hash = finalizedObjects.get(entry.getKey().substring(7)); // Try without the potential "assets/" prefix
                    if (hash == null) {
                        continue; // Part of client.jar
                    }
                }
                File to = AssetMoverAPI.PARENT_PATH.resolve(entry.getValue()).toFile();
                if (finalizedInfo.assetIndexAvailableLocally) {
                    File objectsFolder = new File(mcFolder, "assets/objects");
                    if (objectsFolder.exists()) {
                        File hashFile = new File(objectsFolder, hash.substring(0, 2) + "/" + hash);
                        move(hashFile, to);
                        iter.remove();
                        continue;
                    }
                }
                try {
                    long start = System.currentTimeMillis();
                    URL hashUrl = new URL(RESOURCES_URL + hash.substring(0, 2) + "/" + hash);
                    HttpURLConnection con = (HttpURLConnection) hashUrl.openConnection();
                    con.setRequestProperty("User-Agent", USER_AGENT);
                    move(con.getInputStream(), to);
                    AssetMoverAPI.LOGGER.info("Asset '{}' downloaded in {}", entry.getKey(), getTime(System.currentTimeMillis() - start));
                    iter.remove();
                } catch (IOException e) {
                    AssetMoverAPI.LOGGER.fatal("Unexpected error occurred while downloading asset '{}' {}", entry.getKey(), e);
                }
            }
            Path clientJar = null;
            if (finalizedInfo.clientJar != null) {
                clientJar = finalizedInfo.clientJar;
            } else {
                File clientJarFile = new File(versionFolder, version + "/" + version + ".jar");
                if (clientJarFile.exists()) {
                    clientJar = clientJarFile.toPath();
                } else {
                    String url = versionObject.getAsJsonObject("downloads").getAsJsonObject("client").get("url").getAsString();
                    try {
                        long start = System.currentTimeMillis();
                        URL clientJarUrl = new URL(url);
                        HttpURLConnection con = (HttpURLConnection) clientJarUrl.openConnection();
                        con.setRequestProperty("User-Agent", USER_AGENT);
                        clientJar = downloadToTempJar(con.getInputStream(), version + "-client");
                        AssetMoverAPI.LOGGER.info("Downloaded {}'s client jar in {}", version, getTime(System.currentTimeMillis() - start));
                    } catch (IOException e) {
                        AssetMoverAPI.LOGGER.fatal("Unexpected error occurred while downloading Minecraft {}'s client jar {}", version, e);
                    }
                }
                finalizedInfo.clientJar = clientJar;
            }
            moveViaFilesystem(clientJar, assets);
        });
    }

    static Path downloadToTempJar(InputStream is, String fileName) throws IOException {
        Path tempFile = Files.createTempFile(fileName, ".jar");
        tempFile.toFile().deleteOnExit();
        Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
        return tempFile;
    }

    static void moveViaFilesystem(Path file, Map<String, String> assets) {
        try (FileSystem modFs = FileSystems.newFileSystem(file, null)) {
            Iterator<Map.Entry<String, String>> iter = assets.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, String> entry = iter.next();
                Path path = AssetMoverAPI.PARENT_PATH.resolve(entry.getValue());
                try {
                    move(modFs.getPath(entry.getKey()), path);
                    iter.remove();
                } catch (FileNotFoundException | NoSuchFileException e) {
                    AssetMoverAPI.LOGGER.fatal("Could not find asset '{}' to be copied over.", entry.getKey());
                }
            }
        } catch (IOException e) {
            AssetMoverAPI.LOGGER.fatal("Unexpected error occurred", e);
        }
    }

    static void move(Path from, Path to) throws IOException {
        to.toFile().getParentFile().mkdirs();
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
    }

    static void move(File from, File to) {
        try {
            to.getParentFile().mkdirs();
            com.google.common.io.Files.copy(from, to);
        } catch (IOException e) {
            AssetMoverAPI.LOGGER.fatal("Unexpected error occurred", e);
        }
    }

    static void move(InputStream from, File to) {
        to.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(to)) {
            IOUtils.copy(from, out);
        } catch (IOException e) {
            AssetMoverAPI.LOGGER.fatal("Unexpected error occurred", e);
        }
    }

    private static File getMinecraftDirectory() {
        switch (OS.CURRENT) {
            case LINUX:
                return new File(System.getProperty("user.home"), ".minecraft");
            case WINDOWS:
                String appData = System.getenv("APPDATA");
                String folder = appData != null ? appData : System.getProperty("user.home");
                return new File(folder, ".minecraft");
            case OSX:
                return new File(System.getProperty("user.home"), "Library/Application Support/minecraft");
            default:
                return new File(System.getProperty("user.home"), "minecraft");
        }
    }

    private static String getTime(long millis) {
        String suffix = " ms";
        TimeUnit unit = MILLISECONDS;
        if (MINUTES.convert(millis, MILLISECONDS) > 0) {
            suffix = " min";
            unit = MINUTES;
        }
        if (SECONDS.convert(millis, MILLISECONDS) > 0) {
            suffix = " s";
            unit = SECONDS;
        }
        double value = (double) millis / MILLISECONDS.convert(1, unit);
        return String.format(Locale.ROOT, "%.4g", value) + suffix;
    }

    private static class VersionAssetsInfo {

        private JsonObject versionJson;
        private boolean assetIndexAvailableLocally;
        private Path clientJar;

    }

    private enum OS {

        LINUX("linux", "linux", "bsd", "unix"),
        WINDOWS("windows", "win"),
        OSX("osx", "mac"),
        UNKNOWN("unknown");

        private final String name;
        private final String[] aliases;

        public static final OS CURRENT = getCurrentPlatform();

        OS(String name, String... aliases) {
            this.name = name;
            this.aliases = aliases;
        }

        public static OS getCurrentPlatform() {
            String osName = System.getProperty("os.name").toLowerCase(Locale.US);
            for (OS os : values()) {
                if (osName.contains(os.name)){
                    return os;
                }
                for (String alias : os.aliases) {
                    if (osName.contains(alias)){
                        return os;
                    }
                }
            }
            return UNKNOWN;
        }

        @Override
        public String toString() {
            return name;
        }

    }

}
