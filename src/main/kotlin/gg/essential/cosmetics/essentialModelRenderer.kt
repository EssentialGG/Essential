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
package gg.essential.cosmetics

import gg.essential.Essential
import gg.essential.gui.common.CosmeticHoverOutlineEffect
import gg.essential.mod.cosmetics.CAPE_DISABLED_COSMETIC_ID
import gg.essential.model.EnumPart
import gg.essential.model.backend.PlayerPose
import gg.essential.model.backend.RenderBackend
import gg.essential.model.util.UMatrixStack
import gg.essential.network.cosmetics.Cosmetic

//#if MC>=11600
//$$ import gg.essential.model.backend.minecraft.MinecraftRenderBackend
//$$ import net.minecraft.client.renderer.IRenderTypeBuffer
//#endif

fun WearablesManager.renderForHoverOutline(
    matrixStack: UMatrixStack,
    vertexConsumerProvider: RenderBackend.VertexConsumerProvider,
    pose: PlayerPose,
    skin: RenderBackend.Texture,
    parts: Set<EnumPart> = EnumPart.values().toSet(),
) {
    val outlineEffect = CosmeticHoverOutlineEffect.active ?: return

    vertexConsumerProvider.flush()

    for ((cosmetic, model) in models) {
        outlineEffect.allocOutlineBuffer(cosmetic).use {
            render(matrixStack, vertexConsumerProvider, model, pose, skin, parts)
            vertexConsumerProvider.flush()
        }
    }
}

// Used for fallback renderer, for MC renderer see Mixin_CosmeticHoverOutline_Cape
// `cape` may be null in case of third-party capes
fun renderCapeForHoverOutline(vertexConsumerProvider: RenderBackend.VertexConsumerProvider, cape: Cosmetic?, render: () -> Unit) {
    val outlineEffect = CosmeticHoverOutlineEffect.active ?: return
    val cosmeticsData = Essential.getInstance().connectionManager.cosmeticsManager.cosmeticsData
    val cosmetic = cape ?: cosmeticsData.getCosmetic(CAPE_DISABLED_COSMETIC_ID) ?: return

    vertexConsumerProvider.flush()

    outlineEffect.allocOutlineBuffer(cosmetic).use {
        render()
        vertexConsumerProvider.flush()
    }
}

fun RenderBackend.VertexConsumerProvider.flush() {
    //#if MC>=11600
    //$$ ((this as MinecraftRenderBackend.VertexConsumerProvider).provider as? IRenderTypeBuffer.Impl)?.finish()
    //#endif
}
