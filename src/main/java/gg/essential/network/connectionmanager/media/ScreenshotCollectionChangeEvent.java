/*
 * Copyright (c) 2024 ModCore Inc. All rights reserved.
 *
 * This code is part of ModCore Inc.'s Essential Mod repository and is protected
 * under copyright registration # TX0009138511. For the full license, see:
 * https://github.com/EssentialGG/Essential/blob/main/LICENSE
 *
 * You may not use, copy, reproduce, modify, sell, license, distribute,
 * commercialize, or otherwise exploit, or create derivative works based
 * upon, this file or any other in this repository, all of which is reserved by Essential.
 */
package gg.essential.network.connectionmanager.media;

import java.nio.file.Path;
import java.util.Set;

public class ScreenshotCollectionChangeEvent {

    private final boolean screenshotsCreated;
    private final Set<Path> deletedPaths;

    public ScreenshotCollectionChangeEvent(boolean screenshotsCreated, Set<Path> deletedPaths) {
        this.screenshotsCreated = screenshotsCreated;
        this.deletedPaths = deletedPaths;
    }

    public boolean screenshotsDeleted() {
        return !this.deletedPaths.isEmpty();
    }

    public boolean screenshotsCreated() {
        return screenshotsCreated;
    }

    public Set<Path> getDeletedPaths() {
        return deletedPaths;
    }
}
