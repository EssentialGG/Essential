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
package gg.essential.mixins.transformers.server.integrated;

import com.google.common.util.concurrent.Uninterruptibles;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;
import java.net.Proxy;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

//#if MC>=11900
//$$ import net.minecraft.util.ApiServices;
//#endif

//#if MC>=11802
//$$ import net.minecraft.server.SaveLoader;
//#endif

//#if MC>=11400
//$$ import net.minecraft.resources.DataPackRegistries;
//$$ import net.minecraft.resources.ResourcePackList;
//$$ import net.minecraft.util.registry.DynamicRegistries;
//$$ import net.minecraft.world.chunk.listener.IChunkStatusListenerFactory;
//$$ import net.minecraft.world.storage.IServerConfiguration;
//$$ import net.minecraft.world.storage.SaveFormat;
//$$ import org.spongepowered.asm.mixin.Unique;
//#endif

//#if MC>=11202
//#if MC<11900
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
//#if MC < 11400
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
//#endif
import net.minecraft.server.management.PlayerProfileCache;
//#endif
import net.minecraft.util.datafix.DataFixer;
//#endif

@Mixin(IntegratedServer.class)
public abstract class MixinIntegratedServer extends MinecraftServer {
    //#if MC>=11900
    //$$ public MixinIntegratedServer(Thread serverThread, LevelStorage.Session session, ResourcePackManager dataPackManager, SaveLoader saveLoader, Proxy proxy, DataFixer dataFixer, ApiServices apiServices, WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory) {
    //$$     super(serverThread, session, dataPackManager, saveLoader, proxy, dataFixer, apiServices, worldGenerationProgressListenerFactory);
    //$$ }
    //#elseif MC>=11802
    //$$ public MixinIntegratedServer(Thread serverThread, LevelStorage.Session session, ResourcePackManager dataPackManager, SaveLoader saveLoader, Proxy proxy, DataFixer dataFixer, MinecraftSessionService sessionService, GameProfileRepository gameProfileRepo, UserCache userCache, WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory) {
    //$$     super(serverThread, session, dataPackManager, saveLoader, proxy, dataFixer, sessionService, gameProfileRepo, userCache, worldGenerationProgressListenerFactory);
    //$$ }
    //#elseif MC>=11400
    //$$ public MixinIntegratedServer(Thread serverThread, DynamicRegistries.Impl p_i232576_2_, SaveFormat.LevelSave anvilConverterForAnvilFile, IServerConfiguration p_i232576_4_, ResourcePackList dataPacks, Proxy serverProxy, DataFixer dataFixer, DataPackRegistries dataRegistries, MinecraftSessionService sessionService, GameProfileRepository profileRepo, PlayerProfileCache profileCache, IChunkStatusListenerFactory chunkStatusListenerFactory) {
    //$$     super(serverThread, p_i232576_2_, anvilConverterForAnvilFile, p_i232576_4_, dataPacks, serverProxy, dataFixer, dataRegistries, sessionService, profileRepo, profileCache, chunkStatusListenerFactory);
    //$$ }
    //#elseif MC>=11202
    public MixinIntegratedServer(File anvilFileIn, Proxy proxyIn, DataFixer dataFixerIn, YggdrasilAuthenticationService authServiceIn, MinecraftSessionService sessionServiceIn, GameProfileRepository profileRepoIn, PlayerProfileCache profileCacheIn) {
        super(anvilFileIn, proxyIn, dataFixerIn, authServiceIn, sessionServiceIn, profileRepoIn, profileCacheIn);
    }
    //#else
    //$$ public MixinIntegratedServer(Proxy proxy, File workDir) {
    //$$     super(proxy, workDir);
    //$$ }
    //#endif

    // There is a race condition in vanilla where it waits for a task which it submitted to the server to be finished,
    // but if the server had already begun to shut down, then it will no longer execute its task queue, freezing the
    // client thread.
    // Forge includes a patch that is meant to fix this bug, but it's insufficient and merely reduces the window size
    // and that does not fix it completely.
    // This Mixin replaces the unlimited wait with one which wakes up once a second to check if the server is already
    // shut down, and if it is, then it gives up on the task and returns without it being completed.
    //#if MC>=11400
    //$$ @Unique
    //#else
    @Redirect(method = "initiateShutdown", at = @At(value = "INVOKE", target = "Lcom/google/common/util/concurrent/Futures;getUnchecked(Ljava/util/concurrent/Future;)Ljava/lang/Object;", remap = false))
    //#endif
    private <V> V workaroundRaceCondition(Future<V> future) {
        while (!isServerStopped()) {
            try {
                return Uninterruptibles.getUninterruptibly(future, 1, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException ignored) {
            }
        }
        return null;
    }

    //#if MC>=11400
    //$$ @Redirect(method = "initiateShutdown", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/integrated/IntegratedServer;runImmediately(Ljava/lang/Runnable;)V"))
    //$$ private void workaroundRaceCondition(IntegratedServer integratedServer, Runnable task) {
    //$$     if (!this.isOnExecutionThread()) {
    //$$         workaroundRaceCondition(this.runAsync(task));
    //$$     } else {
    //$$         task.run();
    //$$     }
    //$$ }
    //#endif
}
