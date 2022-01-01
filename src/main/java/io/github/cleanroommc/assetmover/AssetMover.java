package io.github.cleanroommc.assetmover;

import com.google.common.util.concurrent.ListenableFuture;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.util.HttpUtil;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Mod(modid = AssetMover.MODID, name = AssetMover.NAME, version = AssetMover.VERSION)
public class AssetMover {

    public static final String MODID = "assetmover";
    public static final String NAME = "AssetMover";
    public static final String VERSION = "@VERSION@";

    private static final Logger logger = LogManager.getLogger("AssetMover");

    @Mod.EventHandler
    @SuppressWarnings("unchecked")
    public void construct(FMLConstructionEvent event) {
        Map<String, ListenableFuture<File>> minecraftVersions = new Object2ObjectOpenHashMap<>();
        Set<String> mods = new ObjectOpenHashSet<>();
        Map<File, List<String>> assetRequesters = new Object2ObjectOpenHashMap<>();
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
                String minecraftVersion = (String) asmData.getAnnotationInfo().get("minecraftVersion");
                if (!minecraftVersion.isEmpty() && !minecraftVersions.containsKey(minecraftVersion)) {
                    minecraftVersions.put(minecraftVersion, (ListenableFuture<File>) HttpUtil.DOWNLOADER_EXECUTOR.submit(() -> {
                        try {
                            MinecraftVersion minecraftVersionObject = MinecraftVersion.grab(minecraftVersion);
                            URLConnection conn = minecraftVersionObject.getURL().openConnection();
                            if (conn.getContentLengthLong() == minecraftVersionObject.getSize()) {
                                try (InputStream is = conn.getInputStream()) {
                                    Files.copy(is, Files.createTempFile("client-" + minecraftVersion, ".jar"), StandardCopyOption.REPLACE_EXISTING);
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
                    }));
                }
                String modURL = (String) asmData.getAnnotationInfo().get("modURL");
                if (!modURL.isEmpty()) {
                    mods.add(modURL);
                }
                assetRequesters.computeIfAbsent(file, k -> new ArrayList<>()).addAll(data);
            }
        }
    }

}
