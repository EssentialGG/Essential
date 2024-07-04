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

import com.google.common.base.Suppliers;
import com.mojang.authlib.properties.Property;
import gg.essential.Essential;
import gg.essential.api.utils.mojang.SkinResponse;
import gg.essential.event.client.ReAuthEvent;
import gg.essential.gui.notification.Notifications;
import gg.essential.lib.gson.JsonObject;
import gg.essential.mod.Model;
import gg.essential.util.DispatchersKt;
import gg.essential.util.JsonHolder;
import gg.essential.util.ModelKt;
import gg.essential.util.MojangAPI;
import gg.essential.mod.Skin;
import gg.essential.util.SkinKt;
import gg.essential.util.UUIDUtil;
import gg.essential.util.WebUtil;
import kotlinx.coroutines.Dispatchers;
import me.kbrewster.eventbus.Subscribe;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

import static kotlinx.coroutines.ExecutorsKt.asExecutor;

public class MojangSkinManager {
    public static final String SESSION_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false";
    private final GameProfileManager gameProfileManager;
    private final BooleanSupplier delayChanges;
    private SkinUpdate queuedSkinChange;

    /**
     * The currently active skin at Mojang.
     *
     * Access must be synchronized appropriately, including any http calls to fetch and update the skin.
     */
    @NotNull
    private CompletableFuture<Skin> activeSkin;

    public MojangSkinManager(GameProfileManager gameProfileManager, BooleanSupplier delayChanges) {
        this.gameProfileManager = gameProfileManager;
        this.delayChanges = delayChanges;

        this.activeSkin = getSkinFromMinecraft();

        Essential.EVENT_BUS.register(new Object() {
            @Subscribe(priority = 2000)
            private void onReAuth(ReAuthEvent event) {
                activeSkin = getSkinFromMinecraft();
            }
        });
    }

    @NotNull
    public CompletableFuture<Skin> getActiveSkin() {
        return activeSkin;
    }

    @Nullable
    public static Property getTextureProperty(UUID uuid) {
        final JsonHolder jsonHolder = WebUtil.fetchJSON(String.format(Locale.ROOT, SESSION_URL, uuid.toString().replace("-", "")));
        //Http error
        if(jsonHolder.getKeys().isEmpty())
            return null;

        final JsonObject properties = jsonHolder.optJSONArray("properties").get(0).getAsJsonObject();
        return new com.mojang.authlib.properties.Property(
            "textures",
            properties.get("value").getAsString(),
            properties.get("signature").getAsString());
    }

    @Nullable
    public Skin uploadSkin(String accessToken, Model model, File file, boolean notification) {
        this.queuedSkinChange = new SkinUpload(accessToken, model, file);
        // Skin uploads are never delayed
        return updateSkinNow(notification, true);
    }

    public void changeSkin(String accessToken, Model model, String url) {
        this.queuedSkinChange = new SkinChange(accessToken, model, url);
        // Under certain circumstances we want to delay changes until a later point as to not spam Mojang too much
        if (this.delayChanges.getAsBoolean()) {
            return;
        }
        updateSkinNow(false, false /* don't care */);
    }

    public void flushChanges(boolean notification) {
        updateSkinNow(notification, false /* don't care */);
    }

    @Nullable
    private synchronized Skin updateSkinNow(boolean notification, boolean userSet) {
        SkinUpdate queuedSkinChange = this.queuedSkinChange;
        this.queuedSkinChange = null;
        if (queuedSkinChange == null) return null;

        if (queuedSkinChange.matches(activeSkin.join())) {
            return null; // if it's already the active skin, there's no need to update it
        }

        final SkinResponse updateSkinWithMojang = queuedSkinChange.execute();
        Optional<Skin> maybeSkin = Optional.ofNullable(updateSkinWithMojang)
            .flatMap(response -> Optional.ofNullable(response.getSkins()))
            .flatMap(skins -> skins.stream().filter(it -> "ACTIVE".equals(it.getState())).findFirst())
            .map(SkinKt::toMod);
        if (maybeSkin.isPresent()) {
            Skin skin = maybeSkin.get();
            gameProfileManager.updatePlayerSkin(UUIDUtil.getClientUUID(), skin.getHash(), skin.getModel().getType());
            activeSkin = CompletableFuture.completedFuture(skin);
            if (notification) {
                Notifications.INSTANCE.push("Skin", "Skin updated successfully");
            }
            return skin;
        } else {
            Notifications.INSTANCE.push("Skin", "Failed to upload skin, please try again");
            return null;
        }

    }

    @NotNull
    private CompletableFuture<Skin> getSkinFromMinecraft() {
        return CompletableFuture.supplyAsync(
                //#if MC>=12002
                //$$ () -> MinecraftClient.getInstance().getGameProfile().getProperties(),
                //#else
                // Note: getProfileProperties is not thread-safe, so we must evaluate it immediately
                Suppliers.ofInstance(Minecraft.getMinecraft().getProfileProperties()),
                //#endif
                asExecutor(DispatchersKt.getClient(Dispatchers.INSTANCE))
        ).thenApply(properties ->
                properties
                        .get("textures")
                        .stream()
                        .findFirst()
                        .map(SkinKt::propertyToSkin)
                        .orElse(new Skin("", Model.STEVE))
        );
    }

    private static abstract class SkinUpdate {
        protected final String accessToken;
        protected final Model model;

        protected SkinUpdate(String accessToken, Model model) {
            this.accessToken = accessToken;
            this.model = model;
        }

        public abstract boolean matches(Skin activeSkin);
        public abstract SkinResponse execute();
    }

    private static class SkinUpload extends SkinUpdate {
        private final File file;

        protected SkinUpload(String accessToken, Model model, File file) {
            super(accessToken, model);
            this.file = file;
        }

        @Override
        public boolean matches(Skin activeSkin) {
            return false; // we don't know which hash Mojang will assign
        }

        @Override
        public SkinResponse execute() {
            return MojangAPI.INSTANCE.uploadSkin(accessToken, ModelKt.toAPIModel(model), file);
        }
    }

    private static class SkinChange extends SkinUpdate {
        private final String url;

        protected SkinChange(String accessToken, Model model, String url) {
            super(accessToken, model);
            this.url = url;
        }

        @Override
        public boolean matches(Skin activeSkin) {
            return activeSkin.getHash().equals(Skin.hashFromUrl(url)) && activeSkin.getModel() == model;
        }

        @Override
        public SkinResponse execute() {
            return MojangAPI.INSTANCE.changeSkin(accessToken, UUIDUtil.getClientUUID(), ModelKt.toAPIModel(model), url);
        }
    }
}
