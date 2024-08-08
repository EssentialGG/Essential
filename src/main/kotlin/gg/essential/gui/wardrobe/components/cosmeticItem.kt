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
package gg.essential.gui.wardrobe.components

import gg.essential.Essential
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.GradientComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.MousePositionConstraint
import gg.essential.elementa.dsl.basicXConstraint
import gg.essential.elementa.dsl.basicYConstraint
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.minus
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.plus
import gg.essential.elementa.dsl.provideDelegate
import gg.essential.elementa.events.UIClickEvent
import gg.essential.elementa.utils.withAlpha
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.CosmeticPreview
import gg.essential.gui.common.EssentialTooltip
import gg.essential.gui.common.SequenceAnimatedUIImage
import gg.essential.gui.common.bundleRenderPreview
import gg.essential.gui.common.modal.OpenLinkModal
import gg.essential.gui.common.onSetValueAndNow
import gg.essential.gui.common.outfitRenderPreview
import gg.essential.gui.common.skinRenderPreview
import gg.essential.gui.common.state
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.animateTransitions
import gg.essential.gui.elementa.state.v2.combinators.and
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.combinators.not
import gg.essential.gui.elementa.state.v2.combinators.or
import gg.essential.gui.elementa.state.v2.combinators.zip
import gg.essential.gui.elementa.state.v2.filter
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.set
import gg.essential.gui.elementa.state.v2.stateBy
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.elementa.state.v2.toV2
import gg.essential.gui.image.ImageFactory
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.util.Tag
import gg.essential.gui.wardrobe.EmoteWheelPage
import gg.essential.gui.wardrobe.Item
import gg.essential.gui.wardrobe.WardrobeCategory
import gg.essential.gui.wardrobe.WardrobeContainer
import gg.essential.gui.wardrobe.WardrobeState
import gg.essential.mod.cosmetics.CosmeticSlot
import gg.essential.mod.cosmetics.settings.CosmeticProperty
import gg.essential.mod.cosmetics.settings.CosmeticSetting
import gg.essential.model.util.toJavaColor
import gg.essential.network.connectionmanager.coins.CoinsManager
import gg.essential.network.connectionmanager.cosmetics.AssetLoader
import gg.essential.network.cosmetics.Cosmetic
import gg.essential.universal.UMouse
import gg.essential.universal.USound
import gg.essential.util.MinecraftUtils
import gg.essential.util.Multithreading
import gg.essential.util.UuidNameLookup
import gg.essential.util.findParentOfTypeOrNull
import gg.essential.gui.util.hoverScope
import gg.essential.gui.util.isInComponentTree
import gg.essential.gui.util.onAnimationFrame
import gg.essential.util.onRightClick
import gg.essential.gui.util.pollingState
import gg.essential.mod.cosmetics.database.LOCAL_PATH
import gg.essential.mod.cosmetics.settings.setting
import gg.essential.network.cosmetics.Cosmetic.Diagnostic
import gg.essential.util.thenAcceptOnMainThread
import gg.essential.util.toCosmeticOptionTime
import gg.essential.vigilance.utils.onLeftClick
import java.awt.Color
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class CosmeticItemTag(val item: Item) : Tag

fun LayoutScope.cosmeticItem(item: Item, category: WardrobeCategory, state: WardrobeState, modifier: Modifier) {

    val selected: State<Boolean>
    val editing: State<Boolean>
    val owned: State<Boolean>
    val pricingInfo = item.getPricingInfo(state)
    val outfitItems: State<List<Item.OutfitItem>>

    when (item) {
        is Item.CosmeticOrEmote -> {
            val slot = item.cosmetic.type.slot

            selected = memo {
                val selectedEmote = state.selectedEmote()
                val inEmoteWheel = state.inEmoteWheel()
                val isEmote = slot == CosmeticSlot.EMOTE
                when {
                    state.selectedBundle() != null -> false
                    isEmote && inEmoteWheel -> state.emoteWheel().contains(item.cosmetic.id)
                    selectedEmote != null -> selectedEmote.itemId == item.itemId
                    !isEmote && !inEmoteWheel -> state.equippedCosmeticsState()[slot] == item.cosmetic.id
                    else -> false
                }
            }

            editing = state.editingCosmetic.map { it?.cosmetic?.id == item.cosmetic.id }

            owned = state.unlockedCosmetics.map { item.cosmetic.id in it }

            outfitItems = stateOf(emptyList())
        }

        is Item.Bundle -> {
            selected = state.selectedBundle.map { it == item }
            editing = stateOf(false)
            owned = state.unlockedBundles.map { item.id in it }
            outfitItems = stateOf(emptyList())
        }

        is Item.SkinItem -> {
            selected = state.equippedOutfitItem.map { it?.skinId == item.id }
            editing = stateOf(false)
            owned = stateOf(false)
            outfitItems = state.outfitItems.filter { it.skinId == item.id  }
        }

        is Item.OutfitItem -> {
            selected = state.equippedOutfitItem.map { it?.id == item.id }
            editing = stateOf(false)
            owned = stateOf(false)
            outfitItems = stateOf(emptyList())
        }

        else -> {
            selected = stateOf(false)
            editing = stateOf(false)
            owned = stateOf(false)
            outfitItems = stateOf(emptyList())
        }
    }

    val sizeModifier: Modifier = if (item is Item.Bundle) {
        // Height = 198 Main + 2 Padding + 14 Text
        // Width = 190 Main + 2 Padding
        Modifier.height(214f).width(192f)
    } else {
        // Height = 90 Main + 2 Padding + 14 Text
        // Width = 90 Main + 2 Padding
        Modifier.height(108f).width(92f)
    }

    val outlineColor = stateBy {
        when {
            editing() -> EssentialPalette.TEXT_HIGHLIGHT
            selected() -> EssentialPalette.TEXT_MID_GRAY
            else -> EssentialPalette.GUI_BACKGROUND
        }
    }

    val doHighlight = state.highlightItem.map { it == item.itemId }
    val highlightModifier = Modifier.whenTrue(doHighlight, Modifier.animateColor(EssentialPalette.TEXT_HIGHLIGHT, 0.5f))
    doHighlight.onSetValueAndNow(stateScope) {
        if (it) {
            Multithreading.scheduleOnMainThread(
                { state.highlightItem.set(null) },
                1,
                TimeUnit.SECONDS,
            )
        }
    }

    val hasSideOption = mutableStateOf(false).apply {
        if (item is Item.CosmeticOrEmote) {
            state.cosmeticsManager.modelLoader.getModel(item.cosmetic, item.cosmetic.defaultVariantName, AssetLoader.Priority.High).thenAcceptOnMainThread {
                set(it.isContainsSideOption)
            }
        }
    }
    val hasEditButton = stateOf(item is Item.CosmeticOrEmote
        && (item.cosmetic.property<CosmeticProperty.Variants>() != null
            || item.cosmetic.property<CosmeticProperty.PositionRange>() != null
        )) or hasSideOption

    val background = Modifier.then {
        val hovered = hoverScope().toV2()
        val highlight = doHighlight
            .map { if (it) 1f else 0f }
            .animateTransitions(this, 0.5f)
        val hoverOrHighlight = memo { if (hovered()) 1f else highlight() }
        Modifier.gradient(
            top = memo { item.tier.barColor.withAlpha(0.05f + 0.05f * hoverOrHighlight()) },
            bottom = memo { item.tier.barColor.withAlpha(0.2f + 0.2f * hoverOrHighlight()) },
        ).applyToComponent(this)
    }

    box(sizeModifier.tag(CosmeticItemTag(item)).hoverScope().then(modifier)) {
        column(Modifier.fillParent()) {
            box(Modifier.fillRemainingHeight().fillWidth().color(outlineColor).then(highlightModifier)) {
                box(Modifier.fillParent(padding = 1f).color(EssentialPalette.GUI_BACKGROUND)) {
                    box(Modifier.fillRemainingHeight().fillWidth().alignVertical(Alignment.Start).then(background)) {
                        lazyBox {
                            when (item) {
                                is Item.Bundle -> bundleRenderPreview(state, item)
                                is Item.CosmeticOrEmote -> {
                                    // If the player doesn't have a variant setting configured, we use the override one if it exists
                                    // This is used to ensure featured page items initially show as they were configured
                                    val settings = state.selectedPreviewingEquippedSettings.map { settings ->
                                        val variantSetting = settings[item.cosmetic.id]?.setting<CosmeticSetting.Variant>()
                                            ?: item.settingsOverride?.setting<CosmeticSetting.Variant>()
                                        listOfNotNull(variantSetting)
                                    }
                                    CosmeticPreview(item.cosmetic, settings)(Modifier.fillParent())
                                }
                                is Item.OutfitItem -> outfitRenderPreview(state, item)
                                is Item.SkinItem -> skinRenderPreview(item)
                            }
                            box(Modifier.alignBoth(Alignment.Start)) {
                                row(Arrangement.spacedBy(1f)) {
                                    diagnosticIcons(item, category, state)
                                    newIcon(item)
                                    bind(outfitItems) { outfits ->
                                        if (outfits.isEmpty()) return@bind
                                        usedOnOutfitIcon(outfits)
                                    }
                                    salesTag(item, owned, pricingInfo)
                                    partnerIcons(item, state)
                                    lockedIcons(item, owned)
                                    cosmeticTimer(item, owned, state)
                                    unownedItemsOutfitIcon(item, state)
                                }
                            }
                            box(Modifier.alignVertical(Alignment.Start).alignHorizontal(Alignment.End)) {
                                row(Arrangement.spacedBy(1f)) {
                                    if_((containerDontUseThisUnlessYouReallyHaveTo.hoverScope().toV2() or editing) and hasEditButton) {
                                        editButton(item, category, state)
                                    }
                                    favoriteButton(item, state)
                                    infoIcon(item, state)
                                }
                            }
                            box(Modifier.alignVertical(Alignment.End).alignHorizontal(Alignment.Start)) {
                                row(Arrangement.spacedBy(1f)) {
                                    price(item, state, owned)
                                }
                            }
                            box(Modifier.alignBoth(Alignment.End)) {
                                row(Arrangement.spacedBy(1f)) {
                                    owned(item, owned)
                                }
                            }
                            if_(containerDontUseThisUnlessYouReallyHaveTo.hoverScope()) {
                                colorBar(item, category, state, selected)
                            }
                        }
                    }
                    box(Modifier.height(2f).fillWidth().color(item.tier.barColor).alignVertical(Alignment.End))
                }
            }

            box(Modifier.fillWidth().height(14f)) {
                box(Modifier.fillHeight().fillWidth(padding = 2f)) {
                    text(
                        item.name,
                        truncateIfTooSmall = true,
                        modifier = Modifier.color(EssentialPalette.TEXT).alignHorizontal(Alignment.Start),
                    )
                }
                if_(containerDontUseThisUnlessYouReallyHaveTo.hoverScope()) {
                    options(item, category, state)
                }
            }
        }
    }.onRightClick {
        state.handleItemRightClick(item, category, it)
    }.apply {
        // This has been copied from EmoteWheelPage, if something is changed here, you should probably change it there too.
        if (item is Item.CosmeticOrEmote && item.cosmetic.type.slot == CosmeticSlot.EMOTE) {
            fun LayoutScope.thumbnail(cosmetic: Cosmetic, modifier: Modifier): UIComponent {
                return CosmeticPreview(cosmetic)(modifier)
            }

            fun tooltip(parent: UIComponent, text: String) =
                EssentialTooltip(parent, position = EssentialTooltip.Position.RIGHT, notchSize = 0)
                    .constrain {
                        x = MousePositionConstraint() + 10.pixels
                        y = MousePositionConstraint() - 15.pixels
                    }
                    .addLine(text)

            fun isDraggable(): Boolean {
                return item.id in state.unlockedCosmetics.getUntracked()
            }

            var maybeDragging = false
            var clickStart = Pair(0f, 0f)
            val dragging = mutableStateOf(false)

            val draggable by lazy {
                val container = object : UIContainer() {
                    // when we hitTest, we want the thing below the dragging graphic
                    override fun isPointInside(x: Float, y: Float): Boolean = false

                    // `getMousePosition` is protected in UIComponent, so we have to expose it here
                    fun mousePosition() = getMousePosition()
                }

                container.layout(EmoteWheelPage.slotModifier) {
                    thumbnail(item.cosmetic, Modifier.fillParent())
                }

                container.onAnimationFrame {
                    val (mouseX, mouseY) = container.mousePosition()
                    val target = Window.of(container).hitTest(mouseX, mouseY)
                    val slotTarget = target as? EmoteWheelPage.EmoteSlot
                        ?: target.findParentOfTypeOrNull<EmoteWheelPage.EmoteSlot>()
                    val removeTarget = target.findParentOfTypeOrNull<WardrobeContainer>()
                    state.draggingOntoEmoteSlot.set(when {
                        slotTarget != null -> slotTarget.index
                        removeTarget != null -> -1
                        else -> null
                    })
                }

                val nameTooltip by tooltip(this, item.cosmetic.displayName)
                val replace by tooltip(container, "Replace")
                replace.bindVisibility(state.draggingOntoOccupiedEmoteSlot and dragging)
                nameTooltip.bindVisibility(!state.draggingOntoOccupiedEmoteSlot and dragging)

                container.onMouseRelease {
                    val target = state.draggingOntoEmoteSlot.get()
                    if (target != null && target != -1) {
                        if (item.cosmetic.isCosmeticFree && !owned.get()) {
                            claimFreeItemNow(item, state)
                        }

                        state.emoteWheel.set(target, item.cosmetic.id)
                        USound.playButtonPress()
                    }

                    state.draggingEmoteSlot.set(null)
                    state.draggingOntoEmoteSlot.set(null)

                    Window.enqueueRenderOperation {
                        hide(instantly = true)
                    }
                    maybeDragging = false
                    dragging.set(false)
                }

                container
            }

            onLeftClick {
                val xOffset = UMouse.Scaled.x.toFloat() - getLeft()
                val yOffset = UMouse.Scaled.y.toFloat() - getTop()

                clickStart = Pair(xOffset, yOffset)
                maybeDragging = true

                it.stopPropagation()
            }

            onMouseRelease {
                // The click was not a drag, call the click handler
                if (!dragging.get() && isHovered() && maybeDragging) {
                    handleCosmeticOrEmoteLeftClick(item, category, state)
                }
                maybeDragging = false
            }

            onMouseDrag { mouseX, mouseY, _ ->
                if (!maybeDragging) {
                    return@onMouseDrag
                }
                if (!isDraggable()) {
                    return@onMouseDrag
                }
                val distance = abs(clickStart.first - mouseX) + abs(clickStart.second - mouseY)
                if (distance > 5 && !dragging.get()) {

                    draggable.constrain {
                        x = MousePositionConstraint() - basicXConstraint {
                            draggable.getWidth() / 2
                        }
                        y = MousePositionConstraint() - basicYConstraint {
                            draggable.getHeight() / 2
                        }
                    }
                    Window.enqueueRenderOperation {
                        if (!draggable.isInComponentTree()) {
                            Window.of(this).addChild(draggable)
                        }
                        state.draggingEmoteSlot.set(-1)
                    }
                    dragging.set(true)
                }
            }
        } else {
            onLeftClick {
                state.handleItemLeftClick(item, category, it)
            }
        }
    }
}

private fun LayoutScope.textTag(text: String, modifier: Modifier = Modifier, textModifier: Modifier = Modifier) {
    box(Modifier.height(13f).childBasedWidth(3f).then(modifier)) {
        text(text, centeringContainsShadow = false, modifier = Modifier.shadow(EssentialPalette.BLACK_SHADOW).then(textModifier))
    }
}

private fun LayoutScope.iconButton(
    imageFactory: ImageFactory,
    modifier: Modifier = Modifier,
    iconModifier: Modifier,
    onLeftClick: (event: UIClickEvent) -> Unit = {}
) {
    box(modifier) {
        image(imageFactory, iconModifier)
    }.onLeftClick {
        onLeftClick(it)
    }
}

private fun LayoutScope.diagnosticIcons(item: Item, category: WardrobeCategory, state: WardrobeState) {
    if (item !is Item.CosmeticOrEmote || !state.diagnosticsEnabled || category is WardrobeCategory.FeaturedRefresh) {
        return
    }

    val diagnostics = item.cosmetic.diagnostics
    if (diagnostics == null) {
        iconButton(
            EssentialPalette.ROUND_WARNING_7X,
            Modifier.width(13f).height(13f).hoverTooltip("Loading..").hoverScope(),
            Modifier.color(EssentialPalette.WHITE)
        )
        return
    }
    if (diagnostics.isEmpty()) {
        return // no issues
    }

    val byType = Diagnostic.Type.entries.associateWith { type -> diagnostics.filter { it.type == type } }

    fun diagnosticIcon(diagnostics: List<Diagnostic>, color: Color) {
        if (diagnostics.isEmpty()) return

        val tooltip = diagnostics.joinToString("\n") { diagnostic ->
            listOfNotNull(
                diagnostic.skin?.let { "[${it.name.lowercase()}]" },
                diagnostic.variant?.let { "[$it]" },
                diagnostic.file?.let { file ->
                    val lineColumn = diagnostic.lineColumn
                    if (lineColumn != null) {
                        val (line, column) = lineColumn
                        "$file:$line:$column:"
                    } else {
                        "$file:"
                    }
                },
                diagnostic.message,
            ).joinToString(" ")
        }
        iconButton(
            EssentialPalette.ROUND_WARNING_7X,
            Modifier.width(13f).height(13f).hoverTooltip(tooltip).hoverScope(),
            Modifier.color(color),
            onLeftClick = { click ->
                click.stopPropagation()
                state.showingDiagnosticsFor.set(item.cosmetic.displayNames[LOCAL_PATH])
            },
        )
    }

    diagnosticIcon(byType.getValue(Diagnostic.Type.Fatal), Color.RED)
    diagnosticIcon(byType.getValue(Diagnostic.Type.Error), EssentialPalette.BANNER_RED)
    diagnosticIcon(byType.getValue(Diagnostic.Type.Warning), EssentialPalette.BANNER_YELLOW)
}

private fun LayoutScope.partnerIcons(item: Item, wardrobeState: WardrobeState) {

    // TODO: Note for Miha: Add namemc tag boolean here:
    // Reply: this was scrapped for now, but i'll leave this here if we end up adding this
    if (false) {
        textTag(
            "n",
            Modifier.width(13f).height(13f).color(Color.BLACK).hoverTooltip("Found on namemc.com").hoverScope(),
            Modifier.color(Color.WHITE).shadow(Color.BLACK).alignVertical(Alignment.Start(2f))
        )
    }

}

private fun LayoutScope.newIcon(item: Item) {
    if (item !is Item.CosmeticOrEmote || !item.cosmetic.isCosmeticNew) {
        return
    }

    textTag(
        "NEW",
        Modifier.color(EssentialPalette.BANNER_GREEN),
        Modifier.color(Color.BLACK).shadow(EssentialPalette.BLACK.withAlpha(0.25f))
    )
}

private fun LayoutScope.salesTag(item: Item, owned: State<Boolean>, pricingInfo: State<Item.PricingInfo?>) {
    if (item is Item.Bundle) {
        val coinSavings = memo {
            val pricing = pricingInfo() ?: return@memo null
            pricing.baseCost - pricing.realCost
        }

        bind(coinSavings) { savings ->
            if (savings != null && savings > 0) {
                val saveModifier = Modifier.shadow(EssentialPalette.BLACK.withAlpha(0.5f))
                box(Modifier.height(13f).childBasedWidth(6f).color(item.tier.saveTagColor)) {
                    row {
                        text("SAVE " + CoinsManager.COIN_FORMAT.format(savings), saveModifier, centeringContainsShadow = false)
                        spacer(width = 2f)
                        image(EssentialPalette.COIN_7X, saveModifier)
                        spacer(width = 1f)
                    }
                }
            }
        }
    } else {
        val salePercentageTagText = memo {
            val pricing = pricingInfo()
            val costInt = pricing?.realCost
            val discountPercentage = pricing?.discountPercentage
            if (costInt != null && costInt > 0 && !owned() && discountPercentage != null && discountPercentage > 0) {
                // Rounds down an integer percentage (0 to 100) to the nearest multiple of 5.
                // The only exclusions to this are 33 and 66, anything inclusive between 33 and 34, and 66 and 69 will
                // round down to 33 and 66.
                // Linear: EM-2426
                fun roundPercentageDown(percentage: Int): Int = when (percentage) {
                    33, 34 -> 33
                    in (66..69) -> 66
                    else -> 5 * (percentage / 5)
                }

                "${roundPercentageDown(discountPercentage)}%"
            } else {
                null
            }
        }

        ifNotNull(salePercentageTagText) { text ->
            textTag(text, Modifier.color(EssentialPalette.RED))
        }
    }
}

private fun LayoutScope.lockedIcons(item: Item, owned: State<Boolean>) {
    if (item !is Item.CosmeticOrEmote) {
        return
    }

    val data = item.cosmetic.property<CosmeticProperty.RequiresUnlockAction>()?.data ?: return

    if_(!owned) {
        iconButton(
            EssentialPalette.LOCK_7X9,
            Modifier.width(13f).height(13f).color(EssentialPalette.LOCKED_ORANGE).hoverTooltip(data.actionDescription).hoverScope(),
            Modifier.shadow(EssentialPalette.BLACK_SHADOW),
        )

        val actionModifier = Modifier.width(13f).height(13f).color(EssentialPalette.BANNER_BLUE).hoverScope()

        when (data) {
            is CosmeticProperty.RequiresUnlockAction.Data.JoinServer -> {
                iconButton(
                    EssentialPalette.JOIN_ARROW_5X,
                    actionModifier.hoverTooltip("Join ${data.serverAddress}"),
                    Modifier.shadow(EssentialPalette.BLACK_SHADOW),
                    onLeftClick = {
                        MinecraftUtils.connectToServer(data.serverAddress, data.serverAddress)
                        it.stopPropagation()
                    }
                )
            }

            is CosmeticProperty.RequiresUnlockAction.Data.OpenLink -> {
                iconButton(
                    EssentialPalette.JOIN_ARROW_5X,
                    actionModifier.hoverTooltip(data.linkShort),
                    Modifier.shadow(EssentialPalette.BLACK_SHADOW),
                    onLeftClick = {
                        OpenLinkModal.openUrl(URI(data.linkAddress))
                        it.stopPropagation()
                    }
                )
            }

            else -> {}
        }
    }
}

private fun LayoutScope.cosmeticTimer(item: Item, owned: State<Boolean>, wardrobeState: WardrobeState) {
    val availableUntilState = when (item) {
        is Item.CosmeticOrEmote -> owned.map { if (it) null else item.cosmetic.availableUntil }
        is Item.Bundle -> wardrobeState.featuredPageCollection.map { it?.availability?.until }
        else -> null
    } ?: return

    bind(availableUntilState) { availableUntil ->
        if (availableUntil == null) return@bind
        val saleTime = containerDontUseThisUnlessYouReallyHaveTo.pollingState { availableUntil.toCosmeticOptionTime() }
        box(Modifier.height(13f).widthAspect(1f).color(EssentialPalette.RED).hoverTooltip(saleTime).hoverScope()) {
            SequenceAnimatedUIImage(
                "/assets/essential/textures/studio/clock_", ".png",
                4,
                1000,
                TimeUnit.MILLISECONDS,
            )(Modifier.color(EssentialPalette.TEXT_HIGHLIGHT).shadow(EssentialPalette.BLACK_SHADOW))
        }
    }
}

private fun LayoutScope.usedOnOutfitIcon(outfits: List<Item.OutfitItem>) {
    iconButton(
        EssentialPalette.COSMETICS_10X7,
        Modifier
            .width(13f)
            .height(13f)
            .color(EssentialPalette.OUTFIT_TAG)
            .hoverTooltip(if (outfits.size == 1) "Used on outfit:\n${outfits.first().name}" else "Used on\nmultiple outfits")
            .hoverScope(),
        Modifier.color(EssentialPalette.TEXT_HIGHLIGHT).shadow(EssentialPalette.OUTFIT_TAG_SHADOW)
    ) {
        it.stopPropagation()
    }
}

private fun LayoutScope.unownedItemsOutfitIcon(item: Item, wardrobeState: WardrobeState) {
    if (item !is Item.OutfitItem) {
        return
    }

    val containsLockedItems = wardrobeState.unlockedCosmetics.map {
        !item.cosmetics.values.all { id -> it.contains(id) }
    }

    if_(containsLockedItems) {
        iconButton(
            EssentialPalette.SHOPPING_CART_8X7,
            Modifier.width(13f).height(13f).color(EssentialPalette.BANNER_BLUE)
                .hoverTooltip("Contains unowned items").hoverScope(),
            Modifier.shadow(EssentialPalette.BLACK_SHADOW),
        )
    }
}

private fun LayoutScope.editButton(item: Item, category: WardrobeCategory, wardrobeState: WardrobeState) {
    if (item !is Item.CosmeticOrEmote) {
        return
    }

    iconButton(
        EssentialPalette.PENCIL_7x7,
        Modifier.width(15f).height(15f).hoverScope(),
        Modifier.color(EssentialPalette.TEXT).hoverColor(EssentialPalette.TEXT_HIGHLIGHT).hoverTooltip("Edit".state()).shadow(EssentialPalette.TEXT_SHADOW)
    ) {
        it.stopPropagation()
        USound.playButtonPress()
        if (!wardrobeState.equippedCosmeticsState.get().values.contains(item.cosmetic.id)) {
            handleCosmeticOrEmoteLeftClick(item, category, wardrobeState)
        }
        if (wardrobeState.editingCosmetic.get() != item) {
            wardrobeState.selectedItem.set(item)
            wardrobeState.editingCosmetic.set(item)
        } else {
            wardrobeState.editingCosmetic.set(null)
        }
    }
}

private fun LayoutScope.favoriteButton(item: Item, wardrobeState: WardrobeState) {
    val isFavorite = item.isFavorite

    if (item !is Item.OutfitItem && item !is Item.SkinItem) {
        return
    }

    val favoriteColorObject = Color(0xFF9AE6)

    if_(containerDontUseThisUnlessYouReallyHaveTo.hoverScope().toV2() or stateOf(isFavorite)) {
        iconButton(
            EssentialPalette.HEART_FILLED_9X,
            Modifier.width(15f).height(15f).hoverTooltip(if (isFavorite) "Remove Favorite" else "Favorite").hoverScope(),
            Modifier.color(if (isFavorite) favoriteColorObject else EssentialPalette.TEXT)
                .hoverColor(if (isFavorite) favoriteColorObject else EssentialPalette.TEXT_HIGHLIGHT)
                .shadow(EssentialPalette.TEXT_SHADOW),
        ) {
            it.stopPropagation()
            wardrobeState.setFavorite(item, !isFavorite)
        }
    }
}

private fun LayoutScope.infoIcon(item: Item, wardrobeState: WardrobeState) {
    val infoTextState = when (item) {
        is Item.Bundle -> {
            wardrobeState.unlockedCosmetics.map { unlocked ->
                val owned = item.cosmetics.values.count { it in unlocked }
                if (owned > 0 && owned < item.cosmetics.size) {
                    "Owned items not included in price"
                } else {
                    ""
                }
            }
        }

        else -> return
    }

    if_(infoTextState.map { it != "" }) {
        textTag(
            "i",
            Modifier.width(13f).height(13f).color(EssentialPalette.BANNER_BLUE)
                .hoverTooltip(infoTextState.toV1(stateScope)).hoverScope()
        )
    }
}

private fun LayoutScope.price(item: Item, wardrobeState: WardrobeState, owned: State<Boolean>) {
    if (!item.isPurchasable) {
        return
    }
    val pricingInfo = item.getPricingInfo(wardrobeState)

    if_(!owned) {
        bind(pricingInfo) { pricing ->
            val previousPrice = pricing?.baseCost
            val price = pricing?.realCost
            column(horizontalAlignment = Alignment.Start) {
                if (previousPrice != null && previousPrice != price) {
                    box(Modifier.childBasedMaxHeight(padding = 1f).childBasedMaxWidth(padding = 2f)) {
                        val text = text(
                            CoinsManager.COIN_FORMAT.format(previousPrice),
                            Modifier.color(EssentialPalette.TEXT_DARK_DISABLED)
                        )
                        box(
                            Modifier.height(1f).width(text.getTextWidth() + 1f)
                                .color(EssentialPalette.TEXT_DARK_DISABLED).shadow()
                        )
                    }
                }
                if (price != null) {
                    row {
                        box(Modifier.childBasedHeight(padding = 1f).childBasedWidth(padding = 2f)) {
                            text(
                                if (price > 0) CoinsManager.COIN_FORMAT.format(price) else "FREE",
                                Modifier.color(EssentialPalette.TEXT_MID_GRAY).shadow(EssentialPalette.GUI_BACKGROUND)
                            )
                        }
                        if (price > 0) {
                            icon(EssentialPalette.COIN_7X)
                        }
                    }
                }
            }
        }
    }
}

private fun LayoutScope.owned(item: Item, owned: State<Boolean>) {
    if (item !is Item.CosmeticOrEmote && item !is Item.Bundle) {
        return
    }

    val isLegacy = item is Item.CosmeticOrEmote && item.cosmetic.isLegacy
    val giftedByUuidState = if (item is Item.CosmeticOrEmote) {
        Essential.getInstance().connectionManager.cosmeticsManager.unlockedCosmeticsData.map {
            it[item.cosmetic.id]?.giftedBy
        }
    } else {
        stateOf(null)
    }
    val giftedByNameState = stateBy {
        val uuid = giftedByUuidState()
        if (uuid != null) {
            UuidNameLookup.nameState(uuid)()
        } else {
            null
        }
    }
    val tooltip = stateBy {
        val giftedName = giftedByNameState()
        if (isLegacy) {
            "Owned (Legacy)"
        } else if (giftedName != null) {
            "Gifted by $giftedName"
        } else {
            "Owned"
        }
    }

    if_(owned) {
        iconButton(
            EssentialPalette.CHECKMARK_7X5,
            Modifier.width(17f).height(13f).hoverScope(),
            Modifier.color(if (isLegacy) EssentialPalette.LEGACY_ICON_YELLOW else EssentialPalette.GREEN)
                .hoverTooltip(tooltip, padding = 6f)
                .shadow(EssentialPalette.TEXT_SHADOW)
        )
    }
}

private fun LayoutScope.colorBar(item: Item, category: WardrobeCategory, wardrobeState: WardrobeState, isItemSelected: State<Boolean>) {
    if (item !is Item.CosmeticOrEmote) {
        return
    }

    val variants = item.cosmetic.property<CosmeticProperty.Variants>()?.data?.variants ?: return

    // If the user doesn't have a variant selected yet, use the variant from the overrides if they have one, otherwise default
    // This is used to ensure featured page items show as configured initially, before the user manually sets a color
    val selected = wardrobeState.getVariant(item).map { variant ->
        variant ?: (item.settingsOverride?.setting<CosmeticSetting.Variant>() ?: item.cosmetic.defaultVariantSetting)?.data?.variant
    }

    column(Modifier.alignHorizontal(Alignment.End(2f))) {
        for (variant in variants) {
            val outlineColor = selected.map {
                if (it == variant.name) {
                    if (variant.color == gg.essential.model.util.Color.WHITE) {
                        EssentialPalette.TEXT
                    } else {
                        EssentialPalette.TEXT_HIGHLIGHT
                    }
                } else {
                    variant.color.toJavaColor()
                }
            }
            val hoverOutlineColor = selected.zip(outlineColor).map { (selected, outlineColor) ->
                if (selected != variant.name) {
                    EssentialPalette.TEXT_MID_GRAY
                } else {
                    outlineColor
                }
            }
            box(Modifier.height(12f).width(12f).hoverScope()) {
                box(Modifier.fillParent(padding = 1f).color(outlineColor).hoverColor(hoverOutlineColor).shadow()) {
                    box(Modifier.fillParent(padding = 1f).color(variant.color.toJavaColor()))
                }
            }.onLeftClick { click ->
                click.stopPropagation()
                USound.playButtonPress()
                if (!isItemSelected.get()) {
                    handleCosmeticOrEmoteLeftClick(item, category, wardrobeState)
                }
                wardrobeState.setVariant(item, variant.name)
            }.hoverScope().onSetValue { hovered ->
                handleVariantHover(variant, item, wardrobeState, hovered)
            }
        }
    }
}

private fun LayoutScope.options(item: Item, category: WardrobeCategory, wardrobeState: WardrobeState) {
    if_(wardrobeState.hasOptionsButton(item)) {
        row(Modifier.fillHeight().alignHorizontal(Alignment.End)) {
            GradientComponent(
                EssentialPalette.GUI_BACKGROUND.withAlpha(0),
                EssentialPalette.GUI_BACKGROUND,
                GradientComponent.GradientDirection.LEFT_TO_RIGHT
            )(Modifier.fillHeight().width(18f))
            box(Modifier.fillHeight().width(12f).color(EssentialPalette.GUI_BACKGROUND))
        }
        iconButton(
            EssentialPalette.OPTIONS_8X2,
            Modifier.fillHeight().width(15f).alignHorizontal(Alignment.End).hoverScope(),
            Modifier.color(EssentialPalette.TEXT).hoverTooltip("Options").hoverColor(EssentialPalette.TEXT_HIGHLIGHT).shadow(),
            onLeftClick = {
                wardrobeState.handleItemRightClick(item, category, it)
                it.stopPropagation()
            }
        )
    }
}
