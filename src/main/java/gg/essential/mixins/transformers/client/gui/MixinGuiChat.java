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

import net.minecraft.client.gui.GuiChat;
import org.spongepowered.asm.mixin.Mixin;


//#if MC<=10809
//$$ import gg.essential.commands.EssentialCommandRegistry;
//$$ import com.google.common.collect.ObjectArrays;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.ModifyVariable;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#endif

@Mixin(GuiChat.class)
public class MixinGuiChat {
    //#if MC<=10809
    //$$ private String[] essential$completionOptions = new String[0];
    //$$
    //$$ @Inject(method = "sendAutocompleteRequest", at = @At("HEAD"))
    //$$ public void onRequest(String request, String idk, CallbackInfo info) {
    //$$     if (request.length() >= 1)
    //$$         essential$completionOptions = EssentialCommandRegistry.INSTANCE.getCompletionOptions(request);
    //$$ }
    //$$
    //$$ @ModifyVariable(method = "onAutocompleteResponse", at = @At("HEAD"))
    //$$ public String[] sendChatMessage(String... message) {
    //$$     return ObjectArrays.concat(message, essential$completionOptions, String.class);
    //$$ }
    //#endif
}
