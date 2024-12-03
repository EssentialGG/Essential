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
package gg.essential.mixins.transformers.feature.particles;

import gg.essential.mixins.ext.client.ParticleSystemHolder;
import gg.essential.model.ParticleSystem;
import gg.essential.model.backend.minecraft.MinecraftRenderBackend;
import gg.essential.model.util.Quaternion;
import gg.essential.model.util.UMatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

import static dev.folomeev.kotgl.matrix.matrices.Matrices.identityMat3;
import static dev.folomeev.kotgl.matrix.matrices.Matrices.identityMat4;
import static dev.folomeev.kotgl.matrix.vectors.Vectors.vec3;
import static dev.folomeev.kotgl.matrix.vectors.Vectors.vecUnitY;
import static dev.folomeev.kotgl.matrix.vectors.mutables.MutableVectors.times;
import static gg.essential.util.HelpersKt.getPerspective;

//#if MC>=12104
//$$ import com.mojang.blaze3d.systems.RenderSystem;
//#endif

//#if MC>=11600
//$$ import com.mojang.blaze3d.matrix.MatrixStack;
//$$ import net.minecraft.client.renderer.IRenderTypeBuffer;
//$$ import net.minecraft.client.renderer.LightTexture;
//$$ import net.minecraft.client.world.ClientWorld;
//#else
import net.minecraft.client.particle.Particle;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
//#endif

@Mixin(ParticleManager.class)
public abstract class Mixin_RenderParticleSystemOfClientWorld {
    // Forge overloads this method with an additional argument in 1.16+
    //#if FORGE && MC>=11600
    //#if MC>=11700
    //$$ private static final String RENDER_PARTICLES = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V";
    //#else
    //$$ private static final String RENDER_PARTICLES = "renderParticles(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer$Impl;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/renderer/ActiveRenderInfo;FLnet/minecraft/client/renderer/culling/ClippingHelper;)V";
    //#endif
    //#else
    private static final String RENDER_PARTICLES = "renderParticles";
    //#endif

    @Shadow
    //#if MC>=11600
    //$$ protected ClientWorld world;
    //#else
    protected World world;
    //#endif

    @Group(name = "render_particles", min = 1, max = 1)
    @Inject(
        method = {
            RENDER_PARTICLES,
            //#if MC>=12004 && FORGE
            //$$ // OptiFine 1.20.4 uses the pre-1.17 method name
            //$$ "renderParticles",
            //#endif
        },
        //#if MC>=12104
        //$$ at = @At("RETURN")
        //#elseif MC>=12005
        //$$ at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;depthMask(Z)V")
        //#elseif MC>=11700
        //$$ at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V")
        //#elseif MC>=11600
        //$$ at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;popMatrix()V")
        //#else
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;depthMask(Z)V", ordinal = 2)
        //#endif
    )
    private void essential$renderParticles(
        //#if MC>=11600
        //#if MC<12005
        //$$ MatrixStack matrixStackIn,
        //$$ IRenderTypeBuffer.Impl bufferIn,
        //#endif
        //#if MC<12104
        //$$ LightTexture lightTextureIn,
        //#endif
        //$$ ActiveRenderInfo activeRenderInfoIn,
        //$$ float partialTicks,
        //#if MC>=12104
        //$$ VertexConsumerProvider.Immediate bufferIn,
        //#endif
        //#if FORGE
        //#if MC>=11700
        //$$ net.minecraft.client.renderer.culling.Frustum frustum,
        //#else
        //$$ net.minecraft.client.renderer.culling.ClippingHelper clippingHelper,
        //#endif
        //#endif
        //#else
        Entity cameraEntity,
        float partialTicks,
        //#endif
        CallbackInfo ci
    ) {
        if (!(this.world instanceof ParticleSystemHolder)) {
            return;
        }
        ParticleSystem particleSystem = ((ParticleSystemHolder) this.world).getParticleSystem();

        if (particleSystem.isEmpty()) {
            return;
        }

        //#if MC>=12102
        //$$ net.minecraft.util.profiler.Profiler profiler = net.minecraft.util.profiler.Profilers.get();
        //#elseif MC>=11600
        //$$ net.minecraft.profiler.IProfiler profiler = this.world.getProfiler();
        //#else
        net.minecraft.profiler.Profiler profiler = this.world.profiler;
        //#endif

        profiler.startSection("essentialUpdate");

        particleSystem.update();

        if (!particleSystem.hasAnythingToRender()) {
            profiler.endSection();
            return;
        }

        profiler.endStartSection("essentialRender");

        UMatrixStack stack = new UMatrixStack(identityMat4(), identityMat3());

        //#if MC>=11600
        //$$ Vector3d cameraPosMc = activeRenderInfoIn.getProjectedView();
        //#else
        Vec3d cameraPosMc = ActiveRenderInfo.projectViewFromEntity(cameraEntity, partialTicks);
        //#endif
        dev.folomeev.kotgl.matrix.vectors.Vec3 cameraPos =
            vec3((float) cameraPosMc.x, (float) cameraPosMc.y, (float) cameraPosMc.z);

        //#if MC>=11600
        //$$ net.minecraft.util.math.vector.Quaternion cameraRotMc = activeRenderInfoIn.getRotation();
        //#if MC>=12100
        //$$ Quaternion cameraRot = new Quaternion(cameraRotMc.x, cameraRotMc.y, cameraRotMc.z, cameraRotMc.w);
        //#elseif MC>=11903
        //$$ Quaternion cameraRot = new Quaternion(cameraRotMc.x, cameraRotMc.y, cameraRotMc.z, cameraRotMc.w).opposite();
        //#else
        //$$ Quaternion cameraRot = new Quaternion(cameraRotMc.getX(), cameraRotMc.getY(), cameraRotMc.getZ(), cameraRotMc.getW()).opposite();
        //#endif
        //#else
        // Intentionally not using Particle.cameraViewDir because that doesn't account for third-person (vanilla bug,
        // though not apparent because they have backface culling disabled) and if another mod fixes that bug, then that
        // would break our code because we'd then be inverting twice.
        // This does however mean that we do break if a mod adds a freely rotating third-person view, but there isn't
        // really any way around that (especially in 1.8 where Particle.cameraViewDir isn't even a thing).
        Vec3d lookAtMc = cameraEntity.getLook(partialTicks);
        dev.folomeev.kotgl.matrix.vectors.Vec3 lookAt = vec3((float) lookAtMc.x, (float) lookAtMc.y, (float) lookAtMc.z);
        if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 2) {
            lookAt = times(lookAt, -1);
        }
        Quaternion cameraRot = Quaternion.Companion.fromLookAt(lookAt, vecUnitY());
        //#endif

        //#if MC>=11600
        //$$ stack.translate(-cameraPos.getX(), -cameraPos.getY(), -cameraPos.getZ());
        //#else
        stack.translate((float) -Particle.interpPosX, (float) -Particle.interpPosY, (float) -Particle.interpPosZ);
        //#endif

        //#if MC>=11600
        //$$ UUID cameraUuid = activeRenderInfoIn.getRenderViewEntity().getUniqueID();
        //#else
        UUID cameraUuid = cameraEntity.getUniqueID();
        //#endif

        ParticleSystem.VertexConsumerProvider particleVertexConsumer = new MinecraftRenderBackend.ParticleVertexConsumerProvider(
            //#if MC>=12104
            //$$ bufferIn
            //#endif
        );

        boolean isFirstPerson = getPerspective() == 0;
        particleSystem.render(stack, cameraPos, cameraRot, particleVertexConsumer, cameraUuid, isFirstPerson);

        profiler.endSection();
    }

    // Forge's overload has been mirrored in OptiFine (which can be used on Fabric through OptiFabric)
    //#if MC>=11600 && FABRIC
    //$$ @Group(name = "render_particles", min = 1, max = 1)
    //$$ @org.spongepowered.asm.mixin.Dynamic("Extra argument added by OptiFine (originally by Forge)")
    //$$ @Inject(
        //$$ // These are forge names, we mustn't try to remap them using fabric mappings (luckily the yarn class names are the same across versions)
        //#disable-remap
        //$$ // OptiFine 1.20.4 uses the pre-1.17 method name
        //#if MC>=11700 && MC<12004
        //$$ method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/render/LightmapTextureManager;Lnet/minecraft/client/render/Camera;FLnet/minecraft/client/render/Frustum;)V",
        //#else
        //$$ method = "renderParticles(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/render/LightmapTextureManager;Lnet/minecraft/client/render/Camera;FLnet/minecraft/client/render/Frustum;)V",
        //#endif
        //#enable-remap
        //#if MC>=11700
        //$$ at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V")
        //#else
        //$$ at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;popMatrix()V")
        //#endif
    //$$ )
    //$$ private void essential$renderParticles(
    //$$     MatrixStack matrixStackIn,
    //$$     VertexConsumerProvider.Immediate bufferIn,
    //$$     LightmapTextureManager lightTextureIn,
    //$$     Camera activeRenderInfoIn,
    //$$     float partialTicks,
    //$$     net.minecraft.client.render.Frustum clippingHelper,
    //$$     CallbackInfo ci
    //$$ ) {
    //$$     essential$renderParticles(
                 //#if MC<12005
                 //$$ matrixStackIn,
                 //$$ bufferIn,
                 //#endif
            //#if MC<12104
            //$$ lightTextureIn,
            //#endif
    //$$         activeRenderInfoIn,
    //$$         partialTicks,
            //#if MC>=12104
            //$$ bufferIn,
            //#endif
    //$$         ci
    //$$     );
    //$$ }
    //#endif
}
