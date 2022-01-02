package io.github.cleanroommc.assetmover;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ListenableFuture;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.util.HttpUtil;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.asm.ModAnnotation;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Mod(modid = AssetMover.MODID, name = AssetMover.NAME, version = AssetMover.VERSION)
public class AssetMover {

    public static final String MODID = "assetmover";
    public static final String NAME = "AssetMover";
    public static final String VERSION = "@VERSION@";

    @Mod.EventHandler
    @SuppressWarnings("unchecked")
    public void construct(FMLConstructionEvent event) {
        Logger logger = LogManager.getLogger("AssetMover");
        Map<MinecraftVersion, ListenableFuture<Path>> minecraftVersions = new Object2ObjectOpenHashMap<>();
        Map<MinecraftVersion, List<Pair<Path, List<String>>>> assetRequesters = new Object2ObjectOpenHashMap<>();
        for (ASMDataTable.ASMData asmData : event.getASMHarvestedData().getAll(RequestMinecraftAssets.class.getName())) {
            List<String> data = new ArrayList<>();
            File file = asmData.getCandidate().getModContainer();
            Path path;
            if (FMLLaunchHandler.isDeobfuscatedEnvironment()) {
                File buildFolder = file.getParentFile().getParentFile().getParentFile();
                path = new File(buildFolder, "/resources/main").toPath();
                for (String d : (List<String>) asmData.getAnnotationInfo().get("data")) {
                    try {
                        if (!Files.exists(path.resolve(d))) {
                            data.add(d);
                        }
                    } catch (InvalidPathException e) {
                        e.printStackTrace();
                        logger.error("Path {} is not formatted correctly!", d);
                    }
                }
            } else {
                path = file.toPath();
                try (FileSystem fs = FileSystems.newFileSystem(path, null)) {
                    for (String d : (List<String>) asmData.getAnnotationInfo().get("data")) {
                        if (!Files.exists(fs.getPath(d))) {
                            data.add(d);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (!data.isEmpty()) {
                MinecraftVersion version = MinecraftVersion.valueOf(((ModAnnotation.EnumHolder) asmData.getAnnotationInfo().get("version")).getValue());
                if (!minecraftVersions.containsKey(version)) {
                    minecraftVersions.put(version, HttpUtil.DOWNLOADER_EXECUTOR.submit(() -> {
                        try {
                            URLConnection conn = version.getURL().openConnection();
                            if (conn.getContentLengthLong() == version.getSize()) {
                                try (InputStream is = conn.getInputStream()) {
                                    Path temp = Files.createTempFile("client-" + version, ".jar");
                                    Stopwatch stopwatch = Stopwatch.createStarted();
                                    logger.warn("Downloading Minecraft {} for its assets", version.getVersion());
                                    Files.copy(is, temp, StandardCopyOption.REPLACE_EXISTING);
                                    logger.warn("Finished downloading Minecraft {} for its assets. It took {}", version.getVersion(), stopwatch.stop());
                                    temp.toFile().deleteOnExit();
                                    return temp;
                                } catch (Throwable t) {
                                    t.printStackTrace();
                                    try {
                                        logger.error(IOUtils.toString(((HttpURLConnection) conn).getErrorStream()));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else {
                                throw new RuntimeException("Minecraft " + version + " failed to be fetched as reported size is mismatched!");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }));
                }
                assetRequesters.computeIfAbsent(version, k -> new ArrayList<>()).add(Pair.of(path, data));
            }
        }
        minecraftVersions.forEach((v, lf) -> assetRequesters.get(v).forEach(pair -> {
            if (FMLLaunchHandler.isDeobfuscatedEnvironment()) {
                try {
                    Path assets = lf.get(2, TimeUnit.MINUTES);
                    try (FileSystem assetFs = FileSystems.newFileSystem(assets, null)) {
                        for (String data : pair.getRight()) {
                            Path resolvedDataPath = pair.getLeft().resolve(data);
                            resolvedDataPath.toFile().mkdirs();
                            Files.copy(assetFs.getPath(data), resolvedDataPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (ExecutionException | InterruptedException | TimeoutException e) {
                    e.printStackTrace();
                }
            } else {
                try (FileSystem modFs = FileSystems.newFileSystem(pair.getLeft(), null)) {
                    try {
                        Path assets = lf.get(2, TimeUnit.MINUTES);
                        try (FileSystem assetFs = FileSystems.newFileSystem(assets, null)) {
                            for (String data : pair.getRight()) {
                                Path resolvedDataPath = modFs.getPath(data);
                                resolvedDataPath.toFile().mkdirs();
                                Files.copy(assetFs.getPath(data), resolvedDataPath);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (ExecutionException | InterruptedException | TimeoutException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));

    }

}
