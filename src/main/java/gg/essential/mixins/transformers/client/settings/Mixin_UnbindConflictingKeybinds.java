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

import gg.essential.Essential;
import gg.essential.key.EssentialKeybinding;
import gg.essential.universal.UKeyboard;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 11600
//#if FORGE
//$$ import net.minecraftforge.client.settings.KeyModifier;
//#endif
//$$ import net.minecraft.client.util.InputMappings;
//#endif

@Mixin(GameSettings.class)
public class Mixin_UnbindConflictingKeybinds {

    @Shadow
    private KeyBinding keyBindSaveToolbar;

    @Inject(method = "loadOptions", at = @At("RETURN"))
    private void Essential$unbindLoadedConflictingKeybinds(CallbackInfo ci) {
        // Unbind default save toolbar activator keybind if it conflicts with zoom keybind
        unbindIfConflicting(getKeyBindZoom(), this.keyBindSaveToolbar);
    }

    //#if MC >= 11600
    //$$ @Inject(method = "setKeyBindingCode", at = @At("RETURN"))
    //$$ private void Essential$unbindSetConflictingKeybinds(KeyBinding key, InputMappings.Input keyCode, CallbackInfo ci) {
    //#else
    @Inject(method = "setOptionKeyBinding", at = @At("RETURN"))
    private void Essential$unbindSetConflictingKeybinds(KeyBinding key, int keyCode, CallbackInfo ci) {
        //#endif
        // Unbind either save toolbar activator or zoom keybind if they conflict, whichever is unchanged
        if (key == getKeyBindZoom()) {
            unbindIfConflicting(key, this.keyBindSaveToolbar);
        } else if (key == this.keyBindSaveToolbar) {
            unbindIfConflicting(key, getKeyBindZoom());
        }
    }

    @Unique
    private KeyBinding getKeyBindZoom() {
        EssentialKeybinding zoomKey = Essential.getInstance().getKeybindingRegistry().getZoom();
        return zoomKey == null ? null : zoomKey.keyBinding;
    }

    @Unique
    private boolean conflicts(KeyBinding key1, KeyBinding key2) {
        //#if MC >= 11200
        return key1.conflicts(key2);
        //#else
        //$$ return key1.getKeyCode() == key2.getKeyCode();
        //#endif
    }

    @Unique
    private void unbindKeybind(KeyBinding keyBinding) {
        //#if MC >= 11600
        //#if FORGE
        //$$ keyBinding.setKeyModifierAndCode(KeyModifier.NONE,
        //#else
        //$$ keyBinding.setBoundKey(
        //#endif
        //$$     InputMappings.INPUT_INVALID);
        //#else
        keyBinding.setKeyCode(UKeyboard.KEY_NONE);
        //#endif

        KeyBinding.resetKeyBindingArrayAndHash();
    }

    @Unique
    private void unbindIfConflicting(KeyBinding keep, KeyBinding unbind) {
        if (keep != null & unbind != null && conflicts(keep, unbind)) {
            unbindKeybind(unbind);
        }
    }
}
