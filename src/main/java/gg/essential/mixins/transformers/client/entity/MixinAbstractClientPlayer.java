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
package gg.essential.mixins.transformers.client.entity;

import com.mojang.authlib.properties.Property;
import gg.essential.Essential;
import gg.essential.api.utils.JsonHolder;
import gg.essential.config.EssentialConfig;
import gg.essential.connectionmanager.common.packet.cosmetic.ClientCosmeticAnimationTriggerPacket;
import gg.essential.cosmetics.*;
import gg.essential.cosmetics.events.AnimationTarget;
import gg.essential.cosmetics.skinmask.MaskedSkinProvider;
import gg.essential.cosmetics.source.CosmeticsSource;
import gg.essential.cosmetics.source.LiveCosmeticsSource;
import gg.essential.gui.common.EmulatedUI3DPlayer;
import gg.essential.handlers.GameProfileManager;
import gg.essential.mod.cosmetics.CosmeticSlot;
import gg.essential.model.BedrockModel;
import gg.essential.model.PlayerMolangQuery;
import gg.essential.model.backend.minecraft.MinecraftRenderBackend;
import gg.essential.model.util.PlayerPoseManager;
import gg.essential.network.connectionmanager.cosmetics.CosmeticsManager;
import gg.essential.util.UIdentifier;
import gg.essential.util.UUIDUtil;
import kotlin.Pair;
import kotlin.Unit;
import net.minecraft.client.entity.AbstractClientPlayer;
import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

import static gg.essential.network.cosmetics.ConversionsKt.toInfra;
import static gg.essential.util.UIdentifierKt.toMC;

//#if MC>=12002
//$$ import com.llamalad7.mixinextras.injector.ModifyReturnValue;
//$$ import net.minecraft.client.util.SkinTextures;
//#endif

@Mixin(AbstractClientPlayer.class)
public abstract class MixinAbstractClientPlayer implements AbstractClientPlayerExt {

    @Unique
    private final PlayerMolangQuery molangQuery = new PlayerMolangQuery((AbstractClientPlayer) (Object) this);
    @Unique
    private CosmeticsSource cosmeticsSource;
    @Unique
    private WearablesManager wearablesManager;
    @Unique
    private final MaskedSkinProvider maskedSkinProvider = new MaskedSkinProvider();
    @Unique
    private String essentialCosmeticsCape;
    @Unique
    private Pair<List<UIdentifier>, @Nullable List<UIdentifier>> essentialCosmeticsCapeResources;
    @Unique
    private Boolean serverSkinOverrideStatus = null;

    @Unique
    private final boolean[] armorRenderingSuppressed = new boolean[4];

    @Unique
    private final PlayerPoseManager poseManager = new PlayerPoseManager(molangQuery);

    @Unique
    private boolean poseModified;

    @Unique
    private float lastCosmeticsUpdateTime;

    // mixin needs this to be able to correctly identify field initializers
    public MixinAbstractClientPlayer() {
    }

    //#if MC>=12002
    //$$ @Unique
    //$$ private String getModel() {
    //$$     return this.getSkinTextures().model().getName();
    //$$ }
    //$$ @Shadow public abstract SkinTextures getSkinTextures();
    //#else
    @Shadow
    public abstract String getSkinType();
    //#endif

    @Override
    public CosmeticsSource getCosmeticsSource() {
        if (this.cosmeticsSource == null) {
            // Lazily initialized in case a player entity is created before Essential is booted
            CosmeticsManager cosmeticsManager = Essential.getInstance().getConnectionManager().getCosmeticsManager();
            AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
            UUID uuid = self.getUniqueID();
            Collection<Property> properties = self.getGameProfile().getProperties().get("essential:real_uuid");
            for (Property property : properties) {
                String value = property.getValue();
                try {
                    uuid = UUIDUtil.formatWithDashes(value);
                } catch (IllegalArgumentException e) {
                    Essential.logger.warn("Failed to parse fake_player uuid \"" + value + "\" for " + self.getUniqueID(), e);
                }
            }
            this.cosmeticsSource = new LiveCosmeticsSource(cosmeticsManager, uuid);
        }
        return this.cosmeticsSource;
    }

    @Override
    public void setCosmeticsSource(CosmeticsSource cosmeticsSource) {
        this.cosmeticsSource = cosmeticsSource;
    }

    @Override
    public @NotNull WearablesManager getWearablesManager() {
        if (this.wearablesManager == null) {
            AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;

            Set<AnimationTarget> animationTargets;
            if (player instanceof EmulatedUI3DPlayer.EmulatedPlayer) {
                animationTargets = EnumSet.of(AnimationTarget.SELF, AnimationTarget.OTHERS);
            } else if (player instanceof EntityPlayerSP) {
                animationTargets = EnumSet.of(AnimationTarget.SELF);
            } else {
                animationTargets = EnumSet.of(AnimationTarget.OTHERS);
            }

            boolean sendsAnimationPackets =
                player.getUniqueID().equals(UUIDUtil.getClientUUID()) && !(player instanceof EmulatedUI3DPlayer.EmulatedPlayer);

            this.wearablesManager = new WearablesManager(MinecraftRenderBackend.INSTANCE, molangQuery, animationTargets, (cosmetic, event) -> {
                if (sendsAnimationPackets) {
                    CosmeticSlot slot = cosmetic.getType().getSlot();
                    Essential.getInstance().getConnectionManager()
                        .send(new ClientCosmeticAnimationTriggerPacket(toInfra(slot), event));
                }
                return Unit.INSTANCE;
            });
        }
        return this.wearablesManager;
    }

    @Override
    public @NotNull CosmeticsState getCosmeticsState() {
        return this.getWearablesManager().getState();
    }

    @Override
    public void setEssentialCosmeticsCape(@Nullable String cape, @Nullable Pair<List<UIdentifier>, @Nullable List<UIdentifier>> textures) {
        this.essentialCosmeticsCape = cape;
        this.essentialCosmeticsCapeResources = textures;
    }

    @Override
    public ResourceLocation applyEssentialCosmeticsMask(ResourceLocation skin) {
        if (EssentialModelRenderer.cosmeticsShouldRender((AbstractClientPlayer) (Object) this)) {
            ResourceLocation maskedSkin = maskedSkinProvider.provide(skin, getCosmeticsState().getSkinMask());
            if (maskedSkin != null) {
                return maskedSkin;
            }
        }
        return skin;
    }

    @Override
    public boolean isSkinOverrodeByServer() {
        if (serverSkinOverrideStatus == null) {
            String values = GameProfileManager.getSafeTexturesValue(((EntityPlayer) (Object) this).getGameProfile());
            final JsonHolder root = new JsonHolder(new String(Base64.getDecoder().decode(values)));
            serverSkinOverrideStatus = !root.optString("profileId").isEmpty() && !((EntityPlayer) (Object) this).getGameProfile().getId().equals(UUIDUtil.formatWithDashes(root.optString("profileId")));
        }

        return serverSkinOverrideStatus;
    }

    //#if MC>=12002
    //$$ @ModifyReturnValue(method = "getSkinTextures", at = @At("RETURN"))
    //$$ private SkinTextures overrideCapeIfSelectedInEssential(SkinTextures skinTextures) {
    //$$     CallbackInfoReturnable<Identifier> ci = new CallbackInfoReturnable<>("", true, skinTextures.capeTexture());
    //$$     overrideCapeIfSelectedInEssential(ci);
    //$$     if (ci.getReturnValue() != skinTextures.capeTexture()) {
    //$$         skinTextures = new SkinTextures(skinTextures.texture(), skinTextures.textureUrl(), ci.getReturnValue(), skinTextures.elytraTexture(), skinTextures.model(), skinTextures.secure());
    //$$     }
    //$$     return skinTextures;
    //$$ }
    //$$ @Unique
    //#else
    @Inject(method = "getLocationCape", at = @At("HEAD"), cancellable = true)
    //#endif
    private void overrideCapeIfSelectedInEssential(CallbackInfoReturnable<ResourceLocation> ci) {
        if (!EssentialConfig.INSTANCE.getEssentialEnabled()) {
            // Don't override the cape's texture if Essential is disabled.
            return;
        }

        if (!EssentialModelRenderer.cosmeticsShouldRender((AbstractClientPlayer) (Object) this)) {
            return;
        }

        if (essentialCosmeticsCape != null) {
            Pair<List<UIdentifier>, @Nullable List<UIdentifier>> frames = this.essentialCosmeticsCapeResources;

            // Frames will be null if "Cape Disabled" is equipped
            if (frames == null) {
                ci.setReturnValue(null);
                return;
            }

            ci.setReturnValue(toMC(frames.getFirst().get(getCapeAnimationFrame(frames.getFirst().size()))));
        }
    }

    @Override
    public @Nullable UIdentifier getEmissiveCapeTexture() {
        if (!EssentialConfig.INSTANCE.getEssentialEnabled()) {
            return null;
        }

        if (!EssentialModelRenderer.cosmeticsShouldRender((AbstractClientPlayer) (Object) this)) {
            return null;
        }

        if (essentialCosmeticsCape == null) {
            return null;
        }

        Pair<List<UIdentifier>, @Nullable List<UIdentifier>> frames = this.essentialCosmeticsCapeResources;
        if (frames == null) {
            return null;
        }
        List<UIdentifier> emissiveFrames = frames.getSecond();
        if (emissiveFrames == null) {
            return null;
        }

        return emissiveFrames.get(getCapeAnimationFrame(emissiveFrames.size()));
    }

    @Unique
    private int getCapeAnimationFrame(int frameCount) {
        if (frameCount == 1) {
            // fast path, no animation
            return 0;
        } else {
            // slow path, need to compute the current frame index
            float lifetime = new PlayerMolangQuery((AbstractClientPlayer) (Object) this).getLifeTime();
            int frame = (int) (lifetime * BedrockModel.TEXTURE_ANIMATION_FPS);
            return frame % frameCount;
        }
    }

    @Override
    public void assumeArmorRenderingSuppressed() {
        Arrays.fill(armorRenderingSuppressed, true);
    }

    @Override
    public void armorRenderingNotSuppressed(int slot) {
        armorRenderingSuppressed[slot] = false;
    }

    @Override
    public boolean[] wasArmorRenderingSuppressed() {
        return armorRenderingSuppressed;
    }

    @NotNull
    @Override
    public PlayerPoseManager getPoseManager() {
        return poseManager;
    }

    @Override
    public boolean isPoseModified() {
        return poseModified;
    }

    @Override
    public void setPoseModified(boolean poseModified) {
        this.poseModified = poseModified;
    }

    @Override
    public float getLastCosmeticsUpdateTime() {
        return lastCosmeticsUpdateTime;
    }

    @Override
    public void setLastCosmeticsUpdateTime(float lastCosmeticsUpdateTime) {
        this.lastCosmeticsUpdateTime = lastCosmeticsUpdateTime;
    }
}
