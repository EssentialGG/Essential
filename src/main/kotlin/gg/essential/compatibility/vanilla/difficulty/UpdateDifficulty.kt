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
// 1.12.2 and below
package gg.essential.compatibility.vanilla.difficulty

import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.network.NetHandlerPlayServer
import net.minecraft.network.PacketBuffer
import net.minecraft.world.EnumDifficulty
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import org.apache.logging.log4j.LogManager

internal class UpdateDifficulty(
    var difficulty: EnumDifficulty = EnumDifficulty.NORMAL,
) : IMessage {
    override fun fromBytes(buf: ByteBuf) {
        difficulty = PacketBuffer(buf).readEnumValue(EnumDifficulty::class.java)
    }

    override fun toBytes(buf: ByteBuf) {
        PacketBuffer(buf).writeEnumValue(difficulty)
    }

    internal class Handler : IMessageHandler<UpdateDifficulty, IMessage> {
        override fun onMessage(message: UpdateDifficulty, context: MessageContext): IMessage? {
            when (val netHandler = context.netHandler) {
                is NetHandlerPlayServer -> netHandler.player.serverWorld.addScheduledTask {
                    val player = netHandler.player
                    val server = player.mcServer
                    val hasPerms = player.canUseCommand(2, "difficulty")
                    val isHost = player.name == server.serverOwner
                    val isLocked = server.worlds.first().worldInfo.isDifficultyLocked
                    if ((!hasPerms && !isHost) || isLocked) {
                        Net.WRAPPER.sendTo(UpdateDifficulty(server.difficulty), player)
                        return@addScheduledTask
                    }
                    LOGGER.info("{} changed difficulty to {}, from {}", player.name, message.difficulty, server.difficulty)
                    server.setDifficultyForAllWorlds(message.difficulty)
                }
                is NetHandlerPlayClient -> Minecraft.getMinecraft().addScheduledTask {
                    Minecraft.getMinecraft().world.worldInfo.setDifficultyFromServer(message.difficulty)
                }
            }
            return null
        }

        companion object {
            private val LOGGER = LogManager.getLogger(Handler::class.java)
        }
    }
}