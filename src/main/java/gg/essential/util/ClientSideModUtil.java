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
package gg.essential.util;

//#if MC<=11202
import com.google.common.collect.ImmutableSet;
import gg.essential.Essential;
import net.minecraftforge.fml.common.FMLModContainer;
import net.minecraftforge.fml.common.ModContainer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

public class ClientSideModUtil {
    private static final Set<String> knownClientSideMods = ImmutableSet.<String>builder().addAll(ModLoaderUtil.KNOWN_CLIENT_SIDE_MODS).build();
    private static final MethodHandle getDescriptorHandle;

    static {
        MethodHandle discoveredHandle = null;
        try {
            Field descriptorField = FMLModContainer.class.getDeclaredField("descriptor");
            descriptorField.setAccessible(true);
            discoveredHandle = MethodHandles.lookup().unreflectGetter(descriptorField);
        } catch (Throwable t) {
            Essential.logger.error("Failed to make getter for FMLModContainer::descriptor: ", t);
        }
        getDescriptorHandle = discoveredHandle;
    }

    @SuppressWarnings("unchecked")
    public static boolean isModClientSide(ModContainer container) {
        if (container instanceof FMLModContainer) {
            boolean markedAsClientSideOnly = false;
            try {
                markedAsClientSideOnly = Boolean.TRUE.equals(((Map<String, Object>) getDescriptorHandle.invokeExact((FMLModContainer) container)).get("clientSideOnly"));
            } catch (Throwable t) {
                Essential.logger.error("Could not determine whether mod '" + container.getModId() + "' is client-side: ", t);
            }
            return markedAsClientSideOnly || knownClientSideMods.contains(container.getModId());
        }
        return false;
    }
}
//#endif
