/*
 * Copyright (c) 2022-2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.assetmover.api;

import com.cleanroommc.assetmover.AssetMoverHelper;

import java.io.IOException;

/**
 * A mod hosting site AssetMover can pull a file from, see {@link AssetMoverAPI#fromModSite(ModSite, String, String)}.
 * <p>Implement it to support a site AssetMover does not ship with.
 */
@FunctionalInterface
public interface ModSite {

    /**
     * Both ids are numerical, e.g. {@code fromModSite(CURSEFORGE, "238222", "4593548")}.
     * <p>The project id is on the project page, the file id is the trailing number of the file's download page url.
     */
    ModSite CURSEFORGE = AssetMoverHelper::curseForge;

    /**
     * The project id is the slug or id, the file id is the version id or version number.
     * The primary file of the version is used, e.g. {@code fromModSite(MODRINTH, "jei", "AAAAAAAA")}.
     */
    ModSite MODRINTH = AssetMoverHelper::modrinth;

    /**
     * Resolves the download url of a file. Called off the main thread.
     *
     * @param projectId the id of the project on the site
     * @param fileId the id of the file within the project
     */
    String downloadUrl(String projectId, String fileId) throws IOException;

}
