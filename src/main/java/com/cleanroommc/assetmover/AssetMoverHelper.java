package com.cleanroommc.assetmover;

import com.google.common.base.Charsets;
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

public class AssetMoverHelper {

    ;

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36 Edg/107.0.1418.42";
    private static final String VERSIONS_MANIFEST = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    private static final String RESOURCES_URL = "http://resources.download.minecraft.net/";

    private static Map<String, Path> URL_FILES = new Object2ObjectOpenHashMap<>();
    private static Map<String, VersionAssetsInfo> VERSION_ASSET_INFO = new Object2ObjectOpenHashMap<>();
    private static Map<String, Map<String, String>> ASSET_MAPPING = new Object2ObjectOpenHashMap<>();
    private static JsonArray VERSIONS_ARRAY = null;

    static void haltAndFlush() {
        AssetMoverAPI.LOGGER.info("Clearing cache...");

        URL_FILES.clear();
        URL_FILES = null;

        VERSION_ASSET_INFO.clear();
        VERSION_ASSET_INFO = null;

        ASSET_MAPPING.clear();
        ASSET_MAPPING = null;

        VERSIONS_ARRAY = null;
    }

    static void getMinecraftVersion(String version, Map<String, String> assets) throws IOException {
        Map<String, String> assetsModifiable = new Object2ObjectOpenHashMap<>(assets);
        File versionsFolder = new File(getMinecraftDirectory(), "versions");
        JsonObject versionObject = getVersionJson(version, versionsFolder);
        getAssetIndexObjectsAndMove(version, versionObject, assetsModifiable);
        getClientJarAndMove(version, versionsFolder, versionObject, assetsModifiable);
    }

    static Path getCurseForgeMod(String projectId, String fileId) throws IOException, URISyntaxException {
        Path file = URL_FILES.get(projectId + "-" + fileId);
        if (file != null) {
            return file;
        }
        URL url = new URL(String.format("https://api.curse.tools/v1/cf/mods/%s/files/%s", projectId, fileId));
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("User-Agent", USER_AGENT);
        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), Charsets.UTF_8));
        JsonObject reqData = new JsonParser().parse(br).getAsJsonObject().getAsJsonObject("data");

        String downloadUrl = reqData.get("downloadUrl").getAsString();
        return getUrlMod(new URL(downloadUrl), projectId + "-" + fileId);
    }

    static Path getUrlMod(URL url, String tempFileName) throws IOException, URISyntaxException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("User-Agent", USER_AGENT);
        Path file = downloadToTempJar(con.getInputStream(), tempFileName);
        URL_FILES.put(url.toURI().toString(), file);
        return file;
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
                } catch (FileNotFoundException e) {
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
                    AssetMoverAPI.LOGGER.fatal("Unexpected error occurred while reading {} {}", version + ".json", e);
                }
            } else {
                if (VERSIONS_ARRAY == null) {
                    URL manifestUrl = new URL(VERSIONS_MANIFEST);
                    HttpURLConnection con = (HttpURLConnection) manifestUrl.openConnection();
                    con.setRequestProperty("User-Agent", USER_AGENT);
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), Charsets.UTF_8))) {
                        VERSIONS_ARRAY = new JsonParser().parse(br).getAsJsonObject().getAsJsonArray("versions");
                    } catch (IOException e) {
                        AssetMoverAPI.LOGGER.fatal("Unexpected error occurred while downloading versions manifest", e);
                    }
                }
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
                    throw AssetMoverAPI.LOGGER.throwing(new IllegalArgumentException("Invalid version detected, " + version + " is not present in manifest json"));
                }
                HttpURLConnection con = (HttpURLConnection) versionUrl.openConnection();
                con.setRequestProperty("User-Agent", USER_AGENT);
                try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), Charsets.UTF_8))) {
                    versionObject = new JsonParser().parse(br).getAsJsonObject();
                } catch (IOException e) {
                    AssetMoverAPI.LOGGER.fatal("Unexpected error occurred while downloading versions json", e);
                }
            }
            info = new VersionAssetsInfo();
            info.versionJson = versionObject;
            VERSION_ASSET_INFO.put(version, info);
        }
        return versionObject;
    }

    private static void getAssetIndexObjectsAndMove(String version, JsonObject versionObject, Map<String, String> assets) throws IOException {
        File mcDir = getMinecraftDirectory();
        JsonObject assetIndexObject = versionObject.getAsJsonObject("assetIndex");
        String assetVersion = assetIndexObject.get("id").getAsString();
        Map<String, String> objects = ASSET_MAPPING.get(assetVersion);
        VersionAssetsInfo info = VERSION_ASSET_INFO.get(version);
        if (info == null) {
            info = new VersionAssetsInfo();
        }
        if (objects == null) {
            File indexesFolder = new File(mcDir, "assets/indexes");
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
                URL assetIndexUrl = new URL(assetIndexObject.get("url").getAsString());
                HttpURLConnection con = (HttpURLConnection) assetIndexUrl.openConnection();
                con.setRequestProperty("User-Agent", USER_AGENT);
                try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), Charsets.UTF_8))) {
                    versionIndexJsonObject = new JsonParser().parse(br).getAsJsonObject().getAsJsonObject("objects");
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
        Iterator<Entry<String, String>> iter = assets.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, String> entry = iter.next();
            String hash = objects.get(entry.getKey());
            if (hash == null) {
                continue; // Part of client.jar
            }
            File to = AssetMoverAPI.PARENT_PATH.resolve(entry.getValue()).toFile();
            if (info.assetIndexAvailableLocally) {
                File objectsFolder = new File(mcDir, "assets/objects");
                if (objectsFolder.exists()) {
                    File hashFile = new File(objectsFolder, hash.substring(0, 2) + "/" + hash);
                    move(hashFile, to);
                    iter.remove();
                    continue;
                }
            }
            URL hashUrl = new URL(RESOURCES_URL + hash.substring(0, 2) + "/" + hash);
            HttpURLConnection con = (HttpURLConnection) hashUrl.openConnection();
            con.setRequestProperty("User-Agent", USER_AGENT);
            move(con.getInputStream(), to);
            iter.remove();
        }
    }

    private static void getClientJarAndMove(String version, File versionFolder, JsonObject versionObject, Map<String, String> assets) throws IOException {
        VersionAssetsInfo info = VERSION_ASSET_INFO.get(version);
        if (info == null) {
            info = new VersionAssetsInfo();
        }
        Path clientJar;
        if (info.clientJar != null) {
            clientJar = info.clientJar;
        } else {
            File clientJarFile = new File(versionFolder, version + "/" + version + ".jar");
            if (clientJarFile.exists()) {
                clientJar = clientJarFile.toPath();
            } else {
                String url = versionObject.getAsJsonObject("downloads").get("url").getAsString();
                URL clientJarUrl = new URL(url);
                HttpURLConnection con = (HttpURLConnection) clientJarUrl.openConnection();
                con.setRequestProperty("User-Agent", USER_AGENT);
                clientJar = downloadToTempJar(con.getInputStream(), version + "-client");
            }
            info.clientJar = clientJar;
        }
        moveViaFilesystem(clientJar, assets);
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
