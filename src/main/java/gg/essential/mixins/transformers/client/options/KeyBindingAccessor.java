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
package gg.essential.mixins.transformers.client.options;

import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//#if MC>=11202
import java.util.Map;
//#if MC>=11400
//$$ import net.minecraft.client.util.InputMappings;
//#endif
//#else
//$$ import java.util.List;
//#endif

@Mixin(KeyBinding.class)
public interface KeyBindingAccessor {
    //#if MC>=11400
    //$$ @Accessor("keyCode")
    //$$ InputMappings.Input getBoundKey();
    //#endif

    @Accessor("KEYBIND_ARRAY")
    //#if MC>=11202
    static Map<String, KeyBinding> getKeybinds() {
    //#else
    //$$ static List<KeyBinding> getKeybinds() {
    //#endif
        throw new AssertionError();
    }
}
