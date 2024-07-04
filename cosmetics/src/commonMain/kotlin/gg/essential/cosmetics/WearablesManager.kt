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

import gg.essential.cosmetics.events.AnimationTarget
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.model.EnumPart
import gg.essential.model.ModelAnimationState
import gg.essential.model.ModelInstance
import gg.essential.model.RenderMetadata
import gg.essential.model.backend.PlayerPose
import gg.essential.model.backend.RenderBackend
import gg.essential.model.backend.atlas.TextureAtlas
import gg.essential.model.molang.MolangQueryEntity
import gg.essential.model.util.UMatrixStack
import gg.essential.network.cosmetics.Cosmetic

class WearablesManager(
    private val renderBackend: RenderBackend,
    private val entity: MolangQueryEntity,
    private val animationTargets: Set<AnimationTarget>,
    private val onAnimation: (Cosmetic, String) -> Unit,
) {
    var state: CosmeticsState = CosmeticsState.EMPTY
        private set

    var models: Map<Cosmetic, ModelInstance> = emptyMap()
        private set

    private var translucentTextureAtlas: TextureAtlas? = null

    fun updateState(newState: CosmeticsState) {
        val oldModels = models
        val oldTextures = oldModels.values.filter { it.model.translucent }.mapNotNull { it.model.texture }.distinct()

        val newModels =
            newState.bedrockModels
                .map { (cosmetic, bedrockModel) ->
                    val wearable = oldModels[cosmetic]
                    if (wearable == null) {
                        ModelInstance(bedrockModel, entity, animationTargets) { onAnimation(cosmetic, it) }
                    } else {
                        wearable.switchModel(bedrockModel)
                        wearable
                    }
                }
                .sortedBy { it.model.translucent } // render opaque models first
                .associateBy { it.cosmetic }

        // If there's more than one translucent model, we need to render them all in a single (sorted) pass
        val newTextures = newModels.values.filter { it.model.translucent }.mapNotNull { it.model.texture }.distinct()
        if (oldTextures != newTextures) {
            translucentTextureAtlas?.close()
            translucentTextureAtlas = null
        }
        if (translucentTextureAtlas == null && newTextures.size > 1) {
            translucentTextureAtlas = TextureAtlas.create(renderBackend, "cosmetics-${atlasCounter++}", newTextures)
        }

        for ((cosmetic, model) in models.entries) {
            if (newModels[cosmetic] != model) {
                model.locator.isValid = false
            }
        }
        state = newState
        models = newModels
    }

    fun resetModel(slot: CosmeticSlot) {
        updateState(state.copyWithout(slot))
    }

    fun render(
        matrixStack: UMatrixStack,
        vertexConsumerProvider: RenderBackend.VertexConsumerProvider,
        pose: PlayerPose,
        skin: RenderBackend.Texture,
        parts: Set<EnumPart> = EnumPart.values().toSet(),
    ) {
        for ((_, model) in models) {
            if (model.model.translucent && translucentTextureAtlas != null) {
                continue // will do these later in a single final pass
            }
            render(matrixStack, vertexConsumerProvider, model, pose, skin, parts)
        }

        val atlas = translucentTextureAtlas
        if (atlas != null) {
            vertexConsumerProvider.provide(atlas.atlasTexture) { vertexConsumer ->
                val atlasVertexConsumerProvider = RenderBackend.VertexConsumerProvider { texture, block ->
                    block(atlas.offsetVertexConsumer(texture, vertexConsumer))
                }
                for ((_, model) in models) {
                    if (model.model.translucent) {
                        render(matrixStack, atlasVertexConsumerProvider, model, pose, skin, parts)
                    }
                }
            }
        }
    }

    fun render(
        matrixStack: UMatrixStack,
        vertexConsumerProvider: RenderBackend.VertexConsumerProvider,
        model: ModelInstance,
        pose: PlayerPose,
        skin: RenderBackend.Texture,
        parts: Set<EnumPart> = EnumPart.values().toSet(),
    ) {
        val cosmetic = model.cosmetic

        val renderMetadata = RenderMetadata(
            pose,
            skin,
            0,
            1 / 16f,
            state.sides[cosmetic.id],
            state.hiddenBones[cosmetic.id] ?: emptySet(),
            state.getPositionAdjustment(cosmetic),
            parts - state.hiddenParts.getOrDefault(cosmetic.id, emptySet()),
        )
        model.render(matrixStack, vertexConsumerProvider, state.rootBones.getValue(cosmetic.id), renderMetadata)
    }

    fun collectEvents(consumer: (ModelAnimationState.Event) -> Unit) {
        for (model in models.values) {
            val pendingEvents = model.animationState.pendingEvents
            if (pendingEvents.isNotEmpty()) {
                for (event in pendingEvents) {
                    consumer(event)
                }
                pendingEvents.clear()
            }
        }
    }

    companion object {
        private var atlasCounter = 0
    }
}