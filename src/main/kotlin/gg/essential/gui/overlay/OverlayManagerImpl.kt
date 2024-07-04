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
package gg.essential.gui.overlay

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.Window
import gg.essential.elementa.events.UIClickEvent
import gg.essential.elementa.events.UIScrollEvent
import gg.essential.event.gui.GuiClickEvent
import gg.essential.event.gui.GuiDrawScreenEvent
import gg.essential.event.gui.GuiKeyTypedEvent
import gg.essential.event.gui.GuiMouseReleaseEvent
import gg.essential.event.gui.MouseScrollEvent
import gg.essential.event.render.RenderTickEvent
import gg.essential.universal.UKeyboard
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UMouse
import gg.essential.universal.UResolution
import gg.essential.universal.UScreen
import me.kbrewster.eventbus.Subscribe
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen

//#if MC>=11600
//$$ import gg.essential.mixins.transformers.client.MouseHelperAccessor
//$$ import net.minecraft.client.gui.widget.Widget
//#else
import gg.essential.mixins.transformers.client.gui.GuiScreenAccessor
import org.lwjgl.input.Mouse
import java.lang.invoke.MethodHandles
//#endif

object OverlayManagerImpl : OverlayManager {
    private val mc = Minecraft.getMinecraft()
    private val layers = mutableListOf<Layer>()

    private const val FAKE_MOUSE_POS = -1e6 // far off-screen but not too far so math doesn't break when cast to Int
    private var layersWithTrueMousePos = emptySet<Layer>()

    private var focus: Pair<Layer, UIComponent>? = null

    private val screenLayer = createLayer(VanillaScreenLayer())
    private val aboveScreenLayer = createPersistentLayer(LayerPriority.AboveScreen)

    override fun createPersistentLayer(priority: LayerPriority): PersistentLayer {
        return createLayer(PersistentLayer(priority))
    }

    override fun createEphemeralLayer(priority: LayerPriority): EphemeralLayer {
        return createLayer(EphemeralLayer(priority))
    }

    private fun <T : Layer> createLayer(layer: T): T {
        layers.add(layers.indexOfLast { it.priority <= layer.priority } + 1, layer)
        return layer
    }

    override fun removeLayer(layer: Layer) {
        layers.remove(layer)
    }

    /**
     * Returns the layer which the mouse is currently hovering.
     *
     * That is the top-most layer which contains a visible component at the mouse position.
     * Invisible components are those which do not get picked by [UIComponent.hitTest], or which have an alpha of zero,
     * or which extend [UIContainer]. If an invisible components gets picked by [UIComponent.hitTest] but one of its
     * parents is visible, then that layer is eligible as well (even if hitTest can never return that parent directly).
     */
    fun getHoveredLayer(): Layer? {
        val mouseX = (UMouse.Scaled.x + 0.5 / UResolution.scaleFactor).toFloat()
        val mouseY = (UMouse.Scaled.y + 0.5 / UResolution.scaleFactor).toFloat()
        return layers.findLast { it.isAnythingHovered(mouseX, mouseY) }
    }

    /**
     * Returns the currently focused layer if any.
     * The returned layer will have a [Window.focusedComponent] set.
     */
    fun getFocusedLayer(): Layer? {
        return focus
            ?.takeIf { (layer, component) -> layer.window.focusedComponent == component }
            ?.first
    }

    /**
     * Disposes of any ephemeral layers which no longer have any children in accordance with [EphemeralLayer.onClose].
     */
    private fun cleanupEphemeralLayers() {
        // Ephemeral layers may be temporarily empty e.g. when a modal is closed in response to a click and a new
        // component is scheduled to be opened via `Window.enqueueRenderOperation`. Ordinarily we'd clean up these
        // layers anyway, because there's no way for us to know that it's only temporarily empty.
        // But to catch at least some of these, we'll process all queued render operations before we clean up.
        // Using the screen layer because it's a dummy layer and it should always be empty.
        screenLayer.window.draw(UMatrixStack())

        layers.removeIf { layer ->
            if (layer is EphemeralLayer && layer.window.children.isEmpty()) {
                layer.onClose()
                if (layer.window.children.isEmpty()) {
                    val screen = UScreen.currentScreen
                    if (screen is OverlayInteractionScreen && screen.layer == layer) {
                        UScreen.displayScreen(null)
                    }
                    return@removeIf true
                }
            }
            return@removeIf false
        }
    }

    /**
     * If there are any layers which require mouse interaction as per [Layer.unlocksMouse], this method will open an
     * empty screen to facilitate that.
     */
    private fun unlockMouseIfRequired() {
        val layer = layers.findLast { it.unlocksMouse } ?: return
        if (UScreen.currentScreen == null) {
            UScreen.displayScreen(OverlayInteractionScreen(layer))
        }
    }

    /**
     * Determines which layers get to see the true mouse position (layers below the hovered layer get a fake one so they
     * do not show any hover effects).
     */
    private fun computeLayersWithTrueMousePos() {
        val hovered = getHoveredLayer()
        layersWithTrueMousePos = if (hovered != null) {
            layers.dropWhile { it != hovered }.toSet()
        } else {
            layers.toSet()
        }
    }

    /**
     * Checks if there are any newly focused components on any of the layers and if so, unfocuses all other layers.
     */
    private fun propagateFocus() {
        val foci = layers.mapNotNull { layer -> layer.window.focusedComponent?.let { layer to it } }
        focus =
            when {
                foci == listOfNotNull(focus) -> return // unchanged
                foci.isEmpty() -> null // nothing focused
                else -> foci.findLast { it != focus } // something focused, find the new focus
            }

        val (newLayer, _) = focus ?: return // if there's no focus, there's nothing to reset either

        for (layer in layers) {
            if (layer != newLayer) {
                layer.unfocus()
            }
        }
    }

    private fun handleDraw(matrixStack: UMatrixStack, priority: LayerPriority) =
        handleDraw(matrixStack, priority..priority)

    private fun handleDraw(matrixStack: UMatrixStack, priority: ClosedRange<LayerPriority>) {
        val hideGui = mc.gameSettings.hideGUI && mc.currentScreen == null

        for (layer in layers.filter { it.priority in priority }) {
            val layerMatrixStack =
                if (hideGui && layer.respectsHideGuiSetting || !layer.rendered) {
                    matrixStack.fork().also {
                        it.translate(FAKE_MOUSE_POS, FAKE_MOUSE_POS, 0.0)
                    }
                } else {
                    matrixStack
                }

            if (layer in layersWithTrueMousePos) {
                layer.window.draw(layerMatrixStack)
            } else {
                withFakeMousePos {
                    layer.window.draw(layerMatrixStack)
                }
            }
        }

        propagateFocus()
    }

    private fun handleClick(event: GuiClickEvent, priority: ClosedRange<LayerPriority>) {
        for (layer in layers.filter { it.priority in priority }.asReversed()) {
            var consumed = true
            val finalHandler: UIComponent.(UIClickEvent) -> Unit = {
                if (!it.propagationStopped && it.target.isPassThrough()) {
                    consumed = false
                }
            }

            layer.window.mouseClickListeners.add(finalHandler)
            layer.window.mouseClick(event.mouseX, event.mouseY, event.button)
            layer.window.mouseClickListeners.remove(finalHandler)

            if (consumed) {
                event.isCancelled = true
                break
            }
        }
        propagateFocus()
    }

    private fun handleMouseRelease(priority: ClosedRange<LayerPriority>) {
        for (layer in layers.filter { it.priority in priority }.asReversed()) {
            layer.window.mouseRelease()
        }
    }

    private fun handleKey(event: GuiKeyTypedEvent, priority: ClosedRange<LayerPriority>) {
        for (layer in layers.filter { it.priority in priority }.asReversed()) {
            layer.passThroughEvent = false

            layer.window.keyType(event.typedChar, event.keyCode)

            if (!layer.passThroughEvent) {
                event.isCancelled = true
                break
            }
        }
    }

    private fun handleScroll(event: MouseScrollEvent, priority: ClosedRange<LayerPriority>) {
        for (layer in layers.filter { it.priority in priority }.asReversed()) {
            var consumed = true
            val finalHandler: UIComponent.(UIScrollEvent) -> Unit = {
                if (!it.propagationStopped && it.target.isPassThrough()) {
                    consumed = false
                }
            }

            layer.window.mouseScrollListeners.add(finalHandler)
            layer.window.mouseScroll(event.amount.coerceIn(-1.0, 1.0))
            layer.window.mouseScrollListeners.remove(finalHandler)

            if (consumed) {
                event.isCancelled = true
                break
            }
        }
    }

    private fun Layer.isAnythingHovered(mouseX: Float, mouseY: Float): Boolean {
        if (this is VanillaScreenLayer) {
            return isAnythingHovered(mouseX, mouseY)
        }
        val hovered =
            window.hoveredFloatingComponent?.hitTest(mouseX, mouseY)
                ?: window.hitTest(mouseX, mouseY)
        return !hovered.isPassThrough()
    }

    private fun UIComponent.isPassThrough(): Boolean {
        return when (this) {
            is Window -> true
            is UIContainer -> parent.isPassThrough() // these have a default alpha of 1 but they don't render anything
            is UIBlock -> getColor().alpha == 0 && parent.isPassThrough()
            else -> false
        }
    }

    private fun Layer.unfocus() {
        if (this is VanillaScreenLayer) {
            unfocus()
        } else {
            window.unfocus()
        }
    }

    private inline fun withFakeMousePos(block: () -> Unit) {
        val orgX = UMouse.Raw.x
        val orgY = UMouse.Raw.y
        GlobalMouseOverride.set(FAKE_MOUSE_POS, FAKE_MOUSE_POS)
        block()
        GlobalMouseOverride.set(orgX, orgY)
    }

    object Events {
        private var originalMousePos: Pair<Double, Double>? = null

        private fun preDraw(event: GuiDrawScreenEvent) {
            cleanupEphemeralLayers()
            computeLayersWithTrueMousePos()

            handleDraw(event.matrixStack, LayerPriority.BelowScreenContent)

            // We're about to draw the screen content, so now's the time to suppress the mouse position if we need to
            // (technically, now is already a bit too late; ideally we modify the mouse pos in a priority-pre-event
            //  but we don't yet have such an event; this works fine for buttons, which is most of what usually gets
            //  hovered anyway)
            if (screenLayer !in layersWithTrueMousePos) {
                originalMousePos = Pair(UMouse.Raw.x, UMouse.Raw.y)
                GlobalMouseOverride.set(FAKE_MOUSE_POS, FAKE_MOUSE_POS)
                event.mouseX = FAKE_MOUSE_POS.toInt()
                event.mouseY = FAKE_MOUSE_POS.toInt()
            }
        }

        private fun postDraw(event: GuiDrawScreenEvent) {
            // Done drawing the screen content, restore mouse position
            originalMousePos?.let { (x, y) -> GlobalMouseOverride.set(x, y) }
            originalMousePos = null

            handleDraw(event.matrixStack, LayerPriority.AboveScreenContent)

            // We're about to draw modded content, so now's the time to suppress the mouse position if we need to
            if (aboveScreenLayer !in layersWithTrueMousePos) {
                originalMousePos = Pair(UMouse.Raw.x, UMouse.Raw.y)
                GlobalMouseOverride.set(FAKE_MOUSE_POS, FAKE_MOUSE_POS)
            }
        }

        private fun finalDraw(event: GuiDrawScreenEvent) {
            // Done with rendering, restore the real mouse position
            originalMousePos?.let { (x, y) -> GlobalMouseOverride.set(x, y) }
            originalMousePos = null

            handleDraw(event.matrixStack, LayerPriority.AboveScreen..LayerPriority.Highest)
        }

        private fun nonScreenDraw(event: RenderTickEvent) {
            cleanupEphemeralLayers()
            layersWithTrueMousePos = emptySet() // mouse is captured, no one gets to see it

            // TODO could add more specific events in the HUD rendering code, but we only use Modal and above atm anyway
            handleDraw(event.matrixStack, LayerPriority.BelowScreenContent..LayerPriority.Highest)

            unlockMouseIfRequired()
        }

        private fun flushVanillaBuffers() {
            // We need to flush the vanilla vertex consumers so that our rendering doesn't mess up their state.
            // Minecraft already flushes these at the *end* of GUI rendering, but we inject somewhere in the "middle",
            // so that buffer can be full, but not flushed yet.
            // `bufferSource` / `entityVertexConsumers` is the only one that we need to flush at the time of writing
            // this comment, since it's the only one that Minecraft uses during GUI rendering.

            //#if MC>=11600
            //$$ Minecraft.getInstance().getRenderTypeBuffers().getBufferSource().finish()
            //#endif
        }

        private var sawPriorityDrawEvent = true // assume good on first frame

        @Subscribe
        fun handleDraw(event: GuiDrawScreenEvent.Priority) {
            if (!event.screen.isReal()) return

            flushVanillaBuffers()
            finalDraw(event)
            sawPriorityDrawEvent = true
        }

        @Subscribe
        fun handleDraw(event: GuiDrawScreenEvent) {
            if (!event.screen.isReal()) return

            flushVanillaBuffers()

            if (event.isPre) {
                preDraw(event)
            } else {
                postDraw(event)

                if (!sawPriorityDrawEvent) {
                    finalDraw(event)
                }
                sawPriorityDrawEvent = false
            }
        }

        @Subscribe
        fun handleNonScreenDraw(event: RenderTickEvent) {
            if (UScreen.currentScreen != null) {
                return // more specific GuiDrawScreenEvents will be emitted
            }

            flushVanillaBuffers()
            nonScreenDraw(event)
        }

        @Subscribe
        fun firstClick(event: GuiClickEvent.Priority) {
            if (!event.screen.isReal()) return

            handleClick(event, LayerPriority.AboveScreen..LayerPriority.Highest)
        }

        @Subscribe
        fun preClick(event: GuiClickEvent) {
            if (!event.screen.isReal()) return

            // TODO don't yet have (nor need) a post-click event
            handleClick(event, LayerPriority.BelowScreenContent..LayerPriority.AboveScreenContent)
        }

        @Subscribe
        fun mouseRelease(event: GuiMouseReleaseEvent) {
            if (!event.screen.isReal()) return

            handleMouseRelease(LayerPriority.BelowScreenContent..LayerPriority.Highest)
        }

        @Subscribe
        fun firstKey(event: GuiKeyTypedEvent) {
            if (!event.screen.isReal()) return

            // TODO don't yet have (nor need) a post-type event, nor do we have a non-priority variant
            handleKey(event, LayerPriority.BelowScreenContent..LayerPriority.Highest)
        }

        @Subscribe
        fun firstScroll(event: MouseScrollEvent) {
            if (!event.screen.isReal()) return

            // TODO don't yet have (nor need) a post-scroll event, nor do we have a non-priority variant
            handleScroll(event, LayerPriority.BelowScreenContent..LayerPriority.Highest)
        }

        // We only care about events if they are for the real screen, not some kind of proxy (e.g. GuiScreenRealmsProxy)
        private fun GuiScreen?.isReal(): Boolean {
            return this != null && this == UScreen.currentScreen
        }
    }

    /**
     * Internal layer representing the vanilla screen.
     */
    private class VanillaScreenLayer : Layer(LayerPriority.AboveScreenContent) {
        fun isAnythingHovered(mouseX: Float, mouseY: Float): Boolean {
            val screen = UScreen.currentScreen ?: return false
            //#if MC>=11600
            //$$ return screen.getEventListenerForPos(mouseX.toDouble(), mouseY.toDouble()).isPresent
            //#else
            return (screen as GuiScreenAccessor).buttonList.any { button ->
                val xRange = button.x until (button.x + button.width)
                val yRange = button.y until (button.y + button.height)
                return mouseX.toInt() in xRange && mouseY.toInt() in yRange
            }
            //#endif
        }

        fun unfocus() {
            //#if MC>=11600
            //$$ val screen = UScreen.currentScreen ?: return // no active screen, nothing to do
            //$$ val focused = screen.listener as Widget? ?: return // nothing in focus, nothing to do
            //$$ if (!focused.isFocused) return // the thing in focus isn't actually in focus, nothing to do
            //#if MC<=11903
            //$$ if (focused.changeFocus(false)) return // the thing in focus refuses to unfocus, nothing we can do
            //#endif
            //$$ screen.listener = null
            //#else
            // There's no way to get the focused text field on old versions, so we have a mixin for it instead.
            // See: Mixin_UnfocusTextFieldWhileOverlayHasFocus
            //#endif
        }
    }

    internal class OverlayInteractionScreen(val layer: Layer) : UScreen() {
        override fun onKeyPressed(keyCode: Int, typedChar: Char, modifiers: UKeyboard.Modifiers?) {
            // no-op to suppress Esc key
        }

        override fun onTick() {
            // This screen may be active even when its corresponding layer has already been removed.
            // This can e.g. happen if this screen gets replaced by another screen, then the layer is destroyed, and
            // then the other screen restores its previous screen (i.e. this screen).
            if (layer !in layers) {
                // In such cases, we simply close this screen. If there's another modal active, it'll re-open a new one.
                displayScreen(null)
            }
            super.onTick()
        }
    }

    private object GlobalMouseOverride {
        //#if MC>=11600
        //$$ fun set(mouseX: Double, mouseY: Double) {
        //$$     val mouse = mc.mouseHelper as MouseHelperAccessor
        //$$     mouse.setMouseX(mouseX)
        //$$     mouse.setMouseY(mouseY)
        //$$ }
        //#else
        private val cls = Mouse::class.java
        private val lookup = MethodHandles.lookup()
        private val xField = lookup.unreflectSetter(cls.getDeclaredField("x").apply { isAccessible = true })
        private val yField = lookup.unreflectSetter(cls.getDeclaredField("y").apply { isAccessible = true })
        private val eventXField = lookup.unreflectSetter(cls.getDeclaredField("event_x").apply { isAccessible = true })
        private val eventYField = lookup.unreflectSetter(cls.getDeclaredField("event_y").apply { isAccessible = true })

        fun set(mouseX: Double, mouseY: Double) {
            val trueX = mouseX.toInt()
            val trueY = UResolution.windowHeight - mouseY.toInt() - 1
            xField.invokeExact(trueX)
            yField.invokeExact(trueY)
            eventXField.invokeExact(trueX)
            eventYField.invokeExact(trueY)
        }
        //#endif
    }
}
