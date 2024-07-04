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
package gg.essential.asm.compat;

//#if MC<11400
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Iterator;

public class PhosphorTransformer implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if ("me.jellysquid.mods.phosphor.mixins.lighting.common.MixinChunk$Vanilla".equals(transformedName)) {
            ClassReader classReader = new ClassReader(basicClass);
            ClassNode classNode = new ClassNode();
            classReader.accept(classNode, 0);
            for (MethodNode method : classNode.methods) {
                if (method.visibleAnnotations == null) continue;
                for (AnnotationNode annotation : method.visibleAnnotations) {
                    if (annotation.desc.endsWith("ModifyVariable;")) {
                        for (Iterator<Object> it = annotation.values.iterator(); it.hasNext(); ) {
                            Object value = it.next();
                            // Mixin 0.8.2 didn't respect these Slices anyway. 0.8.4 does, and that breaks Phosphor.
                            if ("slice".equals(value)) {
                                it.remove();
                                if (it.hasNext()) {
                                    it.next();
                                    it.remove();
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            ClassWriter writer = new ClassWriter(0);
            classNode.accept(writer);
            return writer.toByteArray();
        }
        return basicClass;
    }
}
//#endif