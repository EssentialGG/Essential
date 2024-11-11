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
package gg.essential.network.connectionmanager.cosmetics

import gg.essential.elementa.state.v2.ReferenceHolder
import gg.essential.gui.elementa.state.v2.collections.TrackedList
import gg.essential.network.cosmetics.Cosmetic
import java.util.function.Consumer

fun CosmeticsData.onNewCosmetic(owner: ReferenceHolder, consumer: Consumer<Cosmetic>) {
    var oldList = cosmetics.get()
    cosmetics.onSetValue(owner) { newList ->
        val changes = newList.getChangesSince(oldList).also { oldList = newList }
        for (change in changes) {
            if (change is TrackedList.Add) {
                consumer.accept(change.element.value)
            }
        }
    }
}
