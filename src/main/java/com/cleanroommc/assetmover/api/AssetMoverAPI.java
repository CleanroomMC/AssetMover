package com.cleanroommc.assetmover.api;

/**
 * The one entry point for moving assets. Obtain it from {@link AssetMoveEvent#getAssetMover()}.
 * <p>Every method queues a request and returns immediately.
 * Nothing is downloaded until every listener of the event has been served.
 * At which point AssetMover downloads and moves everything in parallel.
 */
public interface AssetMoverAPI {

    /**
     * Assets of a Minecraft: Java Edition version, e.g. {@code "1.20.1"}.
     * <p>Sources the local launcher installation when possible, falls back to Mojang's servers.
     * Both the asset index ({@code assets/...}) and the client jar are searched, in that order.
     */
    AssetRequest fromJava(String version);

    /**
     * Assets of a Minecraft: Bedrock Edition version, e.g. {@code "1.21.0"}.
     * Taken from Mojang's <a href="https://github.com/Mojang/bedrock-samples">bedrock-samples</a> repository.
     * <p>Paths are relative to the repository root, e.g. {@code "resource_pack/textures/blocks/dirt.png"}.
     */
    AssetRequest fromBedrock(String version);

    /**
     * A file hosted on a mod site
     *
     * @param projectId see {@link ModSite} for more information
     * @param fileId    see {@link ModSite} for more information
     */
    AssetRequest fromModSite(ModSite site, String projectId, String fileId);

    /**
     * An artifact from any maven repository.
     *
     * @param repositoryUrl root of the repository, e.g. {@code "https://maven.cleanroommc.com"}
     * @param coordinate    {@code group:artifact:version[:classifier][@extension]}, extension defaults to {@code jar}
     */
    AssetRequest fromMaven(String repositoryUrl, String coordinate);

    /**
     * An artifact from Maven Central, see {@link #fromMaven(String, String)}.
     */
    AssetRequest fromMavenCentral(String coordinate);

    /**
     * Any zip/jar reachable over http(s). If the archive has a single root folder, paths may omit it.
     */
    AssetRequest fromUrl(String archiveUrl);

    /**
     * A single file reachable over http(s), written straight to {@code destination}.
     */
    SingleAssetRequest fromUrlFile(String fileUrl, String destination);

}
