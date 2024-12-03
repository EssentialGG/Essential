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
package gg.essential.handlers;

import com.mojang.authlib.GameProfile;
import gg.essential.Essential;
import gg.essential.config.EssentialConfig;
import gg.essential.connectionmanager.common.enums.ProfileStatus;
import gg.essential.cosmetics.CosmeticsRenderState;
import gg.essential.data.OnboardingData;
import gg.essential.gui.EssentialPalette;
import gg.essential.mixins.ext.client.network.NetHandlerPlayClientExt;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.profile.ProfileManager;
import gg.essential.render.TextRenderTypeVertexConsumer;
import gg.essential.universal.UGraphics;
import gg.essential.universal.UMatrixStack;
import gg.essential.universal.UMinecraft;
import gg.essential.universal.shader.BlendState;
import gg.essential.util.Diamond;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.ITextComponent;

//#if MC>=11800
//$$ import gg.essential.compat.ImmediatelyFastCompat;
//#endif

//#if MC>=11600
//$$ import net.minecraft.client.renderer.RenderType;
//$$ import net.minecraft.util.ResourceLocation;
//$$ import net.minecraft.client.renderer.IRenderTypeBuffer;
//#endif

import java.awt.*;
import java.util.Map;
import java.util.UUID;

import static gg.essential.elementa.utils.ExtensionsKt.withAlpha;

public class OnlineIndicator {
    //#if MC!=11202
    //$$ public static final ThreadLocal<Boolean> currentlyDrawingEntityName = ThreadLocal.withInitial(() -> false);
    //#else
    /**
     * Set while {@link #currentlyDrawingEntityName()}.
     * Only useful for 1.12.2, all other versions get the entity via regular method arguments.
     */
    public static Entity nametagEntity;
    //#endif

    /**
     * When called from a {@code drawNameplate} mixin, returns whether this nameplate is the primary name nameplate, as
     * opposed to e.g. the scoreboard score line.
     * @return {@code true} if this is the primary name nameplate
     */
    public static boolean currentlyDrawingEntityName() {
        //#if MC!=11202
        //$$ return currentlyDrawingEntityName.get();
        //#else
        return nametagEntity != null;
        //#endif
    }

    public static void drawNametagIndicator(
        UMatrixStack matrixStack,
        //#if MC>=11600
        //$$ IRenderTypeBuffer vertexConsumerProvider,
        //#endif
        CosmeticsRenderState cState,
        String str,
        int light
    ) {

        if (!cState.onlineIndicator()) {
            return;
        }

        boolean alwaysOnTop = !cState.isSneaking();

        int stringWidth = Minecraft.getMinecraft().fontRenderer.getStringWidth(str);
        //#if MC>=12102
        //$$ float vanillaX = (float)(-stringWidth) / 2f;
        //#else
        float vanillaX = -(float)(stringWidth / 2);
        //#endif
        Color color = EssentialPalette.ESSENTIAL_BLUE;
        //#if MC<11600
        UGraphics.enableAlpha();
        UGraphics.disableLighting();
        UGraphics.depthMask(false);
        //#endif
        float x1 = vanillaX - 11;
        float y1 = -1;
        float x2 = vanillaX - 1;
        float y2 = getDiamondBackgroundYMin();

        //#if MC<11600
        if (alwaysOnTop) {
            UGraphics.disableDepth();
        }
        //#endif

        UGraphics.enableBlend();
        UGraphics.tryBlendFuncSeparate(770, 771, 1,0);

        int backgroundOpacity = getTextBackgroundOpacity();
        //#if MC>=12102
        //$$ double z = -0.01;
        //#elseif MC>=11600
        //$$ double z = 0.01;
        //#else
        double z = 0;
        //#endif

        //#if MC>=11600
        //$$ TextRenderTypeVertexConsumer vertexConsumer = TextRenderTypeVertexConsumer.create(vertexConsumerProvider, alwaysOnTop);
        //#else
        UGraphics buffer = UGraphics.getFromTessellator();
        TextRenderTypeVertexConsumer vertexConsumer = TextRenderTypeVertexConsumer.create(buffer);
        //#endif

        vertexConsumer.pos(matrixStack, x1, y1, z).color(0, 0, 0, backgroundOpacity).tex(0, 0).light(light).endVertex();
        vertexConsumer.pos(matrixStack, x1, y2, z).color(0, 0, 0, backgroundOpacity).tex(0, 0).light(light).endVertex();
        vertexConsumer.pos(matrixStack, x2, y2, z).color(0, 0, 0, backgroundOpacity).tex(0, 0).light(light).endVertex();
        vertexConsumer.pos(matrixStack, x2, y1, z).color(0, 0, 0, backgroundOpacity).tex(0, 0).light(light).endVertex();

        float diamondCenter = (y1 + y2) / 2 + getDiamondYOffset();
        Diamond.drawDiamond(matrixStack, vertexConsumer, 6, vanillaX - 6, diamondCenter, withAlpha(color,32).getRGB(), light);

        // On 1.16+, we get a vertex provider from the entity rendering pipeline, so we don't need to draw anything
        // manually like we do on older versions.
        //#if MC<11600
        buffer.drawDirect();
        //#endif

        //#if MC<11600
        if (alwaysOnTop) {
            UGraphics.enableDepth();
        }

        UGraphics.depthMask(true);
        //#endif

        if (alwaysOnTop) {
            //#if MC>=11600
            //$$ vertexConsumer = TextRenderTypeVertexConsumer.create(vertexConsumerProvider, false);
            //#else
            vertexConsumer = TextRenderTypeVertexConsumer.create(buffer);
            //#endif
            Diamond.drawDiamond(matrixStack, vertexConsumer, 6, vanillaX - 6, diamondCenter, color.getRGB(), light);

            //#if MC<11600
            buffer.drawDirect();
            //#endif
        }

        //#if MC<11600
        UGraphics.enableLighting();
        //#endif
    }

    public static float getDiamondBackgroundYMin() {
        //#if MC>=11600
        //$$ return 9;
        //#else
        return 8;
        //#endif
    }

    // Patcher mixins into this at HEAD to cancel it if the background is disabled.
    // Do NOT move, rename or change the return type.
    public static int getTextBackgroundOpacity() {
        //#if MC>=11600
        //$$ return (int) (UMinecraft.getMinecraft().gameSettings.getTextBackgroundOpacity(0.25F) * 255);
        //#else
        return 64;
        //#endif
    }

    private static float getDiamondYOffset() {
        //#if MC>=11600
        //$$ return -0.5f;
        //#else
        return 0;
        //#endif
    }

    // Different name to avoid the OldAnimations mixin to `drawTabIndicator`.
    public static void drawTabIndicatorOuter(
        UMatrixStack matrixStack,
        //#if MC>=11600
        //$$ IRenderTypeBuffer.Impl provider,
        //#endif
        NetworkPlayerInfo networkPlayerInfo,
        int x, int y
    ) {

        drawTabIndicator(
            matrixStack,
            //#if MC>=11600
            //$$ provider,
            //#endif
            networkPlayerInfo, x, y
        );
    }

    // this is modified in OldAnimations, don't remove or rename
    @SuppressWarnings("ConstantConditions")
    private static void drawTabIndicator(
        UMatrixStack matrixStack,
        //#if MC>=11600
        //$$ IRenderTypeBuffer.Impl provider,
        //#endif
        NetworkPlayerInfo networkPlayerInfo,
        int x, int y
    ) {
        if (!OnboardingData.hasAcceptedTos() || !EssentialConfig.INSTANCE.getShowEssentialIndicatorOnTab() || networkPlayerInfo == null)
            return;

        ConnectionManager connectionManager = Essential.getInstance().getConnectionManager();
        ProfileManager profileManager = connectionManager.getProfileManager();
        GameProfile gameProfile = networkPlayerInfo.getGameProfile();
        if (gameProfile == null) return;
        UUID playerUuid = gameProfile.getId();
        if (playerUuid == null) return;

        if (playerUuid.version() == 2) {
            // Could be a fake tab entry, try to get their actual uuid
            UUID actualUuid = OnlineIndicator.findUUIDFromDisplayName(networkPlayerInfo.getDisplayName());
            if (actualUuid != null) playerUuid = actualUuid;
        }
        ProfileStatus status = profileManager.getStatus(playerUuid);
        if (status == ProfileStatus.OFFLINE) return;

        beforeTabDraw();

        BlendState prevBlendState = BlendState.active();
        BlendState.NORMAL.activate();

        matrixStack.push();

        //#if MC>=11600
        //$$ TextRenderTypeVertexConsumer vertexConsumer = TextRenderTypeVertexConsumer.create(provider);
        //#else
        UGraphics buffer = UGraphics.getFromTessellator();
        TextRenderTypeVertexConsumer vertexConsumer = TextRenderTypeVertexConsumer.create(buffer);
        //#endif

        // Draw indicator
        matrixStack.translate(0, 0, 5);

        Diamond.drawDiamond(matrixStack, vertexConsumer, 4, x - 9, y + 1.5f, EssentialPalette.ESSENTIAL_BLUE.getRGB());

        //#if MC>=11600
        //$$ provider.finish();
        //#else
        buffer.drawDirect();
        //#endif

        matrixStack.pop();

        prevBlendState.activate();

        afterTabDraw();
    }

    /**
     * Certain servers will use UUID v2s in the tab list to change the player's name to include prefixes, etc.
     * This means that we need to parse all the extra data out of the username in order to get their actual UUID.
     */
    public static UUID findUUIDFromDisplayName(ITextComponent displayName) {
        if (displayName == null) {
            return null;
        }

        NetHandlerPlayClient netHandler = UMinecraft.getNetHandler();
        if (netHandler == null) {
            return null;
        }

        Map<String, UUID> nameToIdCache = ((NetHandlerPlayClientExt) netHandler).essential$getNameIdCache();

        //#if MC>=11600
        //$$ String unformattedText = displayName.getString();
        //#else
        String unformattedText = displayName.getUnformattedText();
        //#endif

        // We need to replace any invalid username characters (e.g. `[`, `★`, etc.) with a space.
        // This will allow us to iterate over the parts of the name to see which one is the
        // user's actual name.
        // For example, `[18] ★★ caoimheee` will be split into `18` and `caoimheee`.
        String[] nameParts = unformattedText.replaceAll("\\W", " ").split(" ");
        for (String part : nameParts) {
            // Splitting by " " inserts some empty String elements into the array.
            if (part.isEmpty()) continue;

            if (nameToIdCache.containsKey(part)) {
                return nameToIdCache.get(part);
            }
        }

        return null;
    }

    public static void beforeTabDraw() {
        //#if MC>=11800
        //$$ ImmediatelyFastCompat.beforeHudDraw();
        //#endif
    }

    public static void afterTabDraw() {
        //#if MC>=11800
        //$$ ImmediatelyFastCompat.afterHudDraw();
        //#endif
    }
}
