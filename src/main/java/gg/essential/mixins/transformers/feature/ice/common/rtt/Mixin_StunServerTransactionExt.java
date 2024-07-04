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
import org.ice4j.message.Request;
import org.ice4j.stack.StunServerTransaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = StunServerTransaction.class, remap = false)
public abstract class Mixin_StunServerTransactionExt implements StunServerTransactionExt {

    @Unique
    private Request request;

    @Override
    public Request getRequest() {
        return this.request;
    }

    @Override
    public void setRequest(Request request) {
        this.request = request;
    }
}
