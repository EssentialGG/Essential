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
/**
 * Minecraft in old versions synchronizes difficulty between the host and the integrated server by directly accessing
 * the client world from the server thread. This is done with no regard for thread safety and there are at least three
 * different race conditions just with vanilla. It gets worse once we add Essential cause we swap out the client world
 * during rendering of {@link gg.essential.gui.common.EmulatedUI3DPlayer}, which causes the server to randomly switch to
 * peaceful because that's the state in the fake world.
 *
 * The mixins in this package address these issues by re-implementing difficulty syncing (in both direction, so it also
 * fixes the complete lack of syncing for remote clients) via network packets and nukes any unsafe access.
 */
package gg.essential.mixins.transformers.feature.difficulty;