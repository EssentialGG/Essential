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
package gg.essential.mixins.transformers.util.math;

import dev.folomeev.kotgl.matrix.matrices.Mat3;
import gg.essential.mixins.impl.util.math.Matrix3fExt;
import net.minecraft.util.math.vector.Matrix3f;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static dev.folomeev.kotgl.matrix.matrices.Matrices.mat3;

@Mixin(Matrix3f.class)
public abstract class Mixin_Matrix3fExt implements Matrix3fExt {

    @Shadow
    protected float m00, m01, m02, m10, m11, m12, m20, m21, m22;

    @NotNull
    @Override
    public Mat3 getKotgl() {
        return mat3(
            this.m00,
            this.m01,
            this.m02,
            this.m10,
            this.m11,
            this.m12,
            this.m20,
            this.m21,
            this.m22
        );
    }

    @Override
    public void setKotgl(@NotNull Mat3 m) {
        this.m00 = m.getM00();
        this.m01 = m.getM01();
        this.m02 = m.getM02();
        this.m10 = m.getM10();
        this.m11 = m.getM11();
        this.m12 = m.getM12();
        this.m20 = m.getM20();
        this.m21 = m.getM21();
        this.m22 = m.getM22();
    }
}
