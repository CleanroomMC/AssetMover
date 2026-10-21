/*
 * Copyright (c) 2022-2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.assetmover.api;

import com.cleanroommc.assetmover.AssetMoverHelper;

import java.io.IOException;

/**
 * A git forge AssetMover can pull archives from, see {@link AssetMoverAPI#fromGit(GitHost, String, String)}.
 * <p>Use {@link #gitlab(String)} or {@link #gitea(String)} for a self-hosted instance, or implement it for any other forge.
 */
public interface GitHost {

    /**
     * github.com, which shares its url layout with Gitea.
     */
    GitHost GITHUB = gitea("https://github.com");

    /**
     * gitlab.com.
     */
    GitHost GITLAB = gitlab("https://gitlab.com");

    /**
     * codeberg.org.
     */
    GitHost CODEBERG = gitea("https://codeberg.org");

    /**
     * A GitLab instance, e.g. {@code gitlab("https://git.example.com")}.
     *
     * @param url the root url of the instance
     */
    static GitHost gitlab(String url) {
        return AssetMoverHelper.gitlab(url);
    }

    /**
     * A Gitea or Forgejo instance, e.g. {@code gitea("https://git.example.com")}.
     *
     * @param url the root url of the instance
     */
    static GitHost gitea(String url) {
        return AssetMoverHelper.gitea(url);
    }

    /**
     * The url of a zip of the repository at a branch, tag or commit.
     *
     * @param repository the path of the repository on the host
     * @param ref a branch, tag or commit
     */
    String archiveUrl(String repository, String ref) throws IOException;

    /**
     * The url of a file attached to a release. Called off the main thread.
     *
     * @param repository the path of the repository on the host
     * @param tag the tag the release was made from
     * @param fileName the name of the attached file
     */
    String releaseUrl(String repository, String tag, String fileName) throws IOException;

}
