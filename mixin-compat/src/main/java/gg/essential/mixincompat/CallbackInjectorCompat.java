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
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.callback.CallbackInjector;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Locals;

import java.util.Locale;

@CompatMixin(CallbackInjector.class)
public abstract class CallbackInjectorCompat extends Injector {
    public CallbackInjectorCompat(InjectionInfo info, String annotationType) {
        super(info, annotationType);
    }

    @CompatShadow
    private LocalCapture localCapture;

    @CompatShadow(original = "preInject")
    protected void preInject$old(Target target, InjectionNodes.InjectionNode node) { throw new LinkageError(); }

    @CompatShadow(original = "inject")
    protected void inject$old(Target target, InjectionNodes.InjectionNode node) { throw new LinkageError(); }

    @CompatShadow
    private void inject(final CallbackBridge callback) { throw new LinkageError(); }

    @Override
    protected void preInject(Target target, InjectionNodes.InjectionNode node) {
        MixinCompatUtils.withCurrentMixinInfo(this.info.getMixin().getMixin(), () -> {
            if ((isCaptureLocals(this.localCapture) || isPrintLocals(this.localCapture)) && !node.hasDecoration(getDecorationKey())) {
                LocalVariableNode[] locals = Locals.getLocalsAt(this.classNode, target.method, node.getCurrentTarget());

                for (int j = 0; j < locals.length; ++j) {
                    if (locals[j] != null && locals[j].desc != null && locals[j].desc.startsWith("Lorg/spongepowered/asm/mixin/injection/callback/")) {
                        locals[j] = null;
                    }
                }

                node.decorate(getDecorationKey(), locals);
            }
        });
    }

    @Override
    protected void inject(Target target, InjectionNodes.InjectionNode node) {
        MixinCompatUtils.withCurrentMixinInfo(this.info.getMixin().getMixin(), () -> {
            LocalVariableNode[] locals = node.getDecoration(getDecorationKey());
            this.inject(new CallbackBridge(this.methodNode, target, node, locals, isCaptureLocals(this.localCapture)));
        });
    }

    private String getDecorationKey() {
        return String.format(Locale.ROOT, "locals(useNewAlgorithm=%s)", MixinCompatUtils.canUseNewLocalsAlgorithm());
    }

    private boolean isCaptureLocals(LocalCapture localCapture) {
        return localCapture == LocalCapture.CAPTURE_FAILHARD || localCapture == LocalCapture.CAPTURE_FAILSOFT || localCapture == LocalCapture.CAPTURE_FAILEXCEPTION;
    }

    private boolean isPrintLocals(LocalCapture localCapture) {
        return localCapture == LocalCapture.PRINT;
    }

    // Bridge class. Will not affect the target but can be used by the outer CompatMixin and will be replaced by the real one during application.
    @SuppressWarnings("InnerClassMayBeStatic")
    @CompatMixin(target = "org.spongepowered.asm.mixin.injection.callback.CallbackInjector$Callback")
    private class CallbackBridge {
        CallbackBridge(MethodNode handler, Target target, InjectionNodes.InjectionNode node, LocalVariableNode[] locals, boolean captureLocals) {
        }
    }
}
