/*
 * Copyright (c) 2022-2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.assetmover.api;

/**
 * The one entry point for moving assets. Obtain it from {@link MoveAssetEvent#getAssetMover()}.
 * <p>Every method queues a request and returns immediately.
 * Nothing is downloaded until every listener of the event has been served.
 * At which point AssetMover downloads and moves everything in parallel.
 */
public interface AssetMoverAPI {

    /**
     * Assets of a Minecraft: Java Edition version, e.g. {@code "1.20.1"}.
     * <p>Sources the local launcher installation when possible, falls back to Mojang's servers.
     * Both the asset index ({@code assets/...}) and the client jar are searched, in that order.
     *
     * @param version the version id as the launcher names it
     */
    AssetRequest fromJava(String version);

    /**
     * Assets of a Minecraft: Bedrock Edition version, e.g. {@code "1.21.0.3"}.
     * Taken from Mojang's <a href="https://github.com/Mojang/bedrock-samples">bedrock-samples</a> repository.
     * <p>Paths are relative to the repository root, e.g. {@code "resource_pack/textures/blocks/dirt.png"}.
     *
     * @param version the version, which names the {@code v<version>} tag of the repository
     */
    AssetRequest fromBedrock(String version);

    /**
     * A file hosted on a mod site, see {@link ModSite} for what the ids are.
     *
     * @param site the site hosting the file
     * @param projectId the id of the project on that site
     * @param fileId the id of the file within the project
     */
    AssetRequest fromModSite(ModSite site, String projectId, String fileId);

    /**
     * The source archive of a git repository at a branch, tag or commit.
     * <p>Paths are relative to the repository root, e.g. {@code fromGit(GitHost.GITHUB, "Mojang/bedrock-samples", "v1.21.0.3")}.
     * <p>Forges build these archives on demand and do not promise the same bytes each time, so
     * {@link AssetRequest#verify(String)} may start failing for no fault of the repository.
     *
     * @param host the forge hosting the repository
     * @param repository the path of the repository on the host, e.g. {@code owner/name} or {@code group/subgroup/name}
     * @param ref        a branch, tag or commit
     */
    AssetRequest fromGit(GitHost host, String repository, String ref);

    /**
     * An archive attached to a release.
     *
     * @param host the forge hosting the repository
     * @param repository the path of the repository on the host, e.g. {@code owner/name}
     * @param tag        the tag the release was made from
     * @param fileName   the name of the attached file, e.g. {@code "mymod-1.0.0.jar"}
     */
    AssetRequest fromGitRelease(GitHost host, String repository, String tag, String fileName);

    /**
     * An artifact from any maven repository.
     *
     * @param repositoryUrl root of the repository, e.g. {@code "https://maven.cleanroommc.com"}
     * @param coordinate    {@code group:artifact:version[:classifier][@extension]}, extension defaults to {@code jar}
     */
    AssetRequest fromMaven(String repositoryUrl, String coordinate);

    /**
     * An artifact from Maven Central, see {@link #fromMaven(String, String)}.
     *
     * @param coordinate {@code group:artifact:version[:classifier][@extension]}
     */
    AssetRequest fromMavenCentral(String coordinate);

    /**
     * Any zip/jar reachable over http(s). If the archive has a single root folder, paths may omit it.
     *
     * @param archiveUrl the url of the zip or jar
     */
    AssetRequest fromUrl(String archiveUrl);

    /**
     * A single file reachable over http(s), written straight to {@code destination}.
     *
     * @param fileUrl the url of the file
     * @param destination the path within the pack
     */
    SingleAssetRequest fromUrlFile(String fileUrl, String destination);

}
