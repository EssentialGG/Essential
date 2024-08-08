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
package gg.essential.coins.model;

import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.model.EssentialAsset;
import org.jetbrains.annotations.NotNull;

public class CoinBundle {

    private final @NotNull String id;

    private final int coins;

    private final double price;

    @SerializedName("extra_percent")
    private final float extraPercent;

    private final @NotNull EssentialAsset icon;

    private final boolean highlighted;

    @SerializedName("exchange_bundle")
    private final boolean exchangeBundle;

    public CoinBundle(@NotNull String id, int coins, double price, int extraPercent, @NotNull EssentialAsset icon, boolean highlighted, boolean exchangeBundle) {
        this.id = id;
        this.coins = coins;
        this.price = price;
        this.extraPercent = extraPercent;
        this.icon = icon;
        this.highlighted = highlighted;
        this.exchangeBundle = exchangeBundle;
    }

    public @NotNull String getId() {
        return id;
    }

    public int getCoins() {
        return coins;
    }

    public double getPrice() {
        return price;
    }

    public float getExtraPercent() {
        return extraPercent;
    }

    public @NotNull EssentialAsset getIcon() {
        return icon;
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    public boolean isExchangeBundle() {
        return exchangeBundle;
    }

}
