package io.github.cleanroommc.assetmover;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class MinecraftVersion {

    private static final Map<String, MinecraftVersion> VERSIONS = new Object2ObjectOpenHashMap<>();

    public static final MinecraftVersion V1_18_1 = new MinecraftVersion("1.18.1", "7e46fb47609401970e2818989fa584fd467cd036", 20042090);

    public static MinecraftVersion grab(String version) {
        return VERSIONS.get(version);
    }

    private final String version, SHA1;
    private final long size;

    private MinecraftVersion(String version, String SHA1, long size) {
        this.version = version;
        this.SHA1 = SHA1;
        this.size = size;
        VERSIONS.put(version, this);
    }

    public URL getURL() throws MalformedURLException {
        return new URL("https://launcher.mojang.com/v1/objects/" + SHA1 + "/client.jar");
    }

    public String getVersion() {
        return version;
    }

    public String getSHA1() {
        return SHA1;
    }

    public long getSize() {
        return size;
    }

}
