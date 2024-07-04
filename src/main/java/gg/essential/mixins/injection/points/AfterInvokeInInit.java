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
package gg.essential.mixins.injection.points;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.spongepowered.asm.mixin.injection.IInjectionPointContext;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.points.BeforeInvoke;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;

import java.util.Collection;

/**
 * Just like INVOKE+SHIFT(AFTER) but allowed in the constructor.
 * Safe only at injection points after the super call. Fabric supports this by default but upstream Mixin does not.
 */
// Need to use AfterInvoke instead of Invoke+Shift because Shift does not properly delegate getTargetRestriction
@InjectionPoint.AtCode("AFTER_INVOKE_IN_INIT")
public class AfterInvokeInInit extends BeforeInvoke {
    public AfterInvokeInInit(InjectionPointData data) {
        super(data);
    }

    @Override
    public RestrictTargetLevel getTargetRestriction(IInjectionPointContext context) {
        return RestrictTargetLevel.CONSTRUCTORS_AFTER_DELEGATE;
    }

    @Override
    protected boolean addInsn(InsnList insns, Collection<AbstractInsnNode> nodes, AbstractInsnNode insn) {
        nodes.add(InjectionPoint.nextNode(insns, insn));
        return true;
    }

    // Need to explicitly overwrite this because some mixin versions check for its existence to determine compatibility
    @Override
    public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
        return super.find(desc, insns, nodes);
    }
}
