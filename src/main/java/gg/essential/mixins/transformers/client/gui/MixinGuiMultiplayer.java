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

import gg.essential.config.EssentialConfig;
import gg.essential.gui.multiplayer.EssentialMultiplayerGui;
import gg.essential.mixins.ext.client.gui.GuiMultiplayerExt;
import gg.essential.universal.UMatrixStack;
import gg.essential.universal.UMinecraft;
import kotlin.collections.CollectionsKt;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.LanServerInfo;
import net.minecraft.client.renderer.GlStateManager;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;

//#if MC>=12000
//$$ import net.minecraft.client.gui.DrawContext;
//#endif

//#if MC>=11600
//$$ import com.mojang.blaze3d.matrix.MatrixStack;
//#endif

//#if MC>=12004
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//$$ import net.minecraft.client.MinecraftClient;
//$$ import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
//#endif

@Mixin(GuiMultiplayer.class)
public abstract class MixinGuiMultiplayer extends GuiScreen implements GuiMultiplayerExt {

    //#if MC>=12004
    //$$ private static final String LIST_WIDGET_INIT = "Lnet/minecraft/client/gui/screen/multiplayer/MultiplayerServerListWidget;";
    //#else
    private static final String LIST_WIDGET_INIT = "Lnet/minecraft/client/gui/ServerSelectionList;<init>(Lnet/minecraft/client/gui/GuiMultiplayer;Lnet/minecraft/client/Minecraft;IIIII)V";
    //#endif

    //#if MC>=12004
    //$$ private static final String SET_DIMENSIONS = "Lnet/minecraft/client/gui/screen/multiplayer/MultiplayerServerListWidget;setDimensionsAndPosition(IIII)V";
    //#elseif MC>=11600
    //$$ private static final String SET_DIMENSIONS = "Lnet/minecraft/client/gui/screen/ServerSelectionList;updateSize(IIII)V";
    //#else
    private static final String SET_DIMENSIONS = "Lnet/minecraft/client/gui/ServerSelectionList;setDimensions(IIII)V";
    //#endif

    //#if MC>=11600
    //$$ protected MixinGuiMultiplayer() {
    //$$     super(null);
    //$$ }
    //#endif

    @Shadow
    @Final
    private GuiScreen parentScreen;

    @Shadow
    protected abstract void refreshServerList();

    @Unique
    private final EssentialMultiplayerGui essentialGui = new EssentialMultiplayerGui();

    @NotNull
    @Override
    public EssentialMultiplayerGui essential$getEssentialGui() {
        return essentialGui;
    }

    @Override
    public void essential$refresh() {
        this.refreshServerList();
    }

    @Override
    public void essential$close() {
        UMinecraft.getMinecraft().displayGuiScreen(this.parentScreen);
    }

    @Inject(method = "initGui", at = @At("RETURN"))
    private void initEssentialGui(CallbackInfo ci) {
        essentialGui.initGui((GuiMultiplayer) (Object) this);
        essentialGui.setupButtons(
            //#if MC>=11700
            //$$ CollectionsKt.filterIsInstance(this.children(), ButtonWidget.class),
            //$$ this::addDrawableChild,
            //#else
            CollectionsKt.filterIsInstance(this.buttonList, GuiButton.class),
            this::addButton,
            //#endif
            this::removeButton
        );
    }

    @Inject(method = "refreshServerList", at = @At("HEAD"))
    private void essential$markRefresh(CallbackInfo ci) {
        EssentialMultiplayerGui.Companion.setRefreshing(true);
    }

    //#if MC < 11200
    //$$ private GuiButton addButton(GuiButton button) {
    //$$     this.buttonList.add(button);
    //$$     return button;
    //$$ }
    //#endif

    private GuiButton removeButton(GuiButton button) {
        //#if MC>=11700
        //$$ this.remove(button);
        //#elseif MC>=11600
        //$$ this.buttons.remove(button);
        //$$ this.children.remove(button);
        //#else
        this.buttonList.remove(button);
        //#endif
        return button;
    }

    //#if MC>=12004
    //$$ @WrapOperation(method = "init", at = @At(value = "NEW", target = LIST_WIDGET_INIT))
    //$$ private MultiplayerServerListWidget shiftListDown$new(MultiplayerScreen screen, MinecraftClient client, int width, int height, int y, int itemHeight, Operation<MultiplayerServerListWidget> original) {
    //$$     if (EssentialConfig.INSTANCE.getEssentialEnabled()) {
    //$$         height -= 32;
    //$$         y += 32;
    //$$     }
    //$$     return original.call(screen, client, width, height, y, itemHeight);
    //$$ }
    //$$
    //$$ @WrapOperation(method = "init", at = @At(value = "INVOKE", target = SET_DIMENSIONS))
    //$$ private void shiftListDown$update(MultiplayerServerListWidget widget, int width, int height, int x, int y, Operation<Void> original) {
    //$$     if (EssentialConfig.INSTANCE.getEssentialEnabled()) {
    //$$         height -= 32;
    //$$         y += 32;
    //$$     }
    //$$     original.call(widget, width, height, x, y);
    //$$ }
    //#else
    @ModifyArg(method = "initGui", at = @At(value = "INVOKE", target = LIST_WIDGET_INIT), index = 4)
    private int shiftListDown$new(int topSpace) {
        if (!EssentialConfig.INSTANCE.getEssentialFull()) return topSpace;
        return topSpace + 32;
    }

    @ModifyArg(method = "initGui", at = @At(value = "INVOKE", target = SET_DIMENSIONS), index = 2)
    private int shiftListDown$update(int topSpace) {
        if (!EssentialConfig.INSTANCE.getEssentialFull()) return topSpace;
        return topSpace + 32;
    }
    //#endif

    @ModifyArg(method = "updateScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ServerSelectionList;updateNetworkServers(Ljava/util/List;)V"))
    private List<LanServerInfo> suppressLanServersOnEssentialTabs(List<LanServerInfo> lanServers) {
        if (!EssentialConfig.INSTANCE.getEssentialFull()) return lanServers;
        if (EssentialConfig.INSTANCE.getCurrentMultiplayerTab() != 0) {
            return Collections.emptyList();
        }
        return lanServers;
    }

    //#if MC>=11600
    //$$ @Inject(method = "func_214287_a", at = @At("RETURN"))
    //#else
    @Inject(method = "selectServer", at = @At("RETURN"))
    //#endif
    private void updateButtonState(CallbackInfo ci) {
        essentialGui.updateButtonState();
    }

    //#if MC<11600
    @Inject(method = "actionPerformed", at = @At("HEAD"))
    private void onButtonClicked(GuiButton button, CallbackInfo ci) {
        essentialGui.onButtonClicked(button);
    }
    //#endif

    @Inject(method = "drawScreen", at = @At("RETURN"))
    //#if MC>=12000
    //$$ private void drawEssentialGui(DrawContext context, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
    //$$     UMatrixStack matrixStack = new UMatrixStack(context.getMatrices());
    //#elseif MC>=11600
    //$$ private void drawEssentialGui(MatrixStack vMatrixStack, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
    //$$     UMatrixStack matrixStack = new UMatrixStack(vMatrixStack);
    //#else
    private void drawEssentialGui(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        UMatrixStack matrixStack = new UMatrixStack();
    //#endif

        //#if MC<11600
        // Tooltip rendering enables these even though they should be disabled.
        // But since MC renders tooltips last, Mojang never noticed.
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        //#endif

        essentialGui.draw(matrixStack);
    }

    @Inject(method = "connectToServer", at = @At("HEAD"))
    private void essential$onConnectToServer(ServerData server, CallbackInfo ci) {
        essentialGui.onConnectToServer(server);
    }
}
