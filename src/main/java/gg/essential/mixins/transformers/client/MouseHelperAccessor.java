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
package gg.essential.mixins.transformers.client;

//#if MC>=11600
//$$ import net.minecraft.client.MouseHelper;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.gen.Accessor;
//$$
//$$ @Mixin(MouseHelper.class)
//$$ public interface MouseHelperAccessor {
//$$     @Accessor
//$$     void setMouseX(double value);
//$$     @Accessor
//$$     void setMouseY(double value);
//$$ }
//#else
@org.spongepowered.asm.mixin.Mixin(gg.essential.mixins.DummyTarget.class)
public interface MouseHelperAccessor {
}
//#endif
