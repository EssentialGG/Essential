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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

public interface PriceHolder {

    @Nullable Map<@NotNull String, @NotNull Double> getPrices();

    default @NotNull Optional<@NotNull Double> getPriceOptional(final @NotNull String currency) {
        return Optional.ofNullable(this.getPrice(currency));
    }

    default @Nullable Double getPrice(final @NotNull String currency) {
        final @Nullable Map<@NotNull String, @NotNull Double> prices = this.getPrices();
        return (prices == null ? null : prices.get(currency));
    }

}
