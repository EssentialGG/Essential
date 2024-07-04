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
package gg.essential.handlers;

import gg.essential.config.EssentialConfig;
import gg.essential.elementa.constraints.animation.Animations;
import gg.essential.event.gui.MouseScrollEvent;
import gg.essential.universal.UMinecraft;
import gg.essential.util.OptiFineUtil;
import me.kbrewster.eventbus.Subscribe;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

public class ZoomHandler {
    private static ZoomHandler instance;
    private boolean oldSmoothCamera;
    public boolean isZoomActive;
    public KeyBinding zoomKeybinding;

    private static final float normalModifier = 4f;
    private float currentModifier = normalModifier;
    private float desiredModifier = currentModifier;
    private float smoothZoomProgress = 0f;
    private boolean hasScrolledYet = false;
    private boolean zoomingOut = false;
    private long timeOfLastUpdate = System.currentTimeMillis();

    private boolean isZoomToggled = false;
    private boolean isZoomBeingHeld = false;

    public static ZoomHandler getInstance() {
        if (instance == null) instance = new ZoomHandler();
        return instance;
    }

    private boolean getZoomState() {
        if (!EssentialConfig.INSTANCE.getEssentialEnabled()) return false;
        boolean down = zoomKeybinding.isKeyDown();
        if (!EssentialConfig.INSTANCE.getToggleToZoom()) return down;
        if (down) {
            if (isZoomBeingHeld) return isZoomToggled;
            isZoomBeingHeld = true;
            isZoomToggled = !isZoomToggled;
        } else {
            isZoomBeingHeld = false;
        }
        return isZoomToggled;
    }

    public float applyModifiers(float f) {
        if (OptiFineUtil.isLoaded()) return f;
        GameSettings settings = UMinecraft.getSettings();
        if (UMinecraft.getMinecraft().currentScreen == null && getZoomState()) {
            if (!isZoomActive) {
                isZoomActive = true;
                timeOfLastUpdate = System.currentTimeMillis();
                if (EssentialConfig.INSTANCE.getZoomSmoothCamera()) {
                    oldSmoothCamera = settings.smoothCamera;
                    settings.smoothCamera = true;
                }
                currentModifier = desiredModifier = normalModifier;
            }
            f /= getZoomHeldModifier();
            if (EssentialConfig.INSTANCE.getSmoothZoomAnimation()) {
                f *= getConstantModifier();
            }
        } else {
            if (isZoomActive) {
                isZoomActive = false;
                hasScrolledYet = false;
                settings.smoothCamera = oldSmoothCamera;
                queueTerrainUpdate();
                zoomingOut = true;
            }
            if (zoomingOut) {
                if (EssentialConfig.INSTANCE.getSmoothZoomAnimation()) {
                    if (smoothZoomProgress > 0) {
                        f *= getConstantModifier();
                    } else {
                        zoomingOut = false;
                    }
                } else zoomingOut = false;
            }
        }
        return f;
    }

    @Subscribe
    public void onMouseScroll(MouseScrollEvent event) {
        if (!isZoomActive || !getZoomState() || event.getScreen() != null) {
            return;
        }

        event.setCancelled(true);

        double moved = event.getAmount();

        if (moved > 0) {
            smoothZoomProgress = 0f;
            hasScrolledYet = true;
            desiredModifier += 0.25f * desiredModifier;
        } else if (moved < 0) {
            smoothZoomProgress = 0f;
            hasScrolledYet = true;
            desiredModifier -= 0.25f * desiredModifier;
            queueTerrainUpdate();
        }
    }

    private float getZoomHeldModifier() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastUpdate = currentTime - timeOfLastUpdate;

        if (desiredModifier < 1f) {
            desiredModifier = 1f;
        }

        if (desiredModifier > 600) {
            desiredModifier = 600f;
        }
        if (EssentialConfig.INSTANCE.getSmoothZoomAnimation()) {
            if (hasScrolledYet && smoothZoomProgress < 1) {
                queueTerrainUpdate();
                smoothZoomProgress += 0.004F * timeSinceLastUpdate;
                smoothZoomProgress = smoothZoomProgress > 1 ? 1 : smoothZoomProgress;
                return currentModifier += (desiredModifier - currentModifier) * calculateEasing(smoothZoomProgress);
            }
        } else currentModifier = desiredModifier;
        return desiredModifier;
    }

    private float getConstantModifier() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastUpdate = currentTime - timeOfLastUpdate;
        timeOfLastUpdate = currentTime;
        if (isZoomActive) {
            if (hasScrolledYet) return 1f;
            if (smoothZoomProgress < 1) {
                smoothZoomProgress += 0.005F * timeSinceLastUpdate;
                smoothZoomProgress = smoothZoomProgress > 1 ? 1 : smoothZoomProgress;
                return 4f - 3f * calculateEasing(smoothZoomProgress);
            }
        } else {
            if (hasScrolledYet) {
                hasScrolledYet = false;
                smoothZoomProgress = 1f;
            }
            if (smoothZoomProgress > 0) {
                smoothZoomProgress -= 0.005F * timeSinceLastUpdate;
                smoothZoomProgress = smoothZoomProgress < 0 ? 0 : smoothZoomProgress;
                queueTerrainUpdate();
                float progress = 1 - smoothZoomProgress;
                float diff =  1f / currentModifier;
                return diff + (1 - diff) * calculateEasing(progress);
            }
        }
        return 1f;
    }

    private void queueTerrainUpdate() {
        UMinecraft.getMinecraft().renderGlobal.setDisplayListEntitiesDirty();
    }

    private float calculateEasing(float progress) {
        switch (EssentialConfig.INSTANCE.getSmoothZoomAlgorithm()) {
            case 0:
                return Animations.IN_OUT_QUAD.getValue(progress);

            case 1:
                return Animations.IN_OUT_CIRCULAR.getValue(progress);

            case 2:
                return Animations.OUT_QUINT.getValue(progress);
        }

        // fallback
        return Animations.IN_OUT_QUAD.getValue(progress);
    }
}
