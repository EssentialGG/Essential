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
package gg.essential.mixins.transformers.item;

//#if MC > 10900 && MC<12102
import net.minecraft.item.ItemElytra;
//#endif
import gg.essential.api.cosmetics.RenderCosmetic;
import org.spongepowered.asm.mixin.Mixin;

// Elytra doesn't exist in 1.8
// ElytraItem class no longer exists in 1.21.2, now handled in PlayerWearableManager.canRenderCosmetic
//#if MC > 10900 && MC<12102
@Mixin(ItemElytra.class)
//#else
//$$ @Mixin(gg.essential.mixins.DummyTarget.class)
//#endif
public class MixinElytraItem implements RenderCosmetic {
}
