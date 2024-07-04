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
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.state.State
import gg.essential.elementa.utils.ObservableList
import gg.essential.gui.EssentialPalette
import gg.essential.gui.account.factory.InitialSessionFactory
import gg.essential.gui.account.factory.ManagedSessionFactory
import gg.essential.gui.common.modal.DangerConfirmationEssentialModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.common.onSetValueAndNow
import gg.essential.gui.elementa.state.v2.ReferenceHolderImpl
import gg.essential.gui.menu.compact.CompactAccountSwitcher
import gg.essential.gui.menu.full.FullAccountSwitcher
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.error
import gg.essential.gui.notification.iconAndMarkdownBody
import gg.essential.handlers.account.WebAccountManager
import gg.essential.universal.UMinecraft
import gg.essential.util.GuiUtil
import gg.essential.util.USession
import gg.essential.util.colored
import gg.essential.util.executor
import gg.essential.util.setSession
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.CompletableFuture

class AccountManager {

    private val accountsList = ObservableList(mutableListOf<AccountInfo>())
    private val referenceHolder = ReferenceHolderImpl()

    fun getFullAccountSwitcher(collapsed: State<Boolean>) = FullAccountSwitcher(accountsList, collapsed, this)

    fun getCompactAccountSwitcher(sidebarContainer: UIContainer) = CompactAccountSwitcher(accountsList, sidebarContainer, this)

    init {
        USession.active.onSetValueAndNow(referenceHolder) {
            refreshAccounts()
        }
        WebAccountManager.mostRecentAccountManager = WeakReference(this)
    }

    private fun refreshAccounts() {
        accountsList.clear()

        // List all accounts except the currently active one, keeping track of what's added to avoid duplicates
        Essential.getInstance().sessionFactories.forEach { sessionFactory ->
            sessionFactory.sessions.filterKeys { uuid ->
                accountsList.none { it.uuid == uuid } && uuid != USession.active.get().uuid
            }.forEach { accountsList.add(AccountInfo(it.key, it.value.username)) }
        }
    }

    /**
     * Logs in with the specified account's [uuid].
     * This will refresh the session and re-establish the connection.
     */
    fun login(uuid: UUID) {
        if (USession.active.get().uuid != uuid) {
            refreshSession(uuid) { session, error ->
                if (error == null) {
                    Notifications.push("", "", 1f) {
                        iconAndMarkdownBody(
                            EssentialPalette.EMOTES_7X.create(),
                            "Logged in as ${session.username.colored(EssentialPalette.TEXT_HIGHLIGHT)}"
                        )
                    }
                } else {
                    Essential.logger.error("Account Error: $error")
                    Notifications.error("Account Error", "Something went wrong\nduring login.")
                }
                refreshAccounts()
            }
        }
    }

    /** Display a modal prompting the user to confirm if they want to remove the account which [uuid] belongs to */
    fun promptRemove(uuid: UUID, name: String) {
        GuiUtil.pushModal { manager -> 
            DangerConfirmationEssentialModal(manager, "Remove", requiresButtonPress = false).configure {
                contentText = "Are you sure you want to remove the account $name?"
            }.onPrimaryAction {
                removeAccount(uuid)
            }
        }
    }

    /** Removes the account which [uuid] belongs to from the [AccountManager] */
    fun removeAccount(uuid: UUID) {
        Essential.getInstance().sessionFactories.filterIsInstance<ManagedSessionFactory>().forEach { it.remove(uuid) }
        refreshAccounts()
    }

    companion object {
        /** Refresh the current session with the option to [force] refresh and specify an optional [callback]. */
        @JvmStatic
        @JvmOverloads
        fun refreshCurrentSession(
            force: Boolean = false,
            callback: ((session: USession, error: String?) -> Unit)? = null
        ) {
            refreshSession(USession.activeNow().uuid, force, callback)
        }

        /**
         * Refresh a specific session belonging to [uuid] with the option to [force] refresh and specify a [callback].
         *
         * [callback] receives the refreshed session if the session was refreshed or the current session if the session
         * was unable to be refreshed, as well as the error if a valid session could not be found or an error occurred
         * while refreshing the session.
         */
        @JvmStatic
        @JvmOverloads
        fun refreshSession(
            uuid: UUID,
            force: Boolean = false,
            callback: ((session: USession, error: String?) -> Unit)? = null
        ) {
            val mc = UMinecraft.getMinecraft()

            fun error(message: String, throwable: Throwable? = null) {
                Essential.logger.error(message, throwable)
                callback?.invoke(USession.activeNow(), message)
            }

            // Check if UUID is in the managed session factory first
            val factory =
                Essential.getInstance().sessionFactories.find { uuid in it.sessions } as? ManagedSessionFactory
            if (factory != null) {
                // If so, then refresh the session
                CompletableFuture.supplyAsync { factory.refresh(factory.sessions[uuid]!!, force) }
                    .whenCompleteAsync({ session, err ->
                        if (session != null) {
                            Essential.logger.info("Successfully refreshed session token.")
                            UMinecraft.getMinecraft().setSession(session)
                            callback?.invoke(session, null)
                        } else {
                            err?.let {
                                error("Failed to refresh session: ${it.message}", it)
                            }
                        }
                    }, mc.executor)
            } else {
                // Otherwise, check if it's in the initial session, which we can simply activate
                val initialSession =
                    Essential.getInstance().sessionFactories.find { uuid in it.sessions } as? InitialSessionFactory
                        ?: return error("Failed to refresh session: Unknown account")
                UMinecraft.getMinecraft().setSession(initialSession.sessions[uuid]!!)
                callback?.invoke(initialSession.sessions[uuid]!!, null)
            }
        }
    }

    data class AccountInfo(val uuid: UUID, val name: String)
}
