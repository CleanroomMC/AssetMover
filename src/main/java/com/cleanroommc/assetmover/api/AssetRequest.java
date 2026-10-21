/*
 * Copyright (c) 2022-2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.assetmover.api;

import java.util.Collection;
import java.util.Map;

/**
 * A queued request against one source (a Minecraft version, a mod jar, a maven artifact, an archive...).
 * <p>The request is queued the moment it is created, there is nothing to submit. Adding no assets to it simply
 * makes it a no-op, and its source is never downloaded.</p>
 * <p>All destinations are relative to {@code /assetmover}, which is a hidden resource pack.
 */
public interface AssetRequest {

    /**
     * Moves assets, keeping their paths.
     *
     * @param paths paths within the source
     */
    AssetRequest move(String... paths);

    /**
     * Moves assets, keeping their paths.
     *
     * @param paths paths within the source
     */
    AssetRequest move(Collection<String> paths);

    /**
     * Moves every {@code source -> destination} pair of the given map.
     *
     * @param assets paths within the source mapped to paths within the pack
     */
    AssetRequest move(Map<String, String> assets);

    /**
     * Moves an asset from {@code source} (path within the source) to {@code destination} (path within the pack).
     *
     * @param source the path within the source
     * @param destination the path within the pack
     */
    AssetRequest moveAs(String source, String destination);

    /**
     * By default, an asset whose destination already exists is skipped, and a source nobody needs is never
     * downloaded. Call this to re-acquire this request's assets on every launch.
     */
    AssetRequest overwrite();

    /**
     * The hash the downloaded file has to have. Nothing is moved out of a file that fails it.
     * The algorithm is taken from the length of the hash.
     * i.e. MD5, SHA-1, SHA-256 and SHA-512.
     * <p>Each request checks the file against its own checksum. Not applicable to
     * {@link AssetMoverAPI#fromJava(String)}, whose assets are always verified against Mojang's own hashes.
     *
     * @param checksum the expected hash, in hexadecimal
     */
    AssetRequest verify(String checksum);

    /**
     * {@link #verify(String)} with the algorithm explicitly stated.
     * e.g. {@code verify("SHA-256", "...")}.
     *
     * @param algorithm a {@link java.security.MessageDigest} algorithm name
     * @param checksum the expected hash, in hexadecimal
     */
    AssetRequest verify(String algorithm, String checksum);

}
