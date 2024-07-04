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
package gg.essential.network.connectionmanager.cosmetics

import gg.essential.mod.Model
import gg.essential.model.AnimatedCape
import gg.essential.model.BedrockModel
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.util.UIdentifier
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class ModelLoader(
    private val assetLoader: AssetLoader,
) {
    private val models: MutableMap<String, ModelState> = ConcurrentHashMap()
    private val capes: MutableMap<String, CapeState> = ConcurrentHashMap()

    private fun getModelState(cosmetic: Cosmetic, variant: String, skinType: Model, priority: AssetLoader.Priority): ModelState {
        val key = "${cosmetic.id}-$variant-$skinType"
        val state = models.compute(key) { _, state ->
            state?.takeUnless { it.cosmetic != cosmetic } ?: ModelState(cosmetic, variant, skinType)
        }!!
        state.ensurePriorityAtLeast(priority)
        return state
    }

    fun getModel(cosmetic: Cosmetic, variant: String, skinType: Model, priority: AssetLoader.Priority): CompletableFuture<BedrockModel> =
        getModelState(cosmetic, variant, skinType, priority).future

    fun getModel(cosmetic: Cosmetic, variant: String, priority: AssetLoader.Priority): CompletableFuture<BedrockModel> =
        getModel(cosmetic, variant, Model.STEVE, priority)

    fun getAssets(cosmetic: Cosmetic, variant: String, skinType: Model, priority: AssetLoader.Priority): List<AssetLoader.Asset<*>> =
        getModelState(cosmetic, variant, skinType, priority).dependencies

    fun getCape(cosmetic: Cosmetic, variant: String, priority: AssetLoader.Priority): CompletableFuture<List<UIdentifier>> {
        val key = "${cosmetic.id}-$variant"
        val state = capes.compute(key) { _, state ->
            state?.takeUnless { it.cosmetic != cosmetic } ?: CapeState(cosmetic, variant)
        }!!
        state.ensurePriorityAtLeast(priority)
        return state.future
    }

    private inner class ModelState(val cosmetic: Cosmetic, variant: String, skinType: Model) {
        private val assets = cosmetic.assets(variant)

        private val modelAsset =
            assets.geometry.let { (steve, alex) -> (if (skinType == Model.ALEX) alex else null) ?: steve }
        private val skinMaskAssets = (assets.sidedSkinMasks + (null to assets.defaultSkinMask)).mapNotNull { (side, masks) ->
            val (steve, alex) = masks
            val mask = (if (skinType == Model.ALEX) alex else null) ?: steve ?: return@mapNotNull null
            side to mask
        }.toMap()

        private val model = assetLoader.getAsset(modelAsset, AssetLoader.Priority.Passive, AssetLoader.AssetType.Model)
        private val animation = assets.animations?.let { assetLoader.getAsset(it, AssetLoader.Priority.Passive, AssetLoader.AssetType.Animation) }
        private val particles = assets.particles.mapValues { assetLoader.getAsset(it.value, AssetLoader.Priority.Passive, AssetLoader.AssetType.Particle) }
        private val soundDefinitions = assets.soundDefinitions?.let { assetLoader.getAsset(it, AssetLoader.Priority.Passive, AssetLoader.AssetType.SoundDefinitions) }
        private val texture = assets.texture?.let { assetLoader.getAsset(it, AssetLoader.Priority.Passive, AssetLoader.AssetType.Texture) }
        private val skinMasks = skinMaskAssets.mapValues { assetLoader.getAsset(it.value, AssetLoader.Priority.Passive, AssetLoader.AssetType.Mask) }
        val dependencies = listOfNotNull(model, animation, soundDefinitions, texture) + skinMasks.values + particles.values

        val future: CompletableFuture<BedrockModel> =
            CompletableFuture.allOf(*dependencies.map { it.parsed }.toTypedArray()).handleAsync { _, _ ->
                BedrockModel(
                    cosmetic,
                    variant,
                    model.parsed.join(),
                    animation?.parsed?.join(),
                    particles.mapValues { it.value.parsed.join() },
                    soundDefinitions?.parsed?.join(),
                    texture?.parsed?.join(),
                    skinMasks.mapValues { it.value.parsed.join() },
                )
            }.whenComplete { _, throwable ->
                if (throwable != null) {
                    LOGGER.error("Failed to load cosmetic ${cosmetic.id}: ", (throwable as? CompletionException)?.cause ?: throwable)
                }
            }

        private val priority = AtomicReference(AssetLoader.Priority.Passive)

        fun ensurePriorityAtLeast(atLeast: AssetLoader.Priority) {
            if (priority.getAndUpdate { if (it < atLeast) atLeast else it } < atLeast) {
                dependencies.forEach { dependency ->
                    assetLoader.getAssetBytes(dependency.info, atLeast)
                }
            }
        }
    }

    private inner class CapeState(val cosmetic: Cosmetic, val variant: String) {
        private val textureAsset = cosmetic.assets(variant).texture!!
        private val textureFuture = assetLoader.getAssetBytes(textureAsset, AssetLoader.Priority.Passive)

        val future: CompletableFuture<List<UIdentifier>> =
            textureFuture.thenCompose { textureBytes ->
                AnimatedCape.createFrames(cosmetic, textureAsset, textureBytes)
            }.whenComplete { _, throwable ->
                if (throwable != null) {
                    LOGGER.error("Failed to load cape ${cosmetic.id}: ", (throwable as? CompletionException)?.cause ?: throwable)
                }
            }

        private val priority = AtomicReference(AssetLoader.Priority.Passive)

        fun ensurePriorityAtLeast(atLeast: AssetLoader.Priority) {
            if (priority.getAndUpdate { if (it < atLeast) atLeast else it } < atLeast) {
                assetLoader.getAssetBytes(textureAsset, atLeast)
            }
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ModelLoader::class.java)
    }
}
