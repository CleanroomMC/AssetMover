package io.github.cleanroommc.assetmover;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public enum MinecraftVersion {

    V1_16_5("1.16.5", "37fd3c903861eeff3bc24b71eed48f828b5269c8", 17547153),
    V1_17_1("1.17.1", "8d9b65467c7913fcf6f5b2e729d44a1e00fde150", 19546842),
    V1_18_1("1.18.1", "7e46fb47609401970e2818989fa584fd467cd036", 20042090);

    private static final Map<String, MinecraftVersion> VERSIONS = new Object2ObjectOpenHashMap<>();

    static {
        VERSIONS.put(V1_16_5.version, V1_16_5);
        VERSIONS.put(V1_17_1.version, V1_17_1);
        VERSIONS.put(V1_18_1.version, V1_18_1);
    }

    public static MinecraftVersion grab(String version) {
        return VERSIONS.get(version);
    }

    private final String version, SHA1;
    private final long size;

    MinecraftVersion(String version, String SHA1, long size) {
        this.version = version;
        this.SHA1 = SHA1;
        this.size = size;
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
