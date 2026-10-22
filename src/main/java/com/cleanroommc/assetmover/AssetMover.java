/*
 * Copyright (c) 2022-2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.assetmover;

import com.cleanroommc.assetmover.api.AssetMoverAPI;
import com.cleanroommc.assetmover.api.AssetRequest;
import com.cleanroommc.assetmover.api.GitHost;
import com.cleanroommc.assetmover.api.ModSite;
import com.cleanroommc.assetmover.api.MoveAssetEvent;
import com.cleanroommc.assetmover.api.SingleAssetRequest;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.common.ProgressManager.ProgressBar;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class AssetMover implements AssetMoverAPI {

    static final Logger LOGGER = LogManager.getLogger("AssetMover");
    static final Path PACK = Launch.minecraftHome.toPath().resolve("assetmover");

    private static final String VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final String RESOURCES = "https://resources.download.minecraft.net/";
    private static final String MAVEN_CENTRAL = "https://repo.maven.apache.org/maven2";
    private static final int THREADS = Integer.getInteger(
        "cleanroom.asset-mover.threads", Math.max(4, Runtime.getRuntime().availableProcessors() * 2)
    );

    private static boolean moved;

    private final List<Request> requests = new ArrayList<>();
    private final Map<List<?>, CompletableFuture<?>> shared = new ConcurrentHashMap<>();

    private ExecutorService executor;

    /**
     * Fires {@link MoveAssetEvent} and blocks until every queued asset is in place. Only the first call does anything.
     */
    static void moveOnce() {
        if (moved) {
            return;
        }
        moved = true;
        AssetMover mover = new AssetMover();
        MinecraftForge.EVENT_BUS.post(new MoveAssetEvent(mover));
        mover.run();
    }

    private static void extract(Path archive, Map<String, String> assets, Object source) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(archive, (ClassLoader) null)) {
            Path root = fs.getPath("/");
            Path folder = singleFolder(root);
            for (Map.Entry<String, String> asset : assets.entrySet()) {
                Path from = root.resolve(asset.getKey());
                if (!Files.exists(from) && folder != null) {
                    from = folder.resolve(asset.getKey());
                }
                if (Files.exists(from)) {
                    AssetMoverHelper.copy(from, PACK.resolve(asset.getValue()));
                } else {
                    LOGGER.error("Could not find '{}' in {}", asset.getKey(), source);
                }
            }
        }
    }

    /**
     * Archives such as GitHub's wrap everything in one folder, which paths are allowed to omit.
     */
    private static @Nullable
    Path singleFolder(Path root) throws IOException {
        try (Stream<Path> children = Files.list(root)) {
            List<Path> first = children.limit(2).collect(Collectors.toList());
            return first.size() == 1 && Files.isDirectory(first.get(0)) ? first.get(0) : null;
        }
    }

    @Override
    public AssetRequest fromJava(String version) {
        return new JavaAssets(version);
    }

    @Override
    public AssetRequest fromBedrock(String version) {
        return new Archive(
            "bedrock", Arrays.asList("bedrock", version), "Minecraft Bedrock " + version,
            () -> GitHost.GITHUB.archiveUrl("Mojang/bedrock-samples", "v" + version)
        );
    }

    @Override
    public AssetRequest fromModSite(ModSite site, String projectId, String fileId) {
        return new Archive(
            "mod-site", Arrays.asList(site, projectId, fileId), "mod site file " + projectId + "/" + fileId,
            () -> site.downloadUrl(projectId, fileId)
        );
    }

    @Override
    public AssetRequest fromGit(GitHost host, String repository, String ref) {
        return new Archive(
            "git", Arrays.asList(host, repository, ref), repository + "@" + ref,
            () -> host.archiveUrl(repository, ref)
        );
    }

    @Override
    public AssetRequest fromGitRelease(GitHost host, String repository, String tag, String fileName) {
        return new Archive(
            "git", Arrays.asList(host, repository, tag, fileName), repository + "@" + tag + "/" + fileName,
            () -> host.releaseUrl(repository, tag, fileName)
        );
    }

    @Override
    public AssetRequest fromMaven(String repositoryUrl, String coordinate) {
        String extension = "jar";
        int at = coordinate.indexOf('@');
        if (at >= 0) {
            extension = coordinate.substring(at + 1);
            coordinate = coordinate.substring(0, at);
        }
        String[] parts = coordinate.split(":");
        if (parts.length < 3 || parts.length > 4) {
            throw new IllegalArgumentException("Invalid maven coordinate " + coordinate);
        }
        String classifier = parts.length == 4 ? "-" + parts[3] : "";
        String repository = repositoryUrl.endsWith("/") ? repositoryUrl : repositoryUrl + "/";
        String url = repository + parts[0].replace('.', '/') + "/" + parts[1] + "/" + parts[2] + "/" + parts[1] + "-" +
            parts[2] + classifier + "." + extension;
        return new Archive("maven", Arrays.asList("url", url), url, () -> url);
    }

    @Override
    public AssetRequest fromMavenCentral(String coordinate) {
        return fromMaven(MAVEN_CENTRAL, coordinate);
    }

    @Override
    public AssetRequest fromUrl(String archiveUrl) {
        return new Archive("url", Arrays.asList("url", archiveUrl), archiveUrl, () -> archiveUrl);
    }

    @Override
    public SingleAssetRequest fromUrlFile(String fileUrl, String destination) {
        return new SingleFile(fileUrl, destination);
    }

    void run() {
        if (requests.isEmpty()) {
            return;
        }
        long start = System.currentTimeMillis();
        executor = Executors.newFixedThreadPool(
            THREADS, new ThreadFactoryBuilder().setNameFormat("AssetMover #%d").setDaemon(true).build()
        );
        BlockingQueue<Request> done = new LinkedBlockingQueue<>();
        for (Request request : requests) {
            request.start().whenComplete((result, t) -> {
                if (t != null) {
                    LOGGER.error("Could not move assets from {}", request, t);
                }
                done.add(request);
            });
        }
        // The splash screen renders progress bars from another thread, so they are only stepped from this one
        ProgressBar bar = ProgressManager.push("AssetMover", requests.size());
        try {
            for (int i = 1; i <= requests.size(); i++) {
                Request request = done.take();
                bar.step(request.toString());
                LOGGER.info("Finished {} ({}/{})", request, i, requests.size());
            }
            ProgressManager.pop(bar);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdownNow();
        }
        LOGGER.info("Moved assets in {} ms", System.currentTimeMillis() - start);
    }

    private <T> CompletableFuture<T> async(Callable<T> task) {
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    return task.call();
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            },
            executor
        );
    }

    /**
     * Runs {@code task} once per {@code key}, so requests sharing a source share its download.
     */
    @SuppressWarnings("unchecked")
    private <T> CompletableFuture<T> shared(List<?> key, Callable<T> task) {
        return (CompletableFuture<T>) shared.computeIfAbsent(key, k -> async(task));
    }

    private abstract class Request {

        boolean overwrite;
        @Nullable String algorithm;
        @Nullable String checksum;

        Request() {
            requests.add(this);
        }

        abstract CompletableFuture<?> start();

        void setChecksum(String algorithm, String checksum) {
            this.algorithm = algorithm;
            this.checksum = checksum;
        }

    }

    private abstract class Assets extends Request implements AssetRequest {

        final Map<String, String> assets = new LinkedHashMap<>();

        @Override
        public AssetRequest move(String... paths) {
            return move(Arrays.asList(paths));
        }

        @Override
        public AssetRequest move(Collection<String> paths) {
            paths.forEach(path -> assets.put(path, path));
            return this;
        }

        @Override
        public AssetRequest move(Map<String, String> assets) {
            this.assets.putAll(assets);
            return this;
        }

        @Override
        public AssetRequest moveAs(String source, String destination) {
            assets.put(source, destination);
            return this;
        }

        @Override
        public AssetRequest overwrite() {
            overwrite = true;
            return this;
        }

        @Override
        public AssetRequest verify(String checksum) {
            return verify(AssetMoverHelper.algorithmOf(checksum), checksum);
        }

        @Override
        public AssetRequest verify(String algorithm, String checksum) {
            setChecksum(algorithm, checksum);
            return this;
        }

        @Override
        final CompletableFuture<?> start() {
            Map<String, String> pending = new LinkedHashMap<>();
            assets.forEach((source, destination) -> {
                if (overwrite || !Files.exists(PACK.resolve(destination))) {
                    pending.put(source, destination);
                }
            });
            return pending.isEmpty() ? CompletableFuture.completedFuture(null) : start(pending);
        }

        abstract CompletableFuture<?> start(Map<String, String> pending);

    }

    private final class Archive extends Assets {

        private final String type;
        private final List<?> source;
        private final String description;
        private final Callable<String> url;

        Archive(String type, List<?> source, String description, Callable<String> url) {
            this.type = type;
            this.source = source;
            this.description = description;
            this.url = url;
        }

        @Override
        public String toString() {
            return description;
        }

        @Override
        CompletableFuture<?> start(Map<String, String> pending) {
            return shared(source, () -> AssetMoverHelper.download(url.call(), type)).thenCompose(file -> async(() -> {
                AssetMoverHelper.verify(file, algorithm, checksum);
                extract(file, pending, this);
                return null;
            }));
        }

    }

    private final class JavaAssets extends Assets {

        private final String version;
        private final Path minecraft = AssetMoverHelper.minecraftDirectory();

        JavaAssets(String version) {
            this.version = version;
        }

        @Override
        public String toString() {
            return "Minecraft " + version;
        }

        @Override
        CompletableFuture<?> start(Map<String, String> pending) {
            return versionJson().thenCompose(json -> assetIndex(json).thenCompose(index -> {
                List<CompletableFuture<?>> tasks = new ArrayList<>();
                Map<String, String> fromJar = new LinkedHashMap<>();
                pending.forEach((source, destination) -> {
                    JsonObject object = index.getAsJsonObject(source);
                    if (object == null && source.startsWith("assets/")) {
                        object = index.getAsJsonObject(source.substring("assets/".length()));
                    }
                    if (object == null) {
                        fromJar.put(source, destination);
                    } else {
                        String hash = object.get("hash").getAsString();
                        tasks.add(async(() -> {
                            moveObject(hash, PACK.resolve(destination));
                            return null;
                        }));
                    }
                });
                if (!fromJar.isEmpty()) {
                    tasks.add(
                        clientJar(json).thenCompose(jar -> async(() -> {
                            extract(jar, fromJar, this);
                            return null;
                        }))
                    );
                }
                return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]));
            }));
        }

        private CompletableFuture<JsonObject> versionJson() {
            Path local = minecraft.resolve("versions").resolve(version).resolve(version + ".json");
            if (Files.exists(local)) {
                return shared(Arrays.asList("java", version), () -> AssetMoverHelper.json(local));
            }
            return shared(
                Arrays.asList("manifest"), () -> AssetMoverHelper.json(VERSION_MANIFEST, "java")
            ).thenCompose(manifest -> shared(Arrays.asList("java", version), () -> {
                for (JsonElement entry : manifest.getAsJsonArray("versions")) {
                    if (version.equals(entry.getAsJsonObject().get("id").getAsString())) {
                        return AssetMoverHelper.json(entry.getAsJsonObject().get("url").getAsString(), "java");
                    }
                }
                throw new IOException("Minecraft " + version + " is not in the version manifest");
            }));
        }

        private CompletableFuture<JsonObject> assetIndex(JsonObject json) {
            JsonObject info = json.getAsJsonObject("assetIndex");
            String id = info.get("id").getAsString();
            Path local = minecraft.resolve("assets").resolve("indexes").resolve(id + ".json");
            return shared(Arrays.asList("index", id), () -> {
                JsonObject index = Files.exists(local) ? AssetMoverHelper.json(local) : AssetMoverHelper.json(info.get("url").getAsString(), "java");
                return index.getAsJsonObject("objects");
            });
        }

        private void moveObject(String hash, Path destination) throws IOException {
            Path local = minecraft.resolve("assets").resolve("objects").resolve(hash.substring(0, 2)).resolve(hash);
            Path file = Files.exists(local) && AssetMoverHelper.matches(local, "SHA-1", hash)
                ? local
                : AssetMoverHelper.download(RESOURCES + hash.substring(0, 2) + "/" + hash, "java");
            AssetMoverHelper.verify(file, "SHA-1", hash);
            AssetMoverHelper.copy(file, destination);
        }

        private CompletableFuture<Path> clientJar(JsonObject json) {
            JsonObject client = json.getAsJsonObject("downloads").getAsJsonObject("client");
            String sha1 = client.get("sha1").getAsString();
            Path local = minecraft.resolve("versions").resolve(version).resolve(version + ".jar");
            return shared(Arrays.asList("client", version), () -> {
                Path jar = Files.exists(local) && AssetMoverHelper.matches(local, "SHA-1", sha1) ? local : AssetMoverHelper.download(client.get("url").getAsString(), "java");
                AssetMoverHelper.verify(jar, "SHA-1", sha1);
                return jar;
            });
        }

    }

    private final class SingleFile extends Request implements SingleAssetRequest {

        private final String url;
        private final String destination;

        SingleFile(String url, String destination) {
            this.url = url;
            this.destination = destination;
        }

        @Override
        public SingleAssetRequest overwrite() {
            overwrite = true;
            return this;
        }

        @Override
        public SingleAssetRequest verify(String checksum) {
            return verify(AssetMoverHelper.algorithmOf(checksum), checksum);
        }

        @Override
        public SingleAssetRequest verify(String algorithm, String checksum) {
            setChecksum(algorithm, checksum);
            return this;
        }

        @Override
        public String toString() {
            return url;
        }

        @Override
        CompletableFuture<?> start() {
            Path to = PACK.resolve(destination);
            if (!overwrite && Files.exists(to)) {
                return CompletableFuture.completedFuture(null);
            }
            return async(() -> {
                Path file = AssetMoverHelper.download(url, "url");
                AssetMoverHelper.verify(file, algorithm, checksum);
                AssetMoverHelper.copy(file, to);
                return null;
            });
        }

    }

}
