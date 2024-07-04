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
package gg.essential.mixins.transformers.client.gui;

import gg.essential.Essential;
import gg.essential.gui.multiplayer.FriendsIndicator;
import gg.essential.mixins.ext.client.gui.ServerListEntryNormalExt;
import gg.essential.network.connectionmanager.sps.SPSManager;
import gg.essential.universal.UMatrixStack;
import gg.essential.universal.UMinecraft;
import gg.essential.util.UUIDUtil;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.ServerListEntryNormal;
import net.minecraft.client.multiplayer.ServerData;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;
import java.util.UUID;

import static gg.essential.mixins.ext.client.multiplayer.ServerDataExtKt.getExt;

//#if MC>=12005
//$$ import net.minecraft.text.Text;
//#endif

//#if MC>=12000
//$$ import net.minecraft.client.gui.DrawContext;
//#endif

//#if MC>=11602
//$$ import com.mojang.blaze3d.matrix.MatrixStack;
//$$ import gg.essential.universal.wrappers.message.UTextComponent;
//$$ import gg.essential.util.HelpersKt;
//$$ import net.minecraft.util.text.ITextComponent;
//$$ import net.minecraft.util.text.ITextProperties;
//$$ import net.minecraft.util.text.TranslationTextComponent;
//$$ import java.util.Arrays;
//$$ import java.util.Collections;
//$$ import java.util.stream.Collectors;
//$$ import static gg.essential.util.HelpersKt.textLiteral;
//#else
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
//#endif

@Mixin(ServerListEntryNormal.class)
public abstract class MixinServerListEntryNormal implements ServerListEntryNormalExt {
    @Shadow @Final private ServerData server;
    @Shadow @Final private GuiMultiplayer owner;

    @Unique
    private FriendsIndicator friends;

    @Unique
    private int populationOrVersionTextWidth;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initFriends(CallbackInfo ci) {
        this.friends = new FriendsIndicator(this.server);
    }

    @NotNull
    @Override
    public FriendsIndicator essential$getFriends() {
        return this.friends;
    }

    //#if MC>=11600
    //$$ @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;getStringPropertyWidth(Lnet/minecraft/util/text/ITextProperties;)I"))
    //$$ private ITextProperties recordPopulationOrVersionTextWidth(ITextProperties text) {
    //$$     this.populationOrVersionTextWidth = UMinecraft.getFontRenderer().getStringPropertyWidth(text);
    //#else
    @ModifyArg(method = "drawEntry", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;getStringWidth(Ljava/lang/String;)I"))
    private String recordPopulationOrVersionTextWidth(String text) {
        this.populationOrVersionTextWidth = UMinecraft.getFontRenderer().getStringWidth(text);
    //#endif
        return text;
    }

    @Inject(method = "drawEntry", at = @At("RETURN"))
    private void drawFriendsIndicator(
        //#if MC>=12000
        //$$ DrawContext context,
        //#elseif MC>=11602
        //$$ MatrixStack vMatrixStack,
        //#endif
        int slotIndex,
        //#if MC>=11602
        //$$ int y,
        //$$ int x,
        //#else
        int x,
        int y,
        //#endif
        int listWidth,
        int slotHeight,
        int mouseX,
        int mouseY,
        boolean isSelected,
        //#if MC>=11202
        float partialTicks,
        //#endif
        CallbackInfo ci
    ) {
        //#if MC>=12000
        //$$ UMatrixStack matrixStack = new UMatrixStack(context.getMatrices());
        //#elseif MC>=11602
        //$$ UMatrixStack matrixStack = new UMatrixStack(vMatrixStack);
        //#else
        UMatrixStack matrixStack = new UMatrixStack();
        //#endif
        String tooltip = this.friends.draw(matrixStack, x, y, listWidth, mouseX, mouseY, populationOrVersionTextWidth);
        if (tooltip != null) {
            //#if MC>=12005
            //$$ this.screen.setTooltip(Arrays.stream(tooltip.split("\n")).map(HelpersKt::textLiteral).map(Text::asOrderedText).collect(Collectors.toList()));
            //#elseif MC>=11600
            //$$ this.owner.func_238854_b_(Arrays.stream(tooltip.split("\n")).map(HelpersKt::textLiteral).collect(Collectors.toList()));
            //#else
            this.owner.setHoveringText(tooltip);
            //#endif
        }
    }

    //#if MC>=11600
    //#if MC>=12005
    //$$ @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/multiplayer/MultiplayerScreen;setTooltip(Lnet/minecraft/text/Text;)V"))
    //$$ private Text addServerRegionToPing(Text str) {
    //#else
    //$$ @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/Collections;singletonList(Ljava/lang/Object;)Ljava/util/List;"))
    //$$ private Object addServerRegionToPing(Object str) {
    //#endif
        //#if MC>=11900
        //$$ Object content = ((Text) str).getContent();
        //#else
        //$$ Object content = str;
        //#endif
    //$$     if (!(content instanceof TranslationTextComponent)) return str;
    //$$     if (!"multiplayer.status.ping".equals(((TranslationTextComponent) content).getKey())) return str;
    //#else
    @ModifyConstant(method = "drawEntry", constant = @Constant(stringValue = "ms"))
    private String addServerRegionToPing(String str) {
    //#endif
        String region = getExt(this.server).getEssential$pingRegion();
        if (region != null) {
            // Uppercase cause it looks nicer, no trailing numbers cause those do not really add anything
            region = region.toUpperCase(Locale.ROOT);
            while (!region.isEmpty() && Character.isDigit(region.charAt(region.length() - 1))) {
                region = region.substring(0, region.length() - 1);
            }

            if ("SPS".equals(region)) {
                SPSManager spsManager = Essential.getInstance().getConnectionManager().getSpsManager();
                UUID hostUuid = spsManager.getHostFromSpsAddress(this.server.serverIP);
                if (hostUuid != null) {
                    region = UUIDUtil.getName(hostUuid).join();
                }
            }

            if (!region.isEmpty()) {
                //#if MC>=11600
                //$$ str = ((ITextComponent) str).deepCopy().append(new UTextComponent(" from " + region));
                //#else
                str += " from " + region;
                //#endif
            }
        }
        return str;
    }
}
