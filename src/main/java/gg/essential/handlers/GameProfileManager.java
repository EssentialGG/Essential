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
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import gg.essential.Essential;
import gg.essential.api.utils.JsonHolder;
import gg.essential.mod.Skin;
import gg.essential.network.connectionmanager.subscription.SubscriptionManager;
import gg.essential.util.UUIDUtil;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static gg.essential.util.ExtensionsKt.copy;

//#if MC>=12000
//$$ import com.mojang.authlib.minecraft.InsecurePublicKeyException;
//$$ import com.mojang.authlib.yggdrasil.ServicesKeySet;
//$$ import com.mojang.authlib.yggdrasil.ServicesKeyType;
//$$ import java.util.stream.Collectors;
//#else
import com.mojang.authlib.minecraft.InsecureTextureException;
//#endif

//#if MC>=11900
//$$ import com.mojang.authlib.yggdrasil.ServicesKeyInfo;
//$$ import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
//$$ import sun.misc.Unsafe;
//$$
//$$ import java.lang.reflect.Field;
//$$ import java.security.Signature;
//#endif

//#if MC>=12004
//$$ import com.mojang.authlib.minecraft.MinecraftProfileTextures;
//#endif

public class GameProfileManager implements SubscriptionManager.Listener {
    public static final String SKIN_URL = Skin.SKIN_URL;
    private final Map<UUID, Overwrites> uuidToOverwrites = new ConcurrentHashMap<>();

    public GameProfile handleGameProfile(GameProfile profile) {
        final Overwrites overwrites = this.uuidToOverwrites.get(profile.getId());
        if (overwrites == null) {
            return null;
        }

        final Property property = profile.getProperties().get("textures").stream().findFirst().orElse(null);
        if (property == null || ManagedTexturesProperty.hasOverwrites(property, overwrites)) {
            return null;
        }

        return overwrites.apply(profile);
    }

    public void updatePlayerSkin(UUID uuid, String hash, String type) {
        updatePlayerSkin(uuid, new Overwrites(hash, type, null));
    }

    private void updatePlayerSkin(UUID uuid, Overwrites overwrites) {
        this.uuidToOverwrites.put(uuid, overwrites);
    }

    @Override
    public void onSubscriptionRemoved(@NotNull Set<UUID> uuids) {
        for (UUID uuid : uuids) {
            this.uuidToOverwrites.remove(uuid);
        }
    }

    public static void updatePropertyMap(PropertyMap profileProperties, Property property) {
        profileProperties.removeAll("textures");
        profileProperties.put("textures", property);
    }

    public static class Overwrites {
        private final String skinHash;
        private final String skinType;
        private final String capeHash;

        public Overwrites(String skinHash, String skinType, String capeHash) {
            this.skinHash = skinHash;
            this.skinType = skinType;
            this.capeHash = capeHash;
        }

        public String apply(String originalValue, UUID id) {
            final JsonHolder root = new JsonHolder(new String(Base64.getDecoder().decode(originalValue)));
            if (!root.optString("profileId").isEmpty() && !id.equals(UUIDUtil.formatWithDashes(root.optString("profileId")))) {
                return originalValue;
            }
            final JsonHolder textures = root.optOrCreateJsonHolder("textures");

            if (this.skinHash != null || this.skinType != null) {
                final JsonHolder skin = textures.optOrCreateJsonHolder("SKIN");
                if (this.skinHash != null) {
                    String url = skin.optString("url");
                    if (!url.endsWith(this.skinHash)) {
                        skin.put("url", String.format(Locale.ROOT, SKIN_URL, this.skinHash));
                    }
                }
                if (this.skinType != null) {
                    if (this.skinType.equals("default")) {
                        skin.remove("metadata");
                    } else {
                        skin.optOrCreateJsonHolder("metadata").put("model", this.skinType);
                    }
                }
            }

            if (this.capeHash != null) {
                if (this.capeHash.isEmpty()) {
                    textures.remove("CAPE");
                } else {
                    final JsonHolder cape = textures.optOrCreateJsonHolder("CAPE");
                    String url = cape.optString("url");
                    if (!url.endsWith(this.capeHash)) {
                        cape.put("url", String.format(Locale.ROOT, SKIN_URL, this.capeHash));
                    }
                }
            }

            return Base64.getEncoder().encodeToString(root.toString().getBytes(StandardCharsets.UTF_8));
        }

        public GameProfile apply(GameProfile originalProfile) {
            GameProfile updatedProfile = copy(originalProfile);
            updatePropertyMap(updatedProfile.getProperties(), ManagedTexturesProperty.create(originalProfile, this));
            return updatedProfile;
        }
    }

    public static String getSafeTexturesValue(GameProfile profile) {
        Minecraft mc = Minecraft.getMinecraft();

        PropertyMap properties = null;
        try {
            //#if MC>=12004
            //$$ if (!mc.getSessionService().getTextures(profile).equals(MinecraftProfileTextures.EMPTY)) {
            //#else
            if (!mc.getSessionService().getTextures(profile, true).isEmpty()) {
            //#endif
                properties = profile.getProperties();
            }
        //#if MC>=12000
        //$$ } catch (InsecurePublicKeyException ignored) { // misnamed, e.g. also thrown for invalid signatures
        //#else
        } catch (InsecureTextureException ignored) {
        //#endif
        } catch (Exception e) {
            // Probably invalid data sent by the server
            Essential.logger.error("Error getting profile textures", e);
        }

        //#if MC>=12002
        //$$ // MC now loads the filled-in game profile upfront
        //#else
        if (properties == null && UUIDUtil.getClientUUID().equals(profile.getId())) {
            properties = mc.getProfileProperties();
        }
        //#endif

        if (properties != null) {
            Iterator<Property> textures = properties.get("textures").iterator();
            if (textures.hasNext()) {
                return textures.next().getValue();
            }
        }

        return "e30="; // "{}" encoded with base64
    }

    //#if MC>=12002
    //$$ private static class ManagedTexturesProperty {
    //$$     private static final Map<Property, Overwrites> INSTANCES =
    //$$         // Using guava's weak map instead of the JRE one because guava uses identity rather than
    //$$         // hashCode/equals, so we can independently store two otherwise identical properties (and if one of
    //$$         // them gets GCed, the other can still function properly).
    //$$         new com.google.common.collect.MapMaker().weakKeys().makeMap();
    //$$
    //$$     public static Property create(GameProfile originalProfile, Overwrites overwrites) {
    //$$         String value = overwrites.apply(getSafeTexturesValue(originalProfile), originalProfile.getId());
    //$$         Property property = new Property("textures", value, "trusted");
    //$$         INSTANCES.put(property, overwrites);
    //$$         return property;
    //$$     }
    //$$
    //$$     public static boolean hasOverwrites(Property property, Overwrites overwrites) {
    //$$         return INSTANCES.get(property) == overwrites;
    //$$     }
    //$$ }
    //#else
    private static class ManagedTexturesProperty extends TrustedProperty {
        private final Overwrites overwrites;

        private ManagedTexturesProperty(GameProfile originalProfile, Overwrites overwrites) {
            super("textures", overwrites.apply(getSafeTexturesValue(originalProfile), originalProfile.getId()));

            this.overwrites = overwrites;
        }

        public static Property create(GameProfile originalProfile, Overwrites overwrites) {
            return new ManagedTexturesProperty(originalProfile, overwrites);
        }

        public static boolean hasOverwrites(Property property, Overwrites overwrites) {
            return property instanceof ManagedTexturesProperty
                    && ((ManagedTexturesProperty) property).overwrites == overwrites;
        }
    }

    private static class TrustedProperty extends Property {
        public TrustedProperty(String name, String value) {
            super(name, value, "trusted");
        }

        //#if MC>=11900
        //$$ @SuppressWarnings("deprecation")
        //#endif
        @Override
        public boolean isSignatureValid(PublicKey publicKey) {
            return true;
        }
    }
    //#endif

    //#if MC>=11900
    //$$ public static void register(YggdrasilAuthenticationService authenticationService) throws ReflectiveOperationException {
    //$$     Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
    //$$     theUnsafeField.setAccessible(true);
    //$$     Unsafe unsafe = (Unsafe) theUnsafeField.get(null);
    //$$
        //#if MC>=12000
        //$$ Field servicesKeySetField = YggdrasilAuthenticationService.class.getDeclaredField("servicesKeySet");
        //$$ long servicesKeySetOffset = unsafe.objectFieldOffset(servicesKeySetField);
        //$$ ServicesKeySet originalSet = (ServicesKeySet) unsafe.getObject(authenticationService, servicesKeySetOffset);
        //$$ ServicesKeySet wrapperSet = (type) -> {
        //$$     if (type != ServicesKeyType.PROFILE_PROPERTY) {
        //$$         return originalSet.keys(type);
        //$$     }
        //$$     return originalSet.keys(type).stream().map(TrustingServicesKeyInfo::new).collect(Collectors.toList());
        //$$ };
        //$$ unsafe.putObject(authenticationService, servicesKeySetOffset, wrapperSet);
        //#else
        //$$ Field servicesKeyField = YggdrasilAuthenticationService.class.getDeclaredField("servicesKey");
        //$$ long servicesKeyOffset = unsafe.objectFieldOffset(servicesKeyField);
        //$$ ServicesKeyInfo originalKey = (ServicesKeyInfo) unsafe.getObject(authenticationService, servicesKeyOffset);
        //$$ ServicesKeyInfo wrapperKey = new TrustingServicesKeyInfo(originalKey);
        //$$ unsafe.putObject(authenticationService, servicesKeyOffset, wrapperKey);
        //#endif
    //$$ }
    //$$
    //$$ private record TrustingServicesKeyInfo(ServicesKeyInfo inner) implements ServicesKeyInfo {
    //$$     @Override
    //$$     public int keyBitCount() {
    //$$         return inner.keyBitCount();
    //$$     }
    //$$
    //$$     @Override
    //$$     public int signatureBitCount() {
    //$$         return inner.signatureBitCount();
    //$$     }
    //$$
    //$$     @Override
    //$$     public Signature signature() {
    //$$         return inner.signature();
    //$$     }
    //$$
    //$$     @Override
    //$$     public boolean validateProperty(Property property) {
            //#if MC>=12002
            //$$ return ManagedTexturesProperty.INSTANCES.containsKey(property) || inner.validateProperty(property);
            //#else
            //$$ return property instanceof TrustedProperty || inner.validateProperty(property);
            //#endif
    //$$     }
    //$$ }
    //#endif
}
