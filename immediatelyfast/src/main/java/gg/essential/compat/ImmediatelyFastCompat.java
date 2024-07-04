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
package gg.essential.compat;

import net.raphimc.immediatelyfastapi.BatchingAccess;
import net.raphimc.immediatelyfastapi.ImmediatelyFastApi;

/**
 * We must disable ImmediatelyFast's batching for any custom rendering we do.
 * @see <a href="https://github.com/RaphiMC/ImmediatelyFast/issues/213">RaphiMC/ImmediatelyFast#213</a>
 * @see <a href="https://github.com/RaphiMC/ImmediatelyFast/blob/702a3aa9459e7079318dab322781b5f97ce9d899/common/src/main/java/net/raphimc/immediatelyfast/injection/mixins/hud_batching/compat/armorchroma/MixinArmorChroma_GuiArmor.java">
 *     MixinArmorChroma_GuiArmor</a>
 */
public class ImmediatelyFastCompat {
    private static final boolean IMMEDIATELYFAST_LOADED;
    static {
        boolean loaded;
        try {
            Class.forName("net.raphimc.immediatelyfastapi.ImmediatelyFastApi");
            loaded = true;
        } catch (ClassNotFoundException e) {
            loaded = false;
        }
        IMMEDIATELYFAST_LOADED = loaded;
    }

    private static boolean wasHudBatching = false;

    public static void beforeHudDraw() {
        if (!IMMEDIATELYFAST_LOADED) return;

        BatchingAccess access = ImmediatelyFastApi.getApiImpl().getBatching();
        if (access.isHudBatching()) {
            access.endHudBatching();
            wasHudBatching = true;
        }
    }

    public static void afterHudDraw() {
        if (!IMMEDIATELYFAST_LOADED) return;

        BatchingAccess access = ImmediatelyFastApi.getApiImpl().getBatching();
        if (wasHudBatching) {
            access.beginHudBatching();
            wasHudBatching = false;
        }
    }
}
