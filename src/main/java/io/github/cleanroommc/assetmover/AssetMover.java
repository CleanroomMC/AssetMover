package io.github.cleanroommc.assetmover;

import com.google.common.util.concurrent.ListenableFuture;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.util.HttpUtil;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
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

    private static Logger logger;

    @Mod.EventHandler
    @SuppressWarnings("unchecked")
    public void construct(FMLConstructionEvent event) {
        logger = LogManager.getLogger("AssetMover");
        Map<MinecraftVersion, ListenableFuture<File>> minecraftVersions = new Object2ObjectOpenHashMap<>();
        Set<String> mods = new ObjectOpenHashSet<>();
        Map<MinecraftVersion, List<Pair<File, List<String>>>> assetRequesters = new Object2ObjectOpenHashMap<>();
        for (ASMDataTable.ASMData asmData : event.getASMHarvestedData().getAll(RequestAsset.class.getName())) {
            List<String> data = new ArrayList<>();
            File file = asmData.getCandidate().getModContainer();
            try (FileSystem fs = FileSystems.newFileSystem(file.toURI(), Collections.emptyMap())) {
                for (String d : (String[]) asmData.getAnnotationInfo().get("data")) {
                    if (!Files.exists(fs.getPath(d))) {
                        data.add(d);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!data.isEmpty()) {
                MinecraftVersion minecraftVersion = (MinecraftVersion) asmData.getAnnotationInfo().get("minecraftVersion");
                if (minecraftVersion != MinecraftVersion.NIL && !minecraftVersions.containsKey(minecraftVersion)) {
                    minecraftVersions.put(minecraftVersion, HttpUtil.DOWNLOADER_EXECUTOR.submit(() -> {
                        try {
                            URLConnection conn = minecraftVersion.getURL().openConnection();
                            if (conn.getContentLengthLong() == minecraftVersion.getSize()) {
                                try (InputStream is = conn.getInputStream()) {
                                    Path temp = Files.createTempFile("client-" + minecraftVersion, ".jar");
                                    Files.copy(is, temp, StandardCopyOption.REPLACE_EXISTING);
                                    return temp.toFile();
                                } catch (Throwable t) {
                                    t.printStackTrace();
                                    try {
                                        logger.error(IOUtils.toString(((HttpURLConnection) conn).getErrorStream()));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else {
                                throw new RuntimeException("Minecraft " + minecraftVersion + " failed to be fetched as reported size is mismatched!");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }));
                }
                String modURL = (String) asmData.getAnnotationInfo().get("modURL");
                if (!modURL.isEmpty()) {
                    mods.add(modURL);
                }
                assetRequesters.computeIfAbsent(minecraftVersion, k -> new ArrayList<>()).add(Pair.of(file, data));
            }
        }
        minecraftVersions.forEach((v, lf) -> assetRequesters.get(v).forEach(pair -> {
            try (FileSystem modFs = FileSystems.newFileSystem(pair.getLeft().toURI(), Collections.emptyMap())) {
                try {
                    File file = lf.get(2, TimeUnit.MINUTES);
                    try (FileSystem assetFs = FileSystems.newFileSystem(file.toURI(), Collections.emptyMap())) {
                        for (String data : pair.getRight()) {
                            Files.move(modFs.getPath(data), assetFs.getPath(data), StandardCopyOption.REPLACE_EXISTING);
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
        }));
        logger = null;
    }

}
