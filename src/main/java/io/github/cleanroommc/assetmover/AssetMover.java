package io.github.cleanroommc.assetmover;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ListenableFuture;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
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

@InternalModAnnotation(modid = AssetMover.MODID, name = AssetMover.NAME, version = AssetMover.VERSION)
@RequestMinecraftAssets(version = MinecraftVersion.V1_18_1, targetNamespace = AssetMover.MODID, data = "assets/minecraft/textures/item/bundle.png")
public class AssetMover {

    public static final String MODID = "assetmover";
    public static final String NAME = "AssetMover";
    public static final String VERSION = "0.1";

    private static String getModNamespacedData(String data, String search, String replacement) {
        return StringUtils.replace(data, search, replacement, 1);
    }

    @Mod.EventHandler
    @SuppressWarnings("unchecked")
    public void construct(FMLConstructionEvent event) {
        Logger logger = LogManager.getLogger("AssetMover");
        Map<MinecraftVersion, ListenableFuture<Path>> mcJars = new Object2ObjectOpenHashMap<>();
        Map<MinecraftVersion, List<Pair<Path, Map<String, String>>>> requests = new Object2ObjectOpenHashMap<>();
        for (ASMDataTable.ASMData asmData : event.getASMHarvestedData().getAll(RequestMinecraftAssets.class.getName())) {
            Map<String, String> requestedData = new Object2ObjectOpenHashMap<>();
            String id = (String) asmData.getAnnotationInfo().get("targetNamespace");
            File file = asmData.getCandidate().getModContainer();
            logger.info("Scanning {} for its requested assets", file.getName());
            Path path = FMLLaunchHandler.isDeobfuscatedEnvironment() ?
                    new File(file.getParentFile().getParentFile().getParentFile(), "resources" + File.separatorChar + "main").toPath() :
                    Launch.minecraftHome.toPath().resolve("assetmover");
            for (String data : (List<String>) asmData.getAnnotationInfo().get("data")) {
                String namespacedData = getModNamespacedData(data, "minecraft", id);
                try {
                    Path namespacedDataPath = path.resolve(namespacedData);
                    logger.debug("Checking if {} exists.", namespacedDataPath);
                    if (!Files.exists(namespacedDataPath)) {
                        logger.debug("{} does not exist, adding to requestedData queue.", namespacedDataPath);
                        requestedData.put(data, namespacedData);
                    } else {
                        logger.debug("{} does exist. Skipping.", namespacedDataPath);
                    }
                } catch (InvalidPathException e) {
                    logger.error("Path {} is not formatted correctly!", namespacedData);
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
                                Path assetPath = assetFs.getPath(data.getKey());
                                logger.info("Moving asset {} to {}", assetPath, resolvedDataPath);
                                Files.copy(assetPath, resolvedDataPath, StandardCopyOption.REPLACE_EXISTING);
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
