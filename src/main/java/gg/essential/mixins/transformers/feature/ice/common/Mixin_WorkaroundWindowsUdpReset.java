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
package gg.essential.mixins.transformers.feature.ice.common;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Windows sometimes just "closes" UDP connections, as if that's even a thing which can happen.
 * The common way to deal with those seems to be to just ignore them: https://stackoverflow.com/a/45050034
 */
@Mixin(targets = "org.ice4j.stack.Connector", remap = false)
public abstract class Mixin_WorkaroundWindowsUdpReset {

    private boolean ignoreNextException;

    @Shadow
    @Final
    private static Logger logger;

    @ModifyVariable(method = "run", at = @At(value = "FIELD", target = "Lorg/ice4j/stack/Connector;running:Z", ordinal = 3))
    private SocketException inspectException(SocketException e) {
        if (e.getMessage().startsWith("Network dropped connection on reset")) {
            logger.log(Level.INFO, "Ignoring nonsensical exception:", e);
            ignoreNextException = true;
        }
        return e;
    }

    @ModifyExpressionValue(method = "run", at = @At(value = "FIELD", target = "Lorg/ice4j/stack/Connector;running:Z", ordinal = 3))
    private boolean runningAndNotIgnored(boolean running) {
        if (ignoreNextException) {
            ignoreNextException = false;
            return false;
        }
        return running;
    }
}
