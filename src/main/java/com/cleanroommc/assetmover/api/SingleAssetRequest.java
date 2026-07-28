package com.cleanroommc.assetmover.api;

/**
 * A queued request for a single file whose destination is already known.
 * See {@link AssetMoverAPI#fromUrlFile(String, String)}.
 */
public interface SingleAssetRequest {

    /**
     * By default, the file is skipped if the destination already exists. Call this to re-download it on every launch.
     */
    SingleAssetRequest overwrite();

    /**
     * The hash the file has to have. A file that fails it never reaches the resource pack. The algorithm is
     * taken from the length of the hash.
     * i.e. MD5, SHA-1, SHA-256 and SHA-512.
     */
    SingleAssetRequest verify(String checksum);

    /**
     * {@link #verify(String)} with the algorithm explicitly stated.
     * e.g. {@code verify("SHA-256", "...")}.
     */
    SingleAssetRequest verify(String algorithm, String checksum);

}
