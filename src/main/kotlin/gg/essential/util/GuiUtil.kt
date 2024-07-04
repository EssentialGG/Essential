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
package gg.essential.util

import gg.essential.Essential
import gg.essential.api.gui.GuiRequiresTOS
import gg.essential.api.utils.GuiUtil
import gg.essential.connectionmanager.common.packet.telemetry.ClientTelemetryPacket
import gg.essential.data.OnboardingData
import gg.essential.elementa.WindowScreen
import gg.essential.event.client.ClientTickEvent
import gg.essential.gui.common.modal.Modal
import gg.essential.gui.friends.SocialMenu
import gg.essential.gui.modals.*
import gg.essential.gui.notification.sendTosNotification
import gg.essential.gui.overlay.ModalManager
import gg.essential.gui.overlay.ModalManagerImpl
import gg.essential.gui.overlay.OverlayManager
import gg.essential.gui.overlay.OverlayManagerImpl
import gg.essential.gui.wardrobe.Wardrobe
import gg.essential.universal.GuiScale
import gg.essential.universal.UMinecraft
import gg.essential.universal.UScreen
import me.kbrewster.eventbus.Subscribe
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.resources.I18n
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

//#if MC>=11600
//$$ import gg.essential.universal.wrappers.message.UTextComponent
//#endif

object GuiUtil : GuiUtil, OverlayManager by OverlayManagerImpl, ModalManager by ModalManagerImpl(OverlayManagerImpl) {
    private var display: (() -> GuiScreen?)? = null

    /**
     * Creates a new [ModalManager] and queues the modal on it (which will result in it being pushed immediately).
     *
     * Returns the newly created [ModalManager], which can be used to queue further modals if needed.
     *
     * If the modal does not need immediate attention, consider using [queueModal] instead, as to not
     * interrupt the user in their current modal.
     */
    @OptIn(ExperimentalContracts::class)
    fun pushModal(builder: (ModalManager) -> Modal): ModalManager {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }

        // If this modal needs to be pushed immediately, we will create a new modal manager for it.
        val manager: ModalManager = ModalManagerImpl(this)
        manager.queueModal(builder(manager))

        return manager
    }

    inline fun <reified T : GuiScreen> openScreen(noinline screen: () -> T?) {
        // Guard against T not being inferred well enough. If there's a compile-time way to do this please tell me.
        when (T::class.java) {
            WindowScreen::class.java,
            UScreen::class.java,
            GuiScreen::class.java -> {
                val cls = T::class.java
                throw IllegalArgumentException("Failed to infer concrete screen type, got generic type $cls instead." +
                        "If this was intentional, use `openScreen(${cls.simpleName}::class.java, () -> T?)` instead.")
            }
        }
        openScreen(T::class.java, screen)
    }

    @JvmStatic
    fun <T : GuiScreen> openScreen(type: Class<T>, screen: () -> T?) {
        val essential = Essential.getInstance()
        val connectionManager = essential.connectionManager
        val screenRequiresTOS = GuiRequiresTOS::class.java.isAssignableFrom(type)
        val screenRequiresCosmetics = type == Wardrobe::class.java
        val screenRequiresAuth = screenRequiresCosmetics || type == SocialMenu::class.java

        if (screenRequiresTOS && !OnboardingData.hasAcceptedTos()) {
            fun showTOS() = pushModal {
                TOSModal(it, unprompted = false, requiresAuth = screenRequiresAuth, { openScreen(type, screen) })
            }
            if (openedScreen() == null) {
                // Show a notification when we're not in any menu, so it's less intrusive
                sendTosNotification { showTOS() }
            } else {
                showTOS()
            }
            return
        }

        if (screenRequiresAuth && AutoUpdate.requiresUpdate()) {
            // Essential outdated, require update first
            pushModal { AutoUpdate.createUpdateModal(it) }
            return
        }

        if (screenRequiresAuth && !connectionManager.isAuthenticated) {
            pushModal { NotAuthenticatedModal(it) { openScreen(type, screen) } }
            return
        }

        if (screenRequiresCosmetics && !connectionManager.cosmeticsManager.cosmeticsLoadedFuture.isDone) {
            pushModal { CosmeticsLoadingModal(it) { openScreen(type, screen) } }
            return
        }

        doOpenScreen(screen)
    }

    @Deprecated("For API users only. Does not check for TOS or similar, use the generic overload instead.", level = DeprecationLevel.ERROR)
    override fun openScreen(screen: GuiScreen?) {
        // This interface is part of our API, so we need to check for it in this method as well
        if (screen is GuiRequiresTOS) {
            openScreen(screen.javaClass) { screen }
        }

        doOpenScreen { screen }
    }

    private fun doOpenScreen(screen: () -> GuiScreen?) {
        display = { screen()?.also { screen ->
            Essential.getInstance().connectionManager.telemetryManager.enqueue(
                ClientTelemetryPacket(
                    "GUI_OPEN",
                    mapOf(Pair("name", screen.javaClass.name))
                )
            )
        } }
    }

    override fun openedScreen(): GuiScreen? {
        return UMinecraft.getMinecraft().currentScreen
    }

    fun getScreenName(screen: GuiScreen): String {
        (screen as? UScreen)?.unlocalizedName?.let { return I18n.format(it) }

        //#if MC>=11600
        //$$ screen.title?.let { return UTextComponent(it).unformattedText }
        //#endif

        return screen.javaClass.simpleName
    }

    override fun getGuiScale(): Int {
        return getGuiScale(650)
    }

    override fun getGuiScale(step: Int): Int {
        return GuiScale.scaleForScreenSize(step).ordinal
    }

    @Subscribe
    fun tick(event: ClientTickEvent?) {
        display?.also { factory ->
            val screen = factory()
            if (screen != null)
                UMinecraft.getMinecraft().displayGuiScreen(screen)
            display = null
            return
        }
    }
}
