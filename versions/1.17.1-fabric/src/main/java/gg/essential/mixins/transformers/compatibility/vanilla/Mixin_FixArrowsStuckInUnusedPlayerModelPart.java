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
package gg.essential.mixins.transformers.compatibility.vanilla;

import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.model.ModelPart;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

/**
 * See 1.12.2 file.
 * The overwrite issue has now been fixed because MC overwrites the nodes in the model definition before parsing it.
 * The cape issue still applies.
 * The ears issue is still dealt with elsewhere.
 */
@Mixin(PlayerEntityModel.class)
public abstract class Mixin_FixArrowsStuckInUnusedPlayerModelPart<T extends LivingEntity> extends BipedEntityModel<T> {

    @Shadow
    @Final
    private ModelPart cloak;

    @ModifyArg(method = "<init>", at = @At(value = "ESSENTIAL:INVOKE_IN_INIT", target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;"))
    private Predicate<ModelPart> removeCapeFromPartsList(Predicate<ModelPart> predicate) {
        return predicate.and(modelPart -> modelPart != this.cloak);
    }

    Mixin_FixArrowsStuckInUnusedPlayerModelPart() { super(null); }
}
