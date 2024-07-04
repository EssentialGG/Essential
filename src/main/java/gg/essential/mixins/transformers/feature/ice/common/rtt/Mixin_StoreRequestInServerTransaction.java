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

import gg.essential.mixins.impl.feature.ice.common.rtt.StunServerTransactionExt;
import org.ice4j.StunMessageEvent;
import org.ice4j.message.Request;
import org.ice4j.stack.StunServerTransaction;
import org.ice4j.stack.StunStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = StunStack.class, remap = false)
public abstract class Mixin_StoreRequestInServerTransaction {

    @ModifyVariable(method = "handleMessageEvent", at = @At(value = "INVOKE", target = "Lorg/ice4j/stack/StunServerTransaction;retransmitResponse()V"))
    private StunServerTransaction storeOnRetransmit(StunServerTransaction transaction, StunMessageEvent event) {
        ((StunServerTransactionExt) transaction).setRequest((Request) event.getMessage());
        return transaction;
    }

    @ModifyVariable(method = "handleMessageEvent", at = @At(value = "INVOKE", target = "Lorg/ice4j/stack/StunServerTransaction;start()V"))
    private StunServerTransaction storeOnCreate(StunServerTransaction transaction, StunMessageEvent event) {
        ((StunServerTransactionExt) transaction).setRequest((Request) event.getMessage());
        return transaction;
    }

}
