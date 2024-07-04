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

import net.minecraft.client.renderer.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.Buffer;
import java.nio.IntBuffer;

/**
 * Minecraft temporarily changes the position and limit of the int buffer view during sorting but forgot to reset it at
 * the end of the method. This can result in an inappropriate resize (because it thinks it hit the end, but it only hit
 * the limit), followed by a crash due to buffer overflow (cause the buffer ends up being shrunk, not grown).
 *
 * Fixed in Vanilla as of 1.12.2 (or a version in between).
 */
@Mixin(WorldRenderer.class)
public abstract class Mixin_FixBufferBuilderSortReset {
    @Shadow
    private IntBuffer rawIntBuffer;

    @Shadow
    protected abstract int getBufferSize();

    @Inject(method = "sortVertexData", at = @At("RETURN"))
    private void resetIntBuffer(CallbackInfo ci) {
        // FIXME using Buffer (not IntBuffer) is required so we do not compile against the methods from the Java 16 JDK
        //       we should switch gradle to use the --release flag where necessary so this does not need manual handling
        //       (not trivial to do cause FG screws with the legacy flags and you must not set both)
        Buffer buffer = this.rawIntBuffer;
        buffer.limit(buffer.capacity());
        buffer.position(this.getBufferSize());
    }
}
