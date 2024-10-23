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
package gg.essential.mixins.transformers.client.settings;

import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static gg.essential.mixins.impl.client.settings.KeybindUtils.getKeyBindSaveToolbar;
import static gg.essential.mixins.impl.client.settings.KeybindUtils.getKeyBindZoom;
import static gg.essential.mixins.impl.client.settings.KeybindUtils.unbindIfConflicting;

//#if MC >= 11600
//$$ import net.minecraft.client.util.InputMappings;
//#endif

//#if MC>=12102
//$$ @Mixin(KeyBinding.class)
//#else
@Mixin(GameSettings.class)
//#endif
public class Mixin_UnbindConflictingKeybinds_OnChange {
    //#if MC>=12102
    //$$ @Inject(method = "setBoundKey", at = @At("RETURN"))
    //$$ private void Essential$unbindSetConflictingKeybinds(InputUtil.Key keyCode, CallbackInfo ci) {
    //$$     KeyBinding key = (KeyBinding) (Object) this;
    //#elseif MC >= 11600
    //$$ @Inject(method = "setKeyBindingCode", at = @At("RETURN"))
    //$$ private void Essential$unbindSetConflictingKeybinds(KeyBinding key, InputMappings.Input keyCode, CallbackInfo ci) {
    //#else
    @Inject(method = "setOptionKeyBinding", at = @At("RETURN"))
    private void Essential$unbindSetConflictingKeybinds(KeyBinding key, int keyCode, CallbackInfo ci) {
    //#endif
        // Unbind either save toolbar activator or zoom keybind if they conflict, whichever is unchanged
        if (key == getKeyBindZoom()) {
            unbindIfConflicting(key, getKeyBindSaveToolbar());
        } else if (key == getKeyBindSaveToolbar()) {
            unbindIfConflicting(key, getKeyBindZoom());
        }
    }
}
