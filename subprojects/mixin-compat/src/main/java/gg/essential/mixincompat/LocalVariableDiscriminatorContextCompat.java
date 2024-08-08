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
package gg.essential.mixincompat;

import gg.essential.CompatMixin;
import gg.essential.CompatShadow;
import gg.essential.mixincompat.util.MixinCompatUtils;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;

@CompatMixin(LocalVariableDiscriminator.Context.class)
public class LocalVariableDiscriminatorContextCompat {
    @CompatShadow
    InjectionInfo info;

    @CompatShadow(original = "initLocals")
    private LocalVariableDiscriminator.Context.Local[] initLocals$original(Target target, boolean argsOnly, AbstractInsnNode node) { throw new LinkageError(); }

    private LocalVariableDiscriminator.Context.Local[] initLocals(Target target, boolean argsOnly, AbstractInsnNode node) {
        return MixinCompatUtils.withCurrentMixinInfo(this.info.getMixin().getMixin(), () -> initLocals$original(target, argsOnly, node));
    }
}
