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

import com.sparkuniverse.toolbox.util.DateTime
import gg.essential.Essential
import gg.essential.config.EssentialConfig
import gg.essential.config.LoadsResources
import gg.essential.elementa.components.UIImage
import gg.essential.elementa.components.Window
import gg.essential.gui.common.ImageLoadCallback
import gg.essential.gui.elementa.essentialmarkdown.EssentialMarkdown
import gg.essential.gui.friends.SocialMenu
import gg.essential.gui.screenshot.LocalScreenshot
import gg.essential.gui.screenshot.components.ScreenshotBrowser
import gg.essential.gui.screenshot.components.ScreenshotProperties
import gg.essential.universal.UDesktop
import gg.essential.universal.UMinecraft
import gg.essential.util.resource.EssentialAssetResourcePack
import net.minecraft.client.gui.GuiOptions
import net.minecraft.client.resources.FileResourcePack
import net.minecraft.client.resources.FolderResourcePack
import net.minecraft.client.resources.IResourcePack
import net.minecraft.util.ResourceLocation
import net.minecraft.util.Session
import org.apache.logging.log4j.LogManager
import java.awt.Desktop
import java.awt.HeadlessException
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.net.URI
import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import kotlin.io.path.toPath

//#if MC >= 11602
//$$ import net.minecraft.client.settings.PointOfView
//#else if MC == 10809 && FORGE
//$$ import com.google.common.collect.Multimap
//$$ import net.minecraftforge.fml.common.Loader
//$$ import net.minecraftforge.fml.common.LoaderState
//#endif

//#if MC>=11800
//$$ import net.minecraft.client.gui.screen.option.KeybindsScreen
//#else
import net.minecraft.client.gui.GuiControls
//#endif

//#if MC>=12005
//$$ import net.minecraft.resource.ResourcePackInfo
//$$ import net.minecraft.resource.ResourcePackSource
//#endif

private val LOGGER = LogManager.getLogger()

inline fun loop(block: () -> Unit) {
    while (true) {
        block()
    }
}

const val DATE_FORMAT = "MMM dd, yyyy"
const val DATE_FORMAT_NO_YEAR = "MMM dd"

fun getTimeFormat(includeSeconds: Boolean): String {
    val seconds = if (includeSeconds) ":ss" else ""
    return if (EssentialConfig.timeFormat == 0) "h:mm$seconds a" else "HH:mm$seconds"
}

fun formatDate(date: LocalDate, displayYear: Boolean = true) =
    date.formatter(if (displayYear) DATE_FORMAT else DATE_FORMAT_NO_YEAR)

fun formatTime(time: TemporalAccessor, includeSeconds: Boolean) =
    time.formatter(getTimeFormat(includeSeconds))

fun formatDateAndTime(date: TemporalAccessor) =
    date.formatter("$DATE_FORMAT @ ${getTimeFormat(includeSeconds = false)}")

fun TemporalAccessor.formatter(pattern: String): String {
    val format = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH).withZone(ZoneId.systemDefault())
    return format.format(this)
}

private val jarFileCopies = ConcurrentHashMap<URL, Path>()

fun findCodeSource(javaClass: Class<*>): CodeSource? {
    var url = javaClass.protectionDomain.codeSource?.location
    // ModLauncher on old Java 8 (like the default one) does not assign code sources, so we'll fall back to querying
    // the class file there.
    // Its `getResource` may or may not also be broken, so to be safe, we'll use `getResources`.
        ?: javaClass.classLoader.getResources(javaClass.name.replace('.', '/') + ".class")?.asSequence()?.firstOrNull()
        // We have no clue whatsoever
        ?: return null

    // With LaunchWrapper, every class gets their own protection domain and urls are of the form
    // jar:file:/some/path/to/archive.jar!/package/of/javaClass.class
    // or for directories
    // file:/some/path/to/dir/package/of/javaClass.class
    val classSuffix = "/${javaClass.name.replace('.', '/')}.class"
    val jarSuffix = "!$classSuffix"
    if (url.protocol == "jar" && url.file.endsWith(jarSuffix)) {
        url = URL(url.file.removeSuffix(jarSuffix))
    } else if (url.protocol == "file" && url.file.endsWith(classSuffix)) {
        url = URL(url.protocol, url.host, url.port, url.file.removeSuffix(classSuffix))
    }

    // With ModLauncher (Forge 1.16 edition), mod jars are on a pseudo "modjar" protocol, using the mod id as host.
    // For these, we need to unwrap them like the ModJarURLHandler does:
    //#if FORGE && MC==11602
    //$$ if (url.protocol == "modjar") {
    //$$     val modList = net.minecraftforge.fml.loading.FMLLoader.getLoadingModList()
    //$$     return CodeSource.Jar(modList.getModFileById(url.host).file.filePath)
    //$$ }
    //#endif

    // With ModLauncher (1.17+ edition), mod jars are wrapped into a "union" pseudo file system, we need to unwrap
    // them to get our actual jar file.
    // union:/home/user/.minecraft/instances/1.17.1%20Forge/mods/Essential%201.17.1-forge-master-SNAPSHOT.jar%2359!
    if (url.protocol == "union") {
        // We unfortunately cannot access UnionFileSystem because it is not exported, so string manipulation it is.
        val unionPath = url.toURI().path
        val originalScheme = "file" // I can't see any way to get hold of this, so let's just assume "file" for now
        val originalPath = unionPath.substring(0 until unionPath.lastIndexOf("#"))
        url = URI(originalScheme, null, originalPath, null).toURL()
    }

    // With ModLauncher (LexForge flavour, used by Forge 1.20.4+), mod jars are no longer wrapped in a "union"
    // file system. Instead, the url is in the form of jar:file:/some/path/to/archive.jar!/
    // much like launchwrapper, but always the root path.
    if (url.protocol == "jar" && url.file.endsWith("!/")) {
        url = URL(url.file.removeSuffix("!/"))
    }

    return if (url.file.endsWith(".jar")) {
        CodeSource.Jar(try {
            // Try to convert the URL to a real path
            url.toURI().toPath().toAbsolutePath().also {
                if (it.fileSystem != FileSystems.getDefault()) {
                    throw IOException("Path \"${it}\" is on non-default file system ${it.fileSystem}")
                }
                if (Files.notExists(it)) {
                    throw IOException("Path \"${it}\" does not exist")
                }
            }
        } catch (e: Exception) {
            LOGGER.debug("Failed to convert \"$url\" to local file, using copy instead.", e)
            // The URL likely points to some in-memory storage or similar
            // so we'll fall back to creating a temporary copy
            jarFileCopies.getOrPut(url) {
                Files.createTempFile("essential", ".jar").also { tmpFile ->
                    tmpFile.toFile().deleteOnExit()
                    url.openStream().use { Files.copy(it, tmpFile, StandardCopyOption.REPLACE_EXISTING) }
                }.toAbsolutePath()
            }
        })
    } else {
        // Likely dev env where the source is a folder
        try {
            // Going File and back to ensure it's not some special type of Path
            CodeSource.Directory(url.toURI().toPath().toFile().toPath())
        } catch (e: Exception) {
            LOGGER.warn("Failed to convert \"$url\" to file:", e)
            // We could copy the directory (if it is one) here but this should never happen in any environment, so let's
            // not worry about it.
            null
        }
    }
}

sealed interface CodeSource {
    data class Jar(val path: Path) : CodeSource
    data class Directory(val path: Path) : CodeSource
}

fun addEssentialResourcePack(consumer: Consumer<IResourcePack>) {
    consumer.accept(EssentialAssetResourcePack(Essential.getInstance().connectionManager.cosmeticsManager.assetLoader))

    //#if MC>=12005
    //$$ val info = makePackInfo("essential")
    //#endif

    val pack = when (val source = findCodeSource(Essential::class.java)) {
        //#if MC>=11903
        //$$ is CodeSource.Jar ->
            //#if MC>=12005
            //$$ ZipResourcePack.ZipBackedFactory(source.path.toFile()).open(info)
            //#elseif MC>=12002
            //$$ ZipResourcePack.ZipBackedFactory(source.path.toFile(), true).open("essential")
            //#else
            //$$ ZipResourcePack("essential", source.path.toFile(), true)
            //#endif
        //$$ is CodeSource.Directory ->
            //#if MC>=12005
            //$$ DirectoryResourcePack(info, source.path)
            //#else
            //$$ DirectoryResourcePack("essential", source.path, true)
            //#endif
        //#else
        is CodeSource.Jar -> FileResourcePack(source.path.toFile())
        is CodeSource.Directory -> FolderResourcePack(source.path.toFile())
        //#endif
        null -> return
    }
    //#if MC>=11400
    //$$ val desiredPackVersion = 4
    //#elseif MC>=11100
    val desiredPackVersion = 3
    //#else
    //$$ val desiredPackVersion = 2
    //#endif
    consumer.accept(
        when (desiredPackVersion) {
            4 -> gg.essential.util.resource.Lang3As4Pack(pack)
            3 -> pack
            2 -> gg.essential.util.resource.Lang3As2Pack(pack)
            else -> throw UnsupportedOperationException()
        }
    )
}

//#if MC>=12005
//$$ fun makePackInfo(name: String) = ResourcePackInfo(name, textLiteral(name), ResourcePackSource.NONE, Optional.empty())
//#endif

// FIXME preprocessor bug: should be able to remap the pre-1.19 ones (fails to remap the package)
//#if MC>=11900
//$$ fun textLiteral(str: String) = net.minecraft.text.Text.literal(str)
//$$ fun textTranslatable(key: String, vararg components: Any) = net.minecraft.text.Text.translatable(key, *components)
//#elseif FABRIC
//$$ fun textLiteral(str: String) = net.minecraft.text.LiteralText(str)
//$$ fun textTranslatable(key: String, vararg components: Any) = net.minecraft.text.TranslatableText(key, *components)
//#elseif MC>=11700
//$$ fun textLiteral(str: String) = net.minecraft.network.chat.TextComponent(str)
//$$ fun textTranslatable(key: String, vararg components: Any) = net.minecraft.network.chat.TranslatableComponent(key, *components)
//#elseif MC>=11200
fun textLiteral(str: String) = net.minecraft.util.text.TextComponentString(str)
fun textTranslatable(key: String, vararg components: Any) = net.minecraft.util.text.TextComponentTranslation(key, *components)
//#else
//$$ fun textLiteral(str: String) = net.minecraft.util.ChatComponentText(str)
//$$ fun textTranslatable(key: String, vararg components: Any) = net.minecraft.util.ChatComponentTranslation(key, *components)
//#endif

//#if MC>=11600
//$$ fun buttonLiteral(str: String) = textLiteral(str)
//#else
fun buttonLiteral(str: String) = str
//#endif

fun identifier(id: String)
    //#if MC>=12100
    //$$ = Identifier.of(id)
    //#else
    = ResourceLocation(id)
    //#endif

@LoadsResources("/assets/%namespace%/%path%(\\.[a-z]+)?")
fun identifier(namespace: String, path: String)
    //#if MC>=12100
    //$$ = Identifier.of(namespace, path)
    //#else
    = ResourceLocation(namespace, path)
    //#endif

// this is the worst code ever written in the history of the world
//#if MC == 10809 && FORGE
//$$ fun makeModsTable(modLoader: Loader, modStates: Multimap<String, LoaderState.ModState>): String {
//$$     val tableBuilder = StringBuilder()
//$$
//$$     val state = mutableListOf("State")
//$$     val id = mutableListOf("ID")
//$$     val version = mutableListOf("Version")
//$$     val source = mutableListOf("Source")
//$$     for (mod in modLoader.modList) {
//$$         var s = ""
//$$         modStates[mod.modId].forEach { s += it.marker }
//$$         state.add(s)
//$$         id.add(mod.modId)
//$$         version.add(mod.version)
//$$         source.add(mod.source.name)
//$$     }
//$$
//$$     fun MutableList<String>.longest(): Int = maxOf { it.length }
//$$
//$$     val stateL = state.longest()
//$$     val idL = id.longest()
//$$     val versionL = version.longest()
//$$     val sourceL = source.longest()
//$$
//$$     fun f(s: String, length: Int): String = "$s${" ".repeat(length - s.length)}"
//$$     fun Int.dashes(): String = "-".repeat(this)
//$$
//$$     tableBuilder.append("\t| ${f("State", stateL)} | ${f("ID", idL)} | ${f("Version", versionL)} | ${f("Source", sourceL)} |\n")
//$$     tableBuilder.append("\t| ${stateL.dashes()} | ${idL.dashes()} | ${versionL.dashes()} | ${sourceL.dashes()} |\n")
//$$     for (i in 1..state.lastIndex) {
//$$         tableBuilder.append("\t| ${f(state[i], stateL)} | ${f(id[i], idL)} | ${f(version[i], versionL)} | ${f(source[i], sourceL)} |\n")
//$$     }
//$$     return tableBuilder.toString()
//$$ }
//#endif

fun getPerspective() =
    //#if MC>=11602
    //$$ UMinecraft.getSettings().pointOfView.ordinal
    //#else
    UMinecraft.getSettings().thirdPersonView
    //#endif

fun setPerspective(perspective: Int) {
    // FIXME: Remap bug: Doesn't remap "method_31043" from 1.16.2 Fabric to "perspective" on 1.17
    //#if MC>=11701
    //$$ UMinecraft.getSettings().perspective = Perspective.values()[perspective]
    //#elseif MC>=11602
    //$$ UMinecraft.getSettings().pointOfView = PointOfView.values()[perspective]
    //#else
    UMinecraft.getSettings().thirdPersonView = perspective
    //#endif
    UMinecraft.getMinecraft().renderGlobal.setDisplayListEntitiesDirty()
}

fun getImageTime(properties: ScreenshotProperties, useEditIfPresent: Boolean): DateTime {
    val (id, metadata) = properties
    return if (metadata != null) {
        if (useEditIfPresent) {
            metadata.editTime ?: metadata.time
        } else {
            metadata.time
        }
    } else if (id is LocalScreenshot) {
        val path = id.path
        val name = path.fileName.toString()
            // If multiple screenshots were taken on this second, trim the trailing counter
            .split("_").take(2).joinToString("_")
            // and in any case, remove the file extension
            .removeSuffix(".png")
        val millis = try {
            ScreenshotBrowser.DATE_FORMAT.parse(name).time
        } catch (e: Exception) {
            try {
                Files.getLastModifiedTime(path).toMillis()
            } catch (e: Exception) {
                0
            }
        }
        DateTime(millis)
    } else {
        DateTime(0)
    }
}

fun openFileInDirectory(path: Path) {
    {
        val declaredMethod = Desktop::class.java.getDeclaredMethod("browseFileDirectory", File::class.java)
        declaredMethod.invoke(Desktop.getDesktop(), path.toFile())
        Unit
    }.catch(NoSuchMethodException::class, InvocationTargetException::class, HeadlessException::class) { throwable ->
        if (throwable is InvocationTargetException && throwable.cause !is UnsupportedOperationException) {
            throwable.printStackTrace()
        }
        fun command(vararg command: String): Boolean {
            return try {
                Runtime.getRuntime().exec(command).let {
                    it != null && it.isAlive
                }
            } catch (e: IOException) {
                false
            }
        }
        //On Windows and Mac we can implement browseFileDirectory on older java versions using commands
        //Adding quotes to the Windows command causes it to fail to work. The , after select is required
        if (!((UDesktop.isWindows && command("explorer.exe", "/select,", path.toAbsolutePath().toString()))
                || (UDesktop.isMac && command("open", "-R", "${path.toAbsolutePath()}")))
        ) {
            UDesktop.open(path.toFile().parentFile)
        }
    }
}

val screenshotFolder: File by lazy {
    var folder = File(UMinecraft.getMinecraft().mcDataDir, "screenshots")

    // Shared Resources mod compatibility
    //#if MC>=11800
    //$$ if (ModLoaderUtil.isModLoaded("shared-resources-api")) {
    //$$     try {
    //$$         val gameResourceHelperClass = Class.forName("nl.enjarai.shared_resources.api.GameResourceHelper")
    //$$         val gameResourceRegistryClass = Class.forName("nl.enjarai.shared_resources.api.GameResourceRegistry")
    //$$         val gameResourceClass = Class.forName("nl.enjarai.shared_resources.api.GameResource")
    //$$         val registry = gameResourceRegistryClass.getField("REGISTRY")[null]
    //$$         val screenshotResource = gameResourceRegistryClass.getMethod("get", Identifier::class.java)
    //$$             .invoke(registry, identifier("shared-resources:screenshots"))
    //$$         val path = gameResourceHelperClass.getMethod("getPathOrDefaultFor", gameResourceClass)
    //$$             .invoke(null, screenshotResource) as Path
    //$$         folder = path.toFile()
    //$$     } catch (e: Throwable) {
    //$$         Essential.logger.error(
    //$$             "Failed to resolve shared-resources mod screenshot directory. Using default instead.",
    //$$             e
    //$$         )
    //$$     }
    //$$ }
    //#endif

    if (!folder.exists()) {
        Essential.logger.debug("Screenshot directory not found. Creating...")
        try {
            Files.createDirectories(folder.toPath())
            Essential.logger.debug("Created screenshot directory.")
        } catch (e: IOException) {
            Essential.logger.error("Failed to create screenshot directory.", e)
        }
    }
    try {
        folder = folder.canonicalFile
    } catch (e: IOException) {
        Essential.logger.error("Failed to resolve screenshot directory.", e)
    }
    folder
}

fun Session.toUSession(): USession =
    USession(
        //#if MC>=12002
        //$$ uuidOrNull!!,
        //#else
        profile.id,
        //#endif
        username,
        token,
    )

fun USession.toMC() =
    Session(
        username,
        //#if MC>=12002
        //$$ uuid,
        //#else
        uuid.toString().replace("-", ""),
        //#endif
        token,
        //#if MC>=11800
        //$$ Optional.empty(),
        //$$ Optional.empty(),
        //$$ Session.AccountType.MSA,
        //#else
        "Xbox"
        //#endif
    )

class Memoize<in T, out R>(val f: (T) -> R) : (T) -> R {
    private val values = HashMap<T, R>()
    override fun invoke(x: T): R {
        return values.getOrPut(x, { f(x) })
    }
}

fun <T, R> ((T) -> R).memoize(): (T) -> R = Memoize(this)

fun getOrderedPaths(files: Set<String>, rootPath: Path, timeExtractor: (Path) -> DateTime): List<Path> {
    val memoized = timeExtractor.memoize()
    return files.map { rootPath.resolve(it) }.sortedWith(
        compareByDescending<Path> { memoized(it) }.thenBy { it.fileName.toString() }
    )
}

fun String?.ifNullOrEmpty(defaultValue: String) = if (isNullOrEmpty()) defaultValue else this

/**
 * Loads [image] as a [UIImage], calling [whenReady] on the UI thread once it is fully loaded.
 * May be called from any thread.
 * If [image] is `null`, no [UIImage] is created and [whenReady] will be called with `null`.
 */
fun maybeLoadUIImage(image: BufferedImage?, whenReady: (UIImage?) -> Unit) {
    if (image == null) {
        Window.enqueueRenderOperation { whenReady(null) }
    } else {
        loadUIImage(image, whenReady)
    }
}

/**
 * Loads [image] as a [UIImage], calling [whenReady] on the UI thread once it is fully loaded.
 * May be called from any thread.
 */
fun loadUIImage(image: BufferedImage, whenReady: (UIImage) -> Unit) {
    val uiImage = UIImage(CompletableFuture.completedFuture(image))
    Window.enqueueRenderOperation {
        uiImage.supply(ImageLoadCallback {
            whenReady(uiImage)
        })
    }
}

/** Listens for, and parses, any links pointing to custom essential protocol scheme and open associated GuiScreen */
val essentialUriListener: EssentialMarkdown.(EssentialMarkdown.LinkClickEvent) -> Unit = { event ->
    val prefix = "essential://"

    if (event.url.startsWith(prefix)) {
        val urlParts = event.url.removePrefix(prefix).lowercase().split("/")
        val screenName = urlParts.getOrNull(0)

        when (screenName) {
            "settings" -> GuiUtil.openScreen { EssentialConfig.gui(urlParts.getOrNull(1)) }
            "social" -> GuiUtil.openScreen { SocialMenu() }
            "minecraft" -> {
                when (urlParts.getOrNull(1)) {
                    "settings" -> {
                        when (urlParts.getOrNull(2)) {
                            null -> {
                                GuiUtil.openScreen {
                                    GuiOptions(UMinecraft.getMinecraft().currentScreen!!, UMinecraft.getSettings())
                                }
                            }
                            "keybinds" -> {
                                GuiUtil.openScreen {
                                    //#if MC>=11800
                                    //$$ KeybindsScreen(UMinecraft.getMinecraft().currentScreen!!, UMinecraft.getSettings())
                                    //#else
                                    GuiControls(UMinecraft.getMinecraft().currentScreen!!, UMinecraft.getSettings())
                                    //#endif
                                }
                            }
                        }
                    }
                }
            }
        }
        event.stopImmediatePropagation()
    }
}

fun createDateOnlyCalendar(time: Long = System.currentTimeMillis()): Calendar {
    val date: Calendar = GregorianCalendar()
    date.timeInMillis = time
    date.set(Calendar.HOUR_OF_DAY, 0)
    date.set(Calendar.MINUTE, 0)
    date.set(Calendar.SECOND, 0)
    date.set(Calendar.MILLISECOND, 0)
    return date
}
