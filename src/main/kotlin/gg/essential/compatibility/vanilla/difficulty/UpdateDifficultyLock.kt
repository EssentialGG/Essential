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
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import org.apache.logging.log4j.LogManager

internal class UpdateDifficultyLock(
    var locked: Boolean = true,
) : IMessage {
    override fun fromBytes(buf: ByteBuf) {
        locked = buf.readBoolean()
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeBoolean(locked)
    }

    internal class Handler : IMessageHandler<UpdateDifficultyLock, IMessage> {
        override fun onMessage(message: UpdateDifficultyLock, context: MessageContext): IMessage? {
            when (val netHandler = context.netHandler) {
                is NetHandlerPlayServer -> netHandler.player.serverWorld.addScheduledTask {
                    val player = netHandler.player
                    val server = player.mcServer
                    val isHost = player.name == server.serverOwner
                    val isLocked = server.worlds.first().worldInfo.isDifficultyLocked
                    if (!isHost || isLocked || !message.locked) {
                        Net.WRAPPER.sendTo(UpdateDifficultyLock(isLocked), player)
                        return@addScheduledTask
                    }
                    LOGGER.info("Locking difficulty to {}", server.difficulty)
                    server.worlds.filterNotNull().forEach { it.worldInfo.isDifficultyLocked = message.locked }
                    Net.WRAPPER.sendToAll(UpdateDifficultyLock(message.locked))
                }
                is NetHandlerPlayClient -> Minecraft.getMinecraft().addScheduledTask {
                    Minecraft.getMinecraft().world.worldInfo.setDifficultyLockedFromServer(message.locked)
                }
            }
            return null
        }

        companion object {
            private val LOGGER = LogManager.getLogger(UpdateDifficultyLock.Handler::class.java)
        }
    }
}