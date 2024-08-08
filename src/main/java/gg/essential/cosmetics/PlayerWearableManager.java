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
package gg.essential.cosmetics;

import com.google.common.collect.ImmutableMap;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import gg.essential.api.cosmetics.RenderCosmetic;
import gg.essential.config.EssentialConfig;
import gg.essential.cosmetics.source.CosmeticsSource;
import gg.essential.event.entity.PlayerTickEvent;
import gg.essential.gui.common.EmulatedUI3DPlayer;
import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import gg.essential.mod.Model;
import gg.essential.mod.cosmetics.CosmeticSlot;
import gg.essential.mod.cosmetics.CosmeticType;
import gg.essential.model.*;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.cosmetics.AssetLoader;
import gg.essential.network.connectionmanager.cosmetics.CosmeticsManager;
import gg.essential.network.connectionmanager.cosmetics.ModelLoader;
import gg.essential.network.connectionmanager.telemetry.ImpressionTelemetryManager;
import gg.essential.network.cosmetics.Cosmetic;
import gg.essential.util.UIdentifier;
import kotlin.Pair;
import me.kbrewster.eventbus.Subscribe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static gg.essential.handlers.GameProfileManager.SKIN_URL;
import static gg.essential.mod.cosmetics.CapeDisabledKt.CAPE_DISABLED_COSMETIC_ID;
import static gg.essential.util.UIdentifierKt.toU;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

//#if MC>=12002
//$$ import gg.essential.mixins.impl.client.resources.PlayerSkinProviderAccessor;
//#endif

public class PlayerWearableManager {

    private final ConnectionManager connectionManager;
    private final CosmeticsManager cosmeticsManager;
    private final ModelLoader modelLoader;

    public PlayerWearableManager(ConnectionManager connectionManager, CosmeticsManager cosmeticsManager) {
        this.connectionManager = connectionManager;
        this.cosmeticsManager = cosmeticsManager;
        this.modelLoader = cosmeticsManager.getModelLoader();
    }

    @Subscribe
    public void tick(PlayerTickEvent tickEvent) {
        if (!tickEvent.isPre()) return;

        EntityPlayer playerEntity = tickEvent.getPlayer();
        if (!(playerEntity instanceof AbstractClientPlayer)) {
            return;
        }
        AbstractClientPlayer player = (AbstractClientPlayer) playerEntity;

        updateCosmetics(player);
    }

    private void updateCosmetics(AbstractClientPlayer player) {
        AbstractClientPlayerExt playerExt = (AbstractClientPlayerExt) player;
        CosmeticsSource cosmeticsSource = playerExt.getCosmeticsSource();
        CosmeticsState oldState = playerExt.getCosmeticsState();

        //#if MC>=12002
        //$$ Model newSkinType = Model.byTypeOrDefault(player.getSkinTextures().model().getName());
        //#else
        Model newSkinType = Model.byTypeOrDefault(player.getSkinType());
        //#endif
        Model oldSkinType = oldState.getSkinType();

        ImmutableMap<CosmeticSlot, EquippedCosmetic> newCosmetics = cosmeticsSource.getCosmetics();
        Map<CosmeticSlot, EquippedCosmetic> oldCosmetics = oldState.getCosmetics();
        Set<EnumPart> newArmour = getArmourFromPlayer(player);
        Set<EnumPart> oldArmour = oldState.getArmor();

        if (Objects.equals(newCosmetics, oldCosmetics) && Objects.equals(newSkinType, oldSkinType) && Objects.equals(oldArmour, newArmour)) {
            return;
        }

        AssetLoader.Priority priority =
            player instanceof EmulatedUI3DPlayer.EmulatedPlayer ? AssetLoader.Priority.High : AssetLoader.Priority.Low;

        Map<Cosmetic, BedrockModel> models = new HashMap<>();
        String cape = null;
        Pair<List<UIdentifier>, @Nullable List<UIdentifier>> capeTextures = null;

        boolean allUpdated = true;
        for (EquippedCosmetic equippedCosmetic : newCosmetics.values()) {
            Cosmetic cosmetic = equippedCosmetic.getCosmetic();
            String variant = equippedCosmetic.getVariant();

            CosmeticType cosmeticType = cosmetic.getType();
            if (cosmeticType.getSlot() == CosmeticSlot.CAPE) {
                cape = cosmetic.getId();

                if (CAPE_DISABLED_COSMETIC_ID.equals(cape)) {
                    continue; // if cape is disabled, apply no texture
                }

                // If the cosmetic id is not the hash of an official cape (or the special Disabled one)
                // then it's one of our custom capes and we need to use its texture data
                CompletableFuture<Pair<List<UIdentifier>, @Nullable List<UIdentifier>>> capeFuture;
                if (cape.length() == 64) {
                    // If it is an official cape, we'll load it via MC for best compatibility
                    MinecraftProfileTexture texture = new MinecraftProfileTexture(String.format(Locale.ROOT, SKIN_URL, cape), emptyMap());
                    SkinManager skinProvider = Minecraft.getMinecraft().getSkinManager();
                    capeFuture =
                        //#if MC>=12002
                        //$$ ((PlayerSkinProviderAccessor) skinProvider).getCapeCache().invokeGet(texture)
                        //#else
                        CompletableFuture.completedFuture(skinProvider.loadSkin(texture, MinecraftProfileTexture.Type.CAPE))
                        //#endif
                            .thenApply(it -> new Pair<>(singletonList(toU(it)), null));
                } else {
                    // otherwise we need to use the texture data from the cosmetic
                    capeFuture = modelLoader.getCape(cosmetic, variant, priority);
                }

                if (!capeFuture.isDone()) {
                    allUpdated = false; // still loading, check again next tick
                    continue;
                }

                if (capeFuture.isCompletedExceptionally()) {
                    continue; // failed to load, this is not recoverable, let's just ignore the cape
                }

                capeTextures = capeFuture.join();
            }

            final CompletableFuture<BedrockModel> modelFuture = modelLoader.getModel(cosmetic, variant, newSkinType, priority);
            if (!modelFuture.isDone()) {
                allUpdated = false; // model still loading, check again next tick
                continue;
            }

            if (modelFuture.isCompletedExceptionally()) {
                // failed to load, this is not recoverable, so we do not need to check again on the next tick
                continue;
            }

            models.put(cosmetic, modelFuture.join());
        }


        if (!allUpdated) {
            return; // some cosmetics are still loading, check again next tick
        }

        WearablesManager wearablesManager = playerExt.getWearablesManager();
        CosmeticsState newState = new CosmeticsState(
            newSkinType,
            newCosmetics,
            models,
            newArmour
        );
        wearablesManager.updateState(newState);
        playerExt.setEssentialCosmeticsCape(cape, capeTextures);

        if (!(player instanceof EmulatedUI3DPlayer.EmulatedPlayer)) {
            for (EquippedCosmetic cosmetic : newCosmetics.values()) {
                ImpressionTelemetryManager.INSTANCE.addImpression(cosmetic.getId(), player.getUniqueID());
            }
        }
    }

    private Set<EnumPart> getArmourFromPlayer(AbstractClientPlayer player) {

        Set<EnumPart> equippedSlots = new HashSet<>();

        int armorSetting = EssentialConfig.INSTANCE.getCosmeticArmorSetting(player);
        if (armorSetting > 0) {
            return equippedSlots;
        }

        if (!canRenderCosmetic(player, 0) || !canRenderCosmetic(player, 1)) {
            equippedSlots.add(EnumPart.LEFT_LEG);
            equippedSlots.add(EnumPart.RIGHT_LEG);
        }
        if (!canRenderCosmetic(player, 2)) {
            equippedSlots.add(EnumPart.LEFT_ARM);
            equippedSlots.add(EnumPart.RIGHT_ARM);
            equippedSlots.add(EnumPart.BODY);
        }
        if (!canRenderCosmetic(player, 3)) {
            equippedSlots.add(EnumPart.HEAD);
        }

        return equippedSlots;
    }

    private boolean canRenderCosmetic(AbstractClientPlayer player, int slot) {
        //#if MC>=11700
        //$$ PlayerInventory inventory = player.getInventory();
        //#else
        InventoryPlayer inventory = player.inventory;
        //#endif

        ItemStack stack = inventory.armorItemInSlot(slot);

        final boolean[] armorRenderingSuppressed = ((AbstractClientPlayerExt) player).wasArmorRenderingSuppressed();

        return isEmpty(stack) || stack.getItem() instanceof RenderCosmetic || armorRenderingSuppressed[slot];
    }

    private boolean isEmpty(ItemStack stack) {
        //#if MC>=11200
        return stack.isEmpty();
        //#else
        //$$ return stack == null;
        //#endif
    }
}
