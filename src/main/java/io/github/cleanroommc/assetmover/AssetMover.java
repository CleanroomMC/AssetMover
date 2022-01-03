package io.github.cleanroommc.assetmover;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ListenableFuture;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.HttpUtil;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.asm.ModAnnotation;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
@RequestMinecraftAssets(version = MinecraftVersion.V1_18_1, targetNamespace = AssetMover.MODID, data = "assets/minecraft/textures/item/bundle.png")
public class AssetMover {

    public static final String MODID = "assetmover";
    public static final String NAME = "AssetMover";
    public static final String VERSION = "@VERSION@";

    private static String getModNamespacedData(String data, String search, String replacement) {
        return StringUtils.replace(data, search, replacement, 1);
    }

    @Mod.EventHandler
    @SuppressWarnings("unchecked")
    public void construct(FMLConstructionEvent event) {
        Logger logger = LogManager.getLogger("AssetMover");
        Path assetsPath = FMLLaunchHandler.isDeobfuscatedEnvironment() ? null : Launch.minecraftHome.toPath().resolve("assetmover");
        Map<MinecraftVersion, ListenableFuture<Path>> mcJars = new Object2ObjectOpenHashMap<>();
        Map<MinecraftVersion, List<Pair<Path, Map<String, String>>>> requests = new Object2ObjectOpenHashMap<>();
        for (ASMDataTable.ASMData asmData : event.getASMHarvestedData().getAll(RequestMinecraftAssets.class.getName())) {
            Map<String, String> requestedData = new Object2ObjectOpenHashMap<>();
            String id = (String) asmData.getAnnotationInfo().get("targetNamespace");
            File file = asmData.getCandidate().getModContainer();
            Path path;
            if (FMLLaunchHandler.isDeobfuscatedEnvironment()) {
                path = new File(file.getParentFile().getParentFile().getParentFile(), /*File.separatorChar + */"resources" + File.separatorChar + "main").toPath();
                for (String data : (List<String>) asmData.getAnnotationInfo().get("data")) {
                    String namespacedData = getModNamespacedData(data, "minecraft", id);
                    try {
                        if (!Files.exists(path.resolve(namespacedData))) {
                            requestedData.put(data, namespacedData);
                        }
                    } catch (InvalidPathException e) {
                        logger.error("Path {} is not formatted correctly!", namespacedData);
                    }
                }
            } else {
                path = assetsPath;
                try (FileSystem fs = FileSystems.newFileSystem(file.toPath(), null)) {
                    for (String data : (List<String>) asmData.getAnnotationInfo().get("data")) {
                        String namespacedData = getModNamespacedData(data, "minecraft", id);
                        try {
                            if (!Files.exists(fs.getPath(namespacedData))) {
                                requestedData.put(data, namespacedData);
                            }
                        } catch (InvalidPathException e) {
                            logger.error("Path {} is not formatted correctly!", namespacedData);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (!requestedData.isEmpty()) {
                MinecraftVersion version = MinecraftVersion.valueOf(((ModAnnotation.EnumHolder) asmData.getAnnotationInfo().get("version")).getValue());
                if (!mcJars.containsKey(version)) {
                    mcJars.put(version, HttpUtil.DOWNLOADER_EXECUTOR.submit(() -> {
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
                requests.computeIfAbsent(version, k -> new ArrayList<>()).add(Pair.of(path, requestedData));
            }
        }
        if (!requests.isEmpty()) {
            mcJars.forEach((v, lf) -> {
                try {
                    Path assets = lf.get(1, TimeUnit.MINUTES);
                    requests.get(v).forEach(pair -> {
                        try (FileSystem assetFs = FileSystems.newFileSystem(assets, null)) {
                            for (Map.Entry<String, String> data : pair.getRight().entrySet()) {
                                Path resolvedDataPath = pair.getLeft().resolve(data.getValue());
                                resolvedDataPath.toFile().mkdirs();
                                Files.copy(assetFs.getPath(data.getKey()), resolvedDataPath, StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                    assets.toFile().delete();
                } catch (ExecutionException | InterruptedException | TimeoutException e) {
                    e.printStackTrace();
                }
            });
        }
    }

}
