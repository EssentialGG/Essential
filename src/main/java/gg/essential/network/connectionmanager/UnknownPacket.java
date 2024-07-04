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
package gg.essential.network.connectionmanager;

import gg.essential.connectionmanager.common.packet.Packet;

/**
 * Dummy packet class for when a packet is received with a type which we do not have the class file for, usually because
 * feature-flags-processor found it to be unused.
 * In these cases, if we did not have another packet to substitute, we'd just drop the data and never invoke the reply
 * handler. But we do want to invoke all reply handlers, even if they don't care about the exact reply (which is likely
 * why the original packet got removed in the first place).
 */
class UnknownPacket extends Packet {
}
