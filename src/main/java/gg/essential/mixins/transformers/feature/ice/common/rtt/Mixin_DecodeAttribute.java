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
package gg.essential.mixins.transformers.feature.ice.common.rtt;

import org.ice4j.attribute.Attribute;
import org.ice4j.attribute.AttributeDecoder;
import org.ice4j.attribute.OptionalAttribute;
import org.ice4j.attribute.TransactionTransmitCounterAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = AttributeDecoder.class, remap = false)
public abstract class Mixin_DecodeAttribute {
    @ModifyVariable(method = "decode", at = @At(value = "INVOKE", target = "Lorg/ice4j/attribute/Attribute;setAttributeType(C)V", shift = At.Shift.AFTER))
    private static Attribute decodeTTCAttribute(Attribute attribute) {
        if (attribute instanceof OptionalAttribute) {
            if (attribute.getAttributeType() == TransactionTransmitCounterAttribute.TYPE) {
                return new TransactionTransmitCounterAttribute();
            }
        }
        return attribute;
    }
}
