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
package gg.essential.mixins.ext.client.gui

import java.util.TreeMap

//#if MC>=11904
//$$ import net.minecraft.client.gui.Element
//#elseif MC>=11600
//$$ import net.minecraft.client.gui.widget.list.AbstractList
//#else
import net.minecraft.client.gui.GuiListExtended.IGuiListEntry
//#endif

interface SelectionListWithDividers
    //#if MC>=11904
    //$$ <T : Element> // not ideal, but EntryListWidget.Entry is protected
    //#elseif MC>=11600
    //$$ <T : AbstractList.AbstractListEntry<*>>
    //#else
    <T : IGuiListEntry>
    //#endif
{
    /**
     * A map of entry indices to dividers. A TreeMap is used to allow easily determining the number
     * of dividers that occur before a given index. The keys of this map reflect the final index
     * of the dividers in the list of server entries and divider entries. This adjustment is done
     * in `essential$setDividers`.
     */
    var `essential$offsetDividers`: TreeMap<Int, T?>

    /**
     * Set the dividers for this server list.
     * @see DividerServerListEntry
     */
    fun `essential$setDividers`(dividers: Map<Int, T>?) {
        if (dividers == null) {
            `essential$offsetDividers` = TreeMap()
        } else {
            val sortedDividers = TreeMap(dividers)
            val offsetDividers = TreeMap<Int, T?>()
            sortedDividers.onEachIndexed { i, (j, divider) -> offsetDividers[i + j] = divider }
            `essential$offsetDividers` = offsetDividers
        }
    }
}
