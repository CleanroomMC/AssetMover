/*
 * Copyright (c) 2022-2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.assetmover;

import com.cleanroommc.assetmover.api.GitHost;

import com.google.common.io.BaseEncoding;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@ApiStatus.Internal
public final class AssetMoverHelper {

    private static final String USER_AGENT = "CleanroomMC/AssetMover/@{VERSION} (+https://github.com/CleanroomMC/AssetMover)";
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private AssetMoverHelper() { }

    public static String curseForge(String projectId, String fileId) {
        String artifact = "curse-" + projectId;
        return "https://curse.cleanroommc.com/curse/maven/" + artifact + "/" + fileId + "/" + artifact + "-" + fileId +
            ".jar";
    }

    public static String modrinth(String projectId, String versionId) throws IOException {
        JsonArray files = json(
            "https://api.modrinth.com/v2/project/" + projectId + "/version/" + versionId,
            "mod-site"
        ).getAsJsonArray("files");
        for (JsonElement file : files) {
            if (file.getAsJsonObject().get("primary").getAsBoolean()) {
                return file.getAsJsonObject().get("url").getAsString();
            }
        }
        return files.get(0).getAsJsonObject().get("url").getAsString();
    }

    public static GitHost gitea(String url) {
        String host = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        return new GitHost() {

            @Override
            public String archiveUrl(String repository, String ref) {
                return host + "/" + repository + "/archive/" + ref + ".zip";
            }

            @Override
            public String releaseUrl(String repository, String tag, String fileName) {
                return host + "/" + repository + "/releases/download/" + tag + "/" + fileName;
            }

        };
    }

    public static GitHost gitlab(String url) {
        String host = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        return new GitHost() {

            @Override
            public String archiveUrl(String repository, String ref) {
                String name = repository.substring(repository.lastIndexOf('/') + 1);
                return host + "/" + repository + "/-/archive/" + ref + "/" + name + "-" + ref.replace('/', '-') +
                    ".zip";
            }

            // GitLab release files are links rather than uploads, so they are looked up by name
            @Override
            public String releaseUrl(String repository, String tag, String fileName) throws IOException {
                JsonObject release = json(
                    host + "/api/v4/projects/" + URLEncoder.encode(repository, "UTF-8") + "/releases/" +
                        URLEncoder.encode(tag, "UTF-8"),
                    "git"
                );
                for (JsonElement element : release.getAsJsonObject("assets").getAsJsonArray("links")) {
                    JsonObject link = element.getAsJsonObject();
                    if (fileName.equals(link.get("name").getAsString())) {
                        JsonElement direct = link.get("direct_asset_url");
                        return (direct != null ? direct : link.get("url")).getAsString();
                    }
                }
                throw new IOException("Release " + tag + " of " + repository + " has no file named " + fileName);
            }

        };
    }

    static JsonObject json(String url, String source) throws IOException {
        try (Reader reader = new InputStreamReader(open(url, source), StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader).getAsJsonObject();
        }
    }

    static JsonObject json(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader).getAsJsonObject();
        }
    }

    static Path download(String url, String source) throws IOException {
        long start = System.currentTimeMillis();
        Path file = Files.createTempFile("assetmover", null);
        file.toFile().deleteOnExit();
        try (InputStream in = open(url, source)) {
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
        }
        AssetMover.LOGGER.info("Downloaded {} in {} ms", url, System.currentTimeMillis() - start);
        return file;
    }

    static boolean matches(Path file, String algorithm, String checksum) throws IOException {
        try {
            byte[] hash = MessageDigest.getInstance(algorithm).digest(Files.readAllBytes(file));
            return BaseEncoding.base16().lowerCase().encode(hash).equalsIgnoreCase(checksum);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Unknown hash algorithm " + algorithm, e);
        }
    }

    static void verify(Path file, @Nullable String algorithm, @Nullable String checksum) throws IOException {
        if (checksum != null && !matches(file, algorithm, checksum)) {
            throw new IOException(algorithm + " checksum of the download does not match " + checksum);
        }
    }

    static String algorithmOf(String checksum) {
        switch (checksum.length()) {
            case 32:
                return "MD5";
            case 40:
                return "SHA-1";
            case 64:
                return "SHA-256";
            case 128:
                return "SHA-512";
            default:
                throw new IllegalArgumentException("Cannot tell the hash algorithm of " + checksum);
        }
    }

    static void copy(Path from, Path to) throws IOException {
        Files.createDirectories(to.getParent());
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
    }

    static Path minecraftDirectory() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String home = System.getProperty("user.home");
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return Paths.get(appData != null ? appData : home, ".minecraft");
        }
        if (os.contains("mac")) {
            return Paths.get(home, "Library", "Application Support", "minecraft");
        }
        return Paths.get(home, ".minecraft");
    }

    /**
     * Connect and read timeouts come from {@code cleanroom.asset-mover.<source>.timeout}, in seconds.
     */
    private static InputStream open(String url, String source) throws IOException {
        int timeout = (int) TimeUnit.SECONDS.toMillis(Integer.getInteger(
            "cleanroom.asset-mover." + source + ".timeout", DEFAULT_TIMEOUT_SECONDS
        ));
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        return connection.getInputStream();
    }

}
