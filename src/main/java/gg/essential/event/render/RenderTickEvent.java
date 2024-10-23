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
package gg.essential.event.render;

import gg.essential.universal.UMatrixStack;

public final class RenderTickEvent {

    private final boolean pre;
    private final boolean loadingScreen;

    private final UMatrixStack matrixStack;
    private final float partialTicksMenu;
    private final float partialTicksInGame;

    public RenderTickEvent(boolean pre, boolean loadingScreen, UMatrixStack matrixStack, float partialTicksMenu, float partialTicksInGame) {
        this.pre = pre;
        this.loadingScreen = loadingScreen;
        this.matrixStack = matrixStack;
        this.partialTicksMenu = partialTicksMenu;
        this.partialTicksInGame = partialTicksInGame;
    }

    public boolean isPre() {
        return pre;
    }

    public boolean isLoadingScreen() {
        return loadingScreen;
    }

    public UMatrixStack getMatrixStack() {
        return matrixStack;
    }

    public float getPartialTicksMenu() {
        return partialTicksMenu;
    }

    public float getPartialTicksInGame() {
        return partialTicksInGame;
    }
}
