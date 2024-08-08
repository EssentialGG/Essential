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

import gg.essential.lib.guava21.primitives.Bytes;
import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Converts any usages of the old (relocated) mixin asm classes (as used by Mixin 0.7) to the original (non-relocated)
 * class names (as used by Mixin 0.8). This vastly improves compatibility of 0.8 with mods compiled against 0.7.
 */
public class BundledAsmTransformer implements IClassTransformer {
    private static final Logger LOGGER = LogManager.getLogger(BundledAsmTransformer.class);

    private static final String originalPackage = "org/objectweb/asm/";
    private static final String legacyPackage = "org/spongepowered/asm/lib/";

    private static final byte[] legacyPackageBytes = legacyPackage.getBytes(StandardCharsets.UTF_8);

    private static final Remapper remapper = new Remapper() {
        @Override
        public String map(String typeName) {
            if (typeName.startsWith(legacyPackage)) {
                return originalPackage + typeName.substring(legacyPackage.length());
            } else {
                return typeName;
            }
        }
    };

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || Bytes.indexOf(bytes, legacyPackageBytes) == -1) {
            return bytes;
        }

        LOGGER.debug("Found reference to legacy mixin asm in \"{}\", remapping to upstream package..", name);

        ClassReader classReader = new ClassReader(bytes);
        ClassWriter classWriter = new ClassWriter(0);
        try {
            classReader.accept(new ClassRemapper(new DuplicateDetectingClassWriter(classWriter), remapper), 0);
            return classWriter.toByteArray();
        } catch (DuplicateMethodException e) {
            LOGGER.debug("Aborting transformation of \"" + name + '"', e);
            return bytes;
        }
    }

    // Some mods have Mixin plugins with methods for both renamed and not renamed ASM. We want to avoid causing crashes by transforming them.
    private static class DuplicateDetectingClassWriter extends ClassVisitor {
        private final Set<String> detectedMethods = new HashSet<>();

        public DuplicateDetectingClassWriter(ClassWriter writer) {
            super(Opcodes.ASM5);
            this.cv = writer;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            String identifier = name + desc;
            if (detectedMethods.contains(identifier)) {
                throw new DuplicateMethodException(identifier);
            }
            detectedMethods.add(identifier);

            return super.visitMethod(access, name, desc, signature, exceptions);
        }
    }

    private static class DuplicateMethodException extends RuntimeException {
        public DuplicateMethodException(String message) {
            super(message);
        }
    }
}
