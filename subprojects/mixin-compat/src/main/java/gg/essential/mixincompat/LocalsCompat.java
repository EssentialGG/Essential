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
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.util.Locals;
import org.spongepowered.asm.util.throwables.LVTGeneratorError;

import java.util.Iterator;
import java.util.List;

@CompatMixin(Locals.class)
public class LocalsCompat {
    @CompatShadow
    private static AbstractInsnNode nextNode(InsnList insns, AbstractInsnNode insn) { throw new LinkageError(); }

    @CompatShadow
    private static int getAdjustedFrameSize(int currentSize, int type, int size, int initialFrameSize) { throw new LinkageError(); }

    @CompatShadow(original = "getLocalsAt")
    public static LocalVariableNode[] getLocalsAt_0_8_4(ClassNode classNode, MethodNode method, AbstractInsnNode node) { throw new LinkageError(); }

    public static LocalVariableNode[] getLocalsAt(ClassNode classNode, MethodNode method, AbstractInsnNode node) {
        if (MixinCompatUtils.canUseNewLocalsAlgorithm()) {
            return LocalsCompat.getLocalsAt_0_8_4(classNode, method, node);
        }
        return LocalsCompat.getLocalsAt_0_8_2(classNode, method, node);
    }

    // The old algorithm used on mods which were built against versions 0.8.3 and below.
    private static LocalVariableNode[] getLocalsAt_0_8_2(ClassNode classNode, MethodNode method, AbstractInsnNode node) {
        for (int i = 0; i < 3 && (node instanceof LabelNode || node instanceof LineNumberNode); i++) {
            node = LocalsCompat.nextNode(method.instructions, node);
        }

        ClassInfo classInfo = ClassInfo.forName(classNode.name);
        if (classInfo == null) {
            throw new LVTGeneratorError("Could not load class metadata for " + classNode.name + " generating LVT for " + method.name);
        }
        ClassInfo.Method methodInfo = classInfo.findMethod(method, method.access | ClassInfo.INCLUDE_INITIALISERS);
        if (methodInfo == null) {
            throw new LVTGeneratorError("Could not locate method metadata for " + method.name + " generating LVT in " + classNode.name);
        }
        List<ClassInfo.FrameData> frames = methodInfo.getFrames();

        LocalVariableNode[] frame = new LocalVariableNode[method.maxLocals];
        int local = 0, index = 0;

        // Initialise implicit "this" reference in non-static methods
        if ((method.access & Opcodes.ACC_STATIC) == 0) {
            frame[local++] = new LocalVariableNode("this", Type.getObjectType(classNode.name).toString(), null, null, null, 0);
        }

        // Initialise method arguments
        for (Type argType : Type.getArgumentTypes(method.desc)) {
            frame[local] = new LocalVariableNode("arg" + index++, argType.toString(), null, null, null, local);
            local += argType.getSize();
        }

        int initialFrameSize = local;
        int frameSize = local;
        int frameIndex = -1;
        int lastFrameSize = local;
        VarInsnNode storeInsn = null;

        for (Iterator<AbstractInsnNode> iter = method.instructions.iterator(); iter.hasNext();) {
            AbstractInsnNode insn = iter.next();
            if (storeInsn != null) {
                frame[storeInsn.var] = Locals.getLocalVariableAt(classNode, method, insn, storeInsn.var);
                storeInsn = null;
            }

            handleFrame: if (insn instanceof FrameNode) {
                frameIndex++;
                FrameNode frameNode = (FrameNode)insn;
                if (frameNode.type == Opcodes.F_SAME || frameNode.type == Opcodes.F_SAME1) {
                    break handleFrame;
                }

                ClassInfo.FrameData frameData = frameIndex < frames.size() ? frames.get(frameIndex) : null;

                if (frameData != null) {
                    if (frameData.type == Opcodes.F_FULL) {
                        frameSize = Math.min(frameSize, frameData.locals);
                        lastFrameSize = frameSize;
                    } else {
                        frameSize = LocalsCompat.getAdjustedFrameSize(frameSize, frameData);
                    }
                } else {
                    frameSize = LocalsCompat.getAdjustedFrameSize(frameSize, frameNode);
                }

                if (frameNode.type == Opcodes.F_CHOP) {
                    for (int framePos = frameSize; framePos < frame.length; framePos++) {
                        frame[framePos] = null;
                    }
                    lastFrameSize = frameSize;
                    break handleFrame;
                }

                int framePos = frameNode.type == Opcodes.F_APPEND ? lastFrameSize : 0;
                lastFrameSize = frameSize;

                // localPos tracks the location in the frame node's locals list, which doesn't leave space for TOP entries
                for (int localPos = 0; framePos < frame.length; framePos++, localPos++) {
                    // Get the local at the current position in the FrameNode's locals list
                    final Object localType = (localPos < frameNode.local.size()) ? frameNode.local.get(localPos) : null;

                    if (localType instanceof String) { // String refers to a reference type
                        frame[framePos] = Locals.getLocalVariableAt(classNode, method, insn, framePos);
                    } else if (localType instanceof Integer) { // Integer refers to a primitive type or other marker
                        boolean isMarkerType = localType == Opcodes.UNINITIALIZED_THIS || localType == Opcodes.NULL;
                        boolean is32bitValue = localType == Opcodes.INTEGER || localType == Opcodes.FLOAT;
                        boolean is64bitValue = localType == Opcodes.DOUBLE || localType == Opcodes.LONG;
                        if (localType == Opcodes.TOP) {
                            // Do nothing, explicit TOP entries are pretty much always bogus, and real ones are handled below
                        } else if (isMarkerType) {
                            frame[framePos] = null;
                        } else if (is32bitValue || is64bitValue) {
                            frame[framePos] = Locals.getLocalVariableAt(classNode, method, insn, framePos);

                            if (is64bitValue) {
                                framePos++;
                                frame[framePos] = null; // TOP
                            }
                        } else {
                            throw new LVTGeneratorError("Unrecognised locals opcode " + localType + " in locals array at position " + localPos
                                    + " in " + classNode.name + "." + method.name + method.desc);
                        }
                    } else if (localType == null) {
                        if (framePos >= initialFrameSize && framePos >= frameSize && frameSize > 0) {
                            frame[framePos] = null;
                        }
                    } else if (localType instanceof LabelNode) {
                        // Uninitialised
                    } else {
                        throw new LVTGeneratorError("Invalid value " + localType + " in locals array at position " + localPos
                                + " in " + classNode.name + "." + method.name + method.desc);
                    }
                }
            } else if (insn instanceof VarInsnNode) {
                VarInsnNode varNode = (VarInsnNode) insn;
                boolean isLoad = insn.getOpcode() >= Opcodes.ILOAD && insn.getOpcode() <= Opcodes.SALOAD;
                if (isLoad) {
                    frame[varNode.var] = Locals.getLocalVariableAt(classNode, method, insn, varNode.var);
                } else {
                    // Update the LVT for the opcode AFTER this one, since we always want to know
                    // the frame state BEFORE the *current* instruction to match the contract of
                    // injection points
                    storeInsn = varNode;
                }
            }

            if (insn == node) {
                break;
            }
        }

        // Null out any "unknown" locals
        for (int l = 0; l < frame.length; l++) {
            if (frame[l] != null && frame[l].desc == null) {
                frame[l] = null;
            }
        }

        return frame;
    }

    // No longer present, copied from 0.8.2 with [initialFrameSize] as 0 to preserve logic.
    private static int getAdjustedFrameSize(int currentSize, FrameNode frameNode) {
        return LocalsCompat.getAdjustedFrameSize(currentSize, frameNode.type, Locals.computeFrameSize(frameNode, 0), 0);
    }

    // No longer present, copied from 0.8.2 with [initialFrameSize] as 0 to preserve logic.
    private static int getAdjustedFrameSize(int currentSize, ClassInfo.FrameData frameData) {
        return LocalsCompat.getAdjustedFrameSize(currentSize, frameData.type, frameData.size, 0);
    }
}
