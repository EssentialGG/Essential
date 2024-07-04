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
package gg.essential.cosmetics.holder;

import gg.essential.cosmetics.SkinLayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

public interface SkinLayersHolder {

    @Nullable Map<@NotNull SkinLayer, @NotNull Boolean> getSkinLayers();

    default @Nullable Boolean getSkinLayer(final @NotNull SkinLayer skinLayer) {
        final @Nullable Map<@NotNull SkinLayer, @NotNull Boolean> skinLayers = this.getSkinLayers();
        return skinLayers == null ? null : skinLayers.get(skinLayer);
    }

    default boolean getSkinLayer(
            final @NotNull SkinLayer skinLayer,
            final @NotNull Supplier<@NotNull Boolean> defaultSupplier
    ) {
        final @Nullable Boolean toggleState = this.getSkinLayer(skinLayer);
        return toggleState == null ? defaultSupplier.get() : toggleState.booleanValue();
    }

}
