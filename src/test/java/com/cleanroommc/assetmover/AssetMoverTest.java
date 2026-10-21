/*
 * Copyright (c) 2022-2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.assetmover;

import com.cleanroommc.assetmover.api.GitHost;

import com.google.common.hash.Hashing;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import net.minecraft.launchwrapper.Launch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class AssetMoverTest {

    private static final Map<String, byte[]> FILES = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> HITS = new ConcurrentHashMap<>();

    @TempDir static Path home;

    private static HttpServer server;
    private static String base;

    @BeforeAll
    static void serve() throws IOException {
        Launch.minecraftHome = home.toFile();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            HITS.computeIfAbsent(path, k -> new AtomicInteger()).incrementAndGet();
            if (path.equals("/stall")) {
                exchange.sendResponseHeaders(200, 0);
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException ignored) {
                    // Interrupted by server.stop, the client has already given up
                }
                exchange.close();
                return;
            }
            byte[] body = FILES.get(path);
            exchange.sendResponseHeaders(body == null ? 404 : 200, body == null ? -1 : body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                if (body != null) {
                    out.write(body);
                }
            }
        });
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
        FILES.put("/repo.zip", zip("repo-1/a.txt", "a", "repo-1/b/c.txt", "c"));
    }

    @AfterAll
    static void stop() {
        server.stop(0);
    }

    private static Path pack(String path) {
        return AssetMover.PACK.resolve(path);
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(pack(path)), StandardCharsets.UTF_8);
    }

    private static byte[] zip(String... entries) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            for (int i = 0; i < entries.length; i += 2) {
                out.putNextEntry(new ZipEntry(entries[i]));
                out.write(entries[i + 1].getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return bytes.toByteArray();
    }

    @Test
    void movesFromArchiveWithSingleRootFolder() throws IOException {
        AssetMover mover = new AssetMover();
        mover.fromUrl(base + "/repo.zip").move("a.txt").moveAs("b/c.txt", "root/c.txt");
        mover.run();
        assertThat(read("a.txt")).isEqualTo("a");
        assertThat(read("root/c.txt")).isEqualTo("c");
    }

    @Test
    void sharesOneDownloadBetweenRequests() {
        FILES.put("/shared.zip", zip("x.txt", "x", "y.txt", "y"));
        AssetMover mover = new AssetMover();
        mover.fromUrl(base + "/shared.zip").moveAs("x.txt", "shared/x.txt");
        mover.fromUrl(base + "/shared.zip").moveAs("y.txt", "shared/y.txt");
        mover.run();
        assertThat(pack("shared/x.txt")).exists();
        assertThat(pack("shared/y.txt")).exists();
        assertThat(HITS.get("/shared.zip")).hasValue(1);
    }

    @Test
    void skipsExistingDestinationsUnlessOverwritten() throws IOException {
        FILES.put("/skip.zip", zip("s.txt", "new"));
        Files.createDirectories(pack("skip"));
        Files.write(pack("skip/s.txt"), "old".getBytes(StandardCharsets.UTF_8));
        AssetMover mover = new AssetMover();
        mover.fromUrl(base + "/skip.zip").moveAs("s.txt", "skip/s.txt");
        mover.run();
        assertThat(read("skip/s.txt")).isEqualTo("old");
        assertThat(HITS).doesNotContainKey("/skip.zip");

        mover = new AssetMover();
        mover.fromUrl(base + "/skip.zip").moveAs("s.txt", "skip/s.txt").overwrite();
        mover.run();
        assertThat(read("skip/s.txt")).isEqualTo("new");
    }

    @Test
    void movesNothingOutOfAFileFailingItsChecksum() {
        byte[] archive = zip("v.txt", "v");
        FILES.put("/verify.zip", archive);
        String sha256 = Hashing.sha256().hashBytes(archive).toString();
        AssetMover mover = new AssetMover();
        mover.fromUrl(base + "/verify.zip").moveAs("v.txt", "verify/good.txt").verify(sha256);
        mover.fromUrl(base + "/verify.zip")
            .moveAs("v.txt", "verify/bad.txt")
            .verify(Hashing.sha256().hashBytes(new byte[0]).toString());
        mover.run();
        assertThat(pack("verify/good.txt")).exists();
        assertThat(pack("verify/bad.txt")).doesNotExist();
    }

    @Test
    void resolvesMavenCoordinates() {
        FILES.put("/maven/g/h/art/1.0/art-1.0-cls.zip", zip("m.txt", "m"));
        AssetMover mover = new AssetMover();
        mover.fromMaven(base + "/maven", "g.h:art:1.0:cls@zip").moveAs("m.txt", "maven/m.txt");
        mover.run();
        assertThat(pack("maven/m.txt")).exists();
    }

    @Test
    void writesSingleFiles() throws IOException {
        FILES.put("/file.txt", "f".getBytes(StandardCharsets.UTF_8));
        AssetMover mover = new AssetMover();
        mover.fromUrlFile(base + "/file.txt", "single/file.txt");
        mover.run();
        assertThat(read("single/file.txt")).isEqualTo("f");
    }

    @Test
    @Timeout(5)
    void givesUpOnAStalledServer() {
        System.setProperty("cleanroom.asset-mover.url.timeout", "1");
        try {
            AssetMover mover = new AssetMover();
            mover.fromUrlFile(base + "/stall", "stall.txt");
            mover.run();
        } finally {
            System.clearProperty("cleanroom.asset-mover.url.timeout");
        }
        assertThat(pack("stall.txt")).doesNotExist();
    }

    @Test
    void buildsForgeUrls() throws IOException {
        assertThat(GitHost.GITHUB.archiveUrl("o/r", "v1")).isEqualTo("https://github.com/o/r/archive/v1.zip");
        assertThat(GitHost.GITHUB.releaseUrl("o/r", "v1", "f.jar")).isEqualTo(
            "https://github.com/o/r/releases/download/v1/f.jar"
        );
        assertThat(GitHost.gitlab("https://git.example.com/").archiveUrl("g/s/r", "feature/x")).isEqualTo(
            "https://git.example.com/g/s/r/-/archive/feature/x/r-feature-x.zip"
        );
    }

}
