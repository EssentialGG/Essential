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
package gg.essential.mixins.transformers.compatibility.quark;

import gg.essential.mixins.impl.client.model.ModelBipedUtil;
import net.minecraft.client.model.ModelBiped;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Quark too implements emotes and ran into the issue solved by {@link ModelBipedUtil#resetPose(ModelBiped)} but decided
 * it'd be OK to just reset the values to 0 with no regard for mod compatibility. And they do so at the worst possible
 * time making it completely break any other mod that tries to use those fields in any sensible way.
 *
 * This mixin fixes that problem by simply "removing" their code, it's not needed after all because Essential's solution
 * already fixes the vanilla issue for all mods.
 */
@Pseudo
@Mixin(targets = {
    // Note: Not using pre-processor for these because the renames may have happened mid-version.
    "vazkii.quark.vanity.client.emotes.base.EmoteHandler", // 1.10, 1.11
    "vazkii.quark.vanity.client.emotes.EmoteHandler", // 1.12
    "vazkii.quark.vanity.client.emote.EmoteHandler", // 1.14
    "vazkii.quark.tweaks.client.emote.EmoteHandler", // 1.15
    "vazkii.quark.content.tweaks.client.emote.EmoteHandler", // 1.16+
}, remap = false)
public class Mixin_NukeModelReset {
    @Inject(method = "resetPart", at = @At("HEAD"), cancellable = true)
    private static void essentialTakesCareOfThisAndDoesItInACompatibleWay(CallbackInfo ci) {
        ci.cancel();
    }
}
