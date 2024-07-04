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
package gg.essential.handlers.account

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import gg.essential.Essential
import gg.essential.config.LoadsResources
import gg.essential.gui.account.factory.MicrosoftAccountSessionFactory
import gg.essential.gui.menu.AccountManager
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.error
import gg.essential.universal.UDesktop
import gg.essential.util.Multithreading
import java.lang.ref.WeakReference
import java.net.InetSocketAddress
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern

/**
 * This utility object handles the webserver for serving Essential web pages
 * and receiving the response from the Microsoft oauth process
 */
object WebAccountManager {
    private var running = false
    private var port = 0

    private var domain = "https://essential.gg"
    private val microsoftOauthParamPattern = Pattern.compile("code=(?<code>.+)")
    private var oauthUriFuture: CompletableFuture<URI>? = null

    var microsoftRedirectUri = "https://essential.gg"
        private set
    var authorizationCodeFuture: CompletableFuture<String>? = null
        private set
    var mostRecentAccountManager: WeakReference<AccountManager?> = WeakReference(null)

    private var loginErrorFuture: CompletableFuture<String> = CompletableFuture()

    /**
     * Opens the browser to the landing page of the account system
     * Starts the web server it is not already running
     */
    fun openInBrowser() {
        if (!running) {
            startServer()
            running = true
        }
        authorizationCodeFuture = CompletableFuture<String>()
        UDesktop.browse(URI("http://localhost:$port"))
    }

    /**
     * Sends the contents of the specified path to the complete the web request
     */
    private fun send(exchange: HttpExchange, path: String, data: Map<String, String> = mapOf()) {
        val resource = javaClass.getResourceAsStream(path)
        if (resource == null) {
            exchange.sendResponseHeaders(500, 0)
            exchange.responseBody.write("Internal server error. Contact support".toByteArray(Charsets.UTF_8))
            exchange.responseBody.close()
            return
        }

        // Don't parse to a string as that will cause an incorrect interpretation
        if (path.endsWith("png")) {
            val content = resource.use { resource.readBytes() }
            exchange.responseHeaders.add("Content-Type", "image/png")
            exchange.sendResponseHeaders(200, content.size.toLong())
            exchange.responseBody.write(content)
            exchange.responseBody.close()
            return
        }
        var content = String(resource.use { resource.readBytes() })
        for (key in data.keys) {
            content = content.replace("{$key}", "${data[key]}")
        }

        exchange.sendResponseHeaders(200, content.toByteArray().size.toLong())
        exchange.responseBody.write(content.toByteArray())
        exchange.responseBody.close()
    }

    /**
     * Starts the server and registers the routes
     */
    @LoadsResources("/assets/essential/account/.+")
    private fun startServer() {
        val server = HttpServer.create(InetSocketAddress("localhost", 0), 0)
        authorizationCodeFuture = CompletableFuture<String>()

        registerAssets(server)
        registerPages(server)

        registerMicrosoftAccountListener(server)

        server.executor = Multithreading.pool
        server.start()


        port = server.address.port
        domain = "http://localhost:$port"

        microsoftRedirectUri = "$domain/microsoft/complete"
    }

    /**
     * Registers the routes for handling Microsoft accounts
     */
    private fun registerMicrosoftAccountListener(server: HttpServer) {

        // Redirect the user to start the oauth process
        // The location is dependent on the future we receive from
        // the MicrosoftUserAuthentication handler
        server.createContext("/microsoft/auth") {
            val future = oauthUriFuture
            if (future == null) {
                println("Future not initialized")
                send(it, "/assets/essential/account/error.html", mapOf("error" to "Internal error please check logs"))
                return@createContext
            }
            it.responseHeaders.add("Location", future.join().toString())
            it.sendResponseHeaders(302, 0)
            it.responseBody.close()
        }


        // Users are redirected here after signing in to their account on live.com
        server.createContext("/microsoft/complete") { exchange ->

            // Extract the code and complete the future with it
            val query = exchange.requestURI.query
            if (query != null) {
                val matcher = microsoftOauthParamPattern.matcher(query)
                if (matcher.find()) {
                    authorizationCodeFuture?.complete(matcher.group("code")).also {
                        // Forcefully unset so that MicrosoftUserAuthentication does not try to use it again
                        authorizationCodeFuture = null
                    } ?: run {
                        println("authorizationCodeFuture == null. Perhaps the user had an extra account frame open?")
                        send(
                            exchange,
                            "/assets/essential/account/error.html",
                            mapOf("error" to "Error during login. Please close browser and try again.")
                        )
                        return@createContext
                    }
                }
            }

            // Wait for the account handler to finish logging in and see if any error occurred
            val error = loginErrorFuture.join()
            if (error != null) {
                send(exchange, "/assets/essential/account/error.html", mapOf("error" to error))
            } else {
                exchange.responseHeaders.add("Location", "/login/success")
                exchange.sendResponseHeaders(302, 0)
                exchange.responseBody.close()
            }


        }

        // Serve the Microsoft login page and begin the login process
        server.createContext("/login/microsoft") { exchange ->

            // Create a new future to allow retries with a fresh URI
            val oauthUriFuture = CompletableFuture<URI>().also {
                this.oauthUriFuture = it
            }
            // Create a new future to handle the any new error we may experience
            val loginErrorFuture = CompletableFuture<String>().also {
                this.loginErrorFuture = it
            }
            authorizationCodeFuture = CompletableFuture()

            val factory = getMicrosoftSessionFactory()

            // Begin the login process on another thread supplying our URI future for
            // the factory to populate
            Multithreading.runAsync {
                // Login and report error if present
                try {
                    factory.login(oauthUriFuture).uuid.let {
                        mostRecentAccountManager.get()?.login(it)
                    }
                    loginErrorFuture.complete(null)
                } catch (e: Exception) {
                    loginErrorFuture.complete(e.message)
                    mostRecentAccountManager.get()?.let {
                        Notifications.error("Account Error", "Something went wrong\nduring login.")
                    }
                    e.printStackTrace()
                }
            }

            send(
                exchange,
                "/assets/essential/account/login/microsoft.html",
                mapOf("location" to "$domain/microsoft/auth")
            )
        }
    }

    /**
     * Provides the current MicrosoftAccountSessionFactory
     */
    private fun getMicrosoftSessionFactory(): MicrosoftAccountSessionFactory {
        return Essential.getInstance().sessionFactories.filterIsInstance<MicrosoftAccountSessionFactory>().first()
    }

    /**
     * Register static pages
     */
    private fun registerPages(server: HttpServer) {
        server.createContext("/login/success") {
            send(it, "/assets/essential/account/login/success.html")
        }
        server.createContext("/") { exchange ->
            exchange.responseHeaders.add("Location", "/login/microsoft")
            exchange.sendResponseHeaders(302, 0)
            exchange.responseBody.close()
        }
    }

    /**
     * Register web assets
     */
    private fun registerAssets(server: HttpServer) {
        val assets = listOf(
            "core.c28d5d87.css",
            "main.c766a760.js",
            "main.5a1fd7db.js",
            "microsoft.2cc0fa02",
            "minecraft-computer-login.5074e926.png"
        )
        for (asset in assets) {
            server.createContext("/$asset") {
                send(it, "/assets/essential/account/$asset")
            }
        }
    }

}