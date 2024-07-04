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
package gg.essential.gui.menu

import gg.essential.Essential
import gg.essential.config.EssentialConfig
import gg.essential.connectionmanager.common.packet.telemetry.ClientTelemetryPacket
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.*
import gg.essential.gui.elementa.state.v2.combinators.and
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.combinators.not
import gg.essential.gui.elementa.state.v2.combinators.or
import gg.essential.gui.elementa.state.v2.combinators.zip
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.wardrobe.Wardrobe
import gg.essential.gui.wardrobe.WardrobeCategory
import gg.essential.handlers.PauseMenuDisplay
import gg.essential.handlers.PauseMenuDisplay.Companion.window
import gg.essential.util.GuiUtil
import gg.essential.util.bindEssentialTooltip
import gg.essential.gui.util.hoveredState
import gg.essential.gui.util.pollingStateV2
import gg.essential.util.toShortString
import gg.essential.vigilance.utils.onLeftClick
import java.time.Duration
import java.time.Instant
import gg.essential.gui.elementa.state.v2.State as StateV2

class LeftSideBar(
    private val topButton: UIContainer,
    private val bottomButton: UIContainer,
    menuVisible: StateV2<Boolean>,
    collapsed: StateV2<Boolean>,
    isCompact: StateV2<Boolean>,
    menuType: PauseMenuDisplay.MenuType,
    rightSideBar: UIContainer,
    leftContainer: UIContainer,
    accountManager: AccountManager,
) : UIContainer() {

    private val connectionManager = Essential.getInstance().connectionManager

    // As a field so that there is a strong reference to it. Otherwise, it will be GC'd
    private val allSales = connectionManager.noticesManager.saleNoticeManager.saleState

    private val currentSale = pollingStateV2 {
        // When the menu is collapsed, the display banner only shows for real sales
        // and does not display the 'fake' sales used as messages unless they privide
        // a compact name, so we must exclude them to avoid the banner cycling between 'sale' and nothing.
        val sales = allSales.get().filter { (!collapsed.get() || it.compactName != null) || it.discountPercent > 0 }.toList()
        return@pollingStateV2 if (sales.isEmpty()) {
            null
        } else {
            val cycleTime = SALE_BANNER_CYCLE_TIME_MS * sales.size
            sales[(System.currentTimeMillis() % cycleTime / SALE_BANNER_CYCLE_TIME_MS).toInt()]
        }
    }
    private val isSale = currentSale.map { it != null }
    private val isFakeSale = currentSale.map { it?.discountPercent == 0 } // 0% off sales are used to show a notice without a sale


    private val menuSize = collapsed.zip(isCompact)

    // Account switcher
    private val accountSwitcher = accountManager.getFullAccountSwitcher(collapsed.toV1(this)).constrain {
        y = 0.pixels boundTo rightSideBar
    }.bindParent(this, !isCompact and stateOf(menuType == PauseMenuDisplay.MenuType.MAIN), delayed = true)

    private val playerWardrobeContainer by UIContainer().constrain {
        x = CenterConstraint()
        width = ChildBasedMaxSizeConstraint()
        height = ChildBasedSizeConstraint()
    }.bindConstraints(isCompact) { compact ->
        y = if (compact) {
            (CenterConstraint() boundTo rightSideBar).coerceAtLeast(0.pixels(alignOpposite = true) boundTo bottomButton)
        } else {
            0.pixels(alignOpposite = true) boundTo rightSideBar
        }
    } childOf this

    private val player by EmulatedUI3DPlayer().constrain {
        // x set in init
        width = AspectConstraint()
    }.bindConstraints(collapsed) { isCollapsed ->
        height = if (isCollapsed) 110.pixels else 120.pixels
    } childOf playerWardrobeContainer

    private val wardrobeButton by MenuButton(BasicState("Wardrobe"), textAlignment = MenuButton.Alignment.LEFT) {
        openWardrobe()
    }.setIcon(
        BasicState(EssentialPalette.COSMETICS_10X7),
        rightAligned = true,
        iconWidth = 10f,
        iconHeight = 7f,
    ).constrain {
        width = 80.pixels
        height = 20.pixels
    }.bindConstraints(menuSize) { (collapsed, isCompact) ->
        if (isCompact) {
            x = CenterConstraint()
            y = SiblingConstraint(9f)
        } else if (collapsed) {
            x = CenterConstraint()
            y = SiblingConstraint(10f)
        } else {
            x = 0.pixels boundTo this@LeftSideBar
            y = SiblingConstraint(30f)
        }
    }.setTooltip(menuSize.map { (isCompact, collapsed) -> if (isCompact || collapsed) "Wardrobe" else "" }.toV1(this))
        .bindCollapsed((collapsed or isCompact).toV1(this), 20f) childOf playerWardrobeContainer

    init {
        bindConstraints(isCompact) { compact ->
            if (compact) {
                x = if (EssentialConfig.closerMenuSidebar) {
                    basicXConstraint { (topButton.getLeft() / 2) - (it.getWidth() / 2) }.coerceIn(
                        basicXConstraint { topButton.getLeft() - (player.getWidth() / 2) - (wardrobeButton.getWidth() / 2) - PauseMenuDisplay.maxSpaceBetweenSides },
                        17.pixels(alignOutside = true) boundTo topButton,
                    )
                } else {
                    basicXConstraint { (topButton.getLeft() / 2) - (it.getWidth() / 2) }
                        .coerceAtMost(17.pixels(alignOutside = true) boundTo topButton)
                }
                y = 0.pixels boundTo player
                width = 100.percent boundTo player
                height = ChildBasedSizeConstraint()
            } else {
                // Maintain the same margin from the window's edge as the right sidebar
                x = ((SiblingConstraint() boundTo window) - (SiblingConstraint() boundTo rightSideBar))
                y = 0.pixels
                width = 100.percent boundTo accountSwitcher
                height = 100.percent
            }
        }

        player.constrain { x = CenterConstraint() boundTo wardrobeButton }

        val saleName = currentSale.map {
            (it?.name?.uppercase() ?: "")
        }
        val saleExpires = pollingStateV2 {
            val sale = currentSale.get() ?: return@pollingStateV2 ""
            val timeLeft = Duration.between(Instant.now(), sale.expiration)
            "${timeLeft.toShortString(false)} left"
        }

        val saleLines = currentSale.map {
            if (it?.displayRemainingTimeOnBanner == true) {
                listOf(saleName, saleExpires)
            } else {
                listOf(saleName)
            }
        }
        // Big sale flag
        TextFlag(
            isCompact.zip(isFakeSale).map { (compact, fakeSale) ->
                val baseStyle = if (fakeSale) MenuButton.NOTICE_GREEN else MenuButton.LIGHT_RED
                if (compact) {
                    baseStyle
                } else {
                    baseStyle.copy(sides = setOf(
                        OutlineEffect.Side.Top,
                        OutlineEffect.Side.Left,
                        OutlineEffect.Side.Right
                    ))
                }
            },
            text = saleLines,
        ).constrain {
            x = CenterConstraint() boundTo wardrobeButton
            height += 1.pixel
        }.bindConstraints(isCompact) { compact ->
            if (compact) {
                y = SiblingConstraint(3f) boundTo wardrobeButton
                width = width.coerceIn(72.pixels, 78.pixels)
            } else {
                y = SiblingConstraint(alignOpposite = true) boundTo wardrobeButton
                width = (100.percent boundTo wardrobeButton) - 2.pixels
            }
        }.onLeftClick {
            openWardrobe(WardrobeCategory.get(currentSale.get()?.category))
        }.bindParent(this, isSale and !collapsed and menuVisible).apply {
            bindEssentialTooltip(hoveredState() and currentSale.map { it?.tooltip != null }.toV1(this), currentSale.map { it?.tooltip ?: ""}.toV1(this), EssentialTooltip.Position.ABOVE)
        }

        val collapsedSaleName = currentSale.map {
            (it?.compactName?.uppercase() ?: "")
        }

        val smallSaleLines = currentSale.map {
            listOf(collapsedSaleName)
        }
        // Small sale flag
        TextFlag(
            isFakeSale.map {
                if (it) {
                    MenuButton.NOTICE_GREEN
                } else {
                    MenuButton.LIGHT_RED
                }
            },
            text = smallSaleLines,
        ).constrain {
            x = CenterConstraint() boundTo wardrobeButton
            y = 3.pixels(alignOpposite = true, alignOutside = true) boundTo wardrobeButton
        }.onLeftClick { openWardrobe(WardrobeCategory.get(currentSale.get()?.category)) }
            .bindParent(leftContainer, isSale and collapsed and menuVisible, delayed = true)
            .apply {
                bindEssentialTooltip(hoveredState() and currentSale.map { it?.tooltip != null }.toV1(this), currentSale.map { it?.tooltip ?: ""}.toV1(this), EssentialTooltip.Position.ABOVE)
            }

        val hasAnyNewCosmetics = connectionManager.cosmeticsManager.cosmetics.map { it.any { it.isCosmeticNew } }

        // New cosmetics flag
        TextFlag(
            stateOf(MenuButton.NOTICE_GREEN),
            MenuButton.Alignment.CENTER,
            stateOf("NEW"),
        ).bindConstraints(isCompact) { compact ->
            if (compact) {
                y = CenterConstraint() boundTo wardrobeButton
                x = 3.pixels(alignOpposite = true, alignOutside = true) boundTo wardrobeButton
            } else {
                x = 3.pixels(alignOpposite = true, alignOutside = true) boundTo wardrobeButton
                y = CenterConstraint() boundTo wardrobeButton
            }
        }.onLeftClick { wardrobeButton.runAction() }.bindParent(
            leftContainer,
            hasAnyNewCosmetics and menuVisible,
            delayed = true,
        )
    }

    private fun openWardrobe(category: WardrobeCategory? = null) {
        GuiUtil.openScreen { Wardrobe(category) }
    }

    override fun animationFrame() {
        super.animationFrame()
        val currentSale = currentSale.get() ?: return
        if (bannerImpressions.add(currentSale.name)) {
            connectionManager.telemetryManager.enqueue(ClientTelemetryPacket("COSMETICS_SALE_BANNER_IMPRESSION", mapOf(
                "sale_name" to currentSale.name,
            )))
        }
    }

    companion object {
        const val SALE_BANNER_CYCLE_TIME_MS = 3000L
        private val bannerImpressions = mutableSetOf<String>() // Name of banners that have been shown to the user
    }
}
