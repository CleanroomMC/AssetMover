package com.cleanroommc.assetmover.api;

/**
 * Mod hosting sites AssetMover knows how to pull a file from.
 */
public enum ModSite {

    /**
     * Both ids are numerical, e.g. {@code fromModSite(CURSEFORGE, "238222", "4593548")}.
     * <p>The project id is on the project page, the file id is the trailing number of the file's download page url.
     */
    CURSEFORGE,

    /**
     * The project id is the slug or id, the file id is the <b>version id</b> (not the version number).
     * e.g. {@code fromModSite(MODRINTH, "jei", "AAAAAAAA")}.
     */
    MODRINTH

}
