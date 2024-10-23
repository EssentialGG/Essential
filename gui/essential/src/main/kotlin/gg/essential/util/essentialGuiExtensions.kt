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

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.GradientComponent
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.events.UIClickEvent
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.utils.withAlpha
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.AbstractTooltip
import gg.essential.gui.common.EssentialTooltip
import gg.essential.gui.common.LayoutDslTooltip
import gg.essential.gui.common.bindParent
import gg.essential.gui.common.constraints.DivisionConstraint
import gg.essential.gui.common.constraints.MultiplicativeConstraint
import gg.essential.gui.elementa.lazyPosition
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.utils.toState
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.util.hoveredState
import gg.essential.gui.util.isComponentInParentChain
import gg.essential.gui.util.stateBy
import gg.essential.vigilance.utils.onLeftClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.Color
import java.util.concurrent.CompletableFuture


infix fun <T : UIComponent> T.hiddenChildOf(parent: UIComponent) = apply {
    parent.addChild(this)
    hide(instantly = true)
}

fun <T : UIComponent> T.animateColor(
    color: ColorConstraint,
    time: Float = .3f,
    strategy: AnimationStrategy = Animations.OUT_EXP
): T = apply {
    animate {
        setColorAnimation(strategy, time, color)
    }
}

fun <T : UIComponent> T.animateColor(
    color: Color,
    time: Float = .3f,
    strategy: AnimationStrategy = Animations.OUT_EXP
): T = animateColor(color.toConstraint(), time, strategy)

fun UIComponent.createLayoutDslTooltip(
    position: EssentialTooltip.Position = EssentialTooltip.Position.BELOW,
    padding: Float = 5f,
    windowPadding: Float? = null,
    layout: (LayoutScope.() -> Unit)
): LayoutDslTooltip {

    val tooltip = LayoutDslTooltip(this, layout)

    positionTooltip(tooltip, position, padding, windowPadding)

    return tooltip
}

/**
 * @param windowPadding Sets the padding from the window borders. The tooltip will be constrained to stay within the window+padding. Disabled if null
 */
fun UIComponent.createEssentialTooltip(
    tooltipContent: gg.essential.gui.elementa.state.v2.State<String>,
    position: EssentialTooltip.Position = EssentialTooltip.Position.BELOW,
    padding: Float = 5f,
    wrapAtWidth: Float? = null,
    configure: UIText.() -> Unit = {},
    windowPadding: Float? = null,
    notchSize: Int = 3,
    configureTooltip: EssentialTooltip.() -> Unit = {},
): EssentialTooltip {

    val tooltip = EssentialTooltip(this, position, notchSize)

    positionTooltip(tooltip, position, padding, windowPadding)

    tooltip.apply(configureTooltip)
    tooltip.bindLine(tooltipContent, wrapAtWidth, configure)
    return tooltip
}

/**
 * @param windowPadding Sets the padding from the window borders. The tooltip will be constrained to stay within the window+padding. Disabled if null
 */
fun UIComponent.createEssentialTooltip(
    tooltipContent: State<String>,
    position: EssentialTooltip.Position = EssentialTooltip.Position.BELOW,
    padding: Float = 5f,
    wrapAtWidth: Float? = null,
    configure: UIText.() -> Unit = {},
    windowPadding: Float? = null,
) = this.createEssentialTooltip(tooltipContent.toV2(), position, padding, wrapAtWidth, configure, windowPadding)


/**
 * @param windowPadding Sets the padding from the window borders. The tooltip will be constrained to stay within the window+padding. Disabled if null
 */
fun <T : UIComponent> T.bindEssentialTooltip(
    display: State<Boolean>,
    tooltipContent: State<String>,
    position: EssentialTooltip.Position = EssentialTooltip.Position.BELOW,
    padding: Float = 5f,
    wrapAtWidth: Float? = null,
    configure: UIText.() -> Unit = {},
    windowPadding: Float? = null,
): T {
    val tooltip = createEssentialTooltip(tooltipContent, position, padding, wrapAtWidth, configure, windowPadding)
    tooltip.bindVisibility(display)
    return this
}

/**
 * @param windowPadding Sets the padding from the window borders. The tooltip will be constrained to stay within the window+padding. Disabled if null
 */
@JvmOverloads
fun <T : UIComponent> T.bindHoverEssentialTooltip(
    tooltipContent: State<String>,
    position: EssentialTooltip.Position = EssentialTooltip.Position.BELOW,
    padding: Float = 5f,
    wrapAtWidth: Float? = null,
    configure: UIText.() -> Unit = {},
    windowPadding: Float? = null,
): T {
    return bindEssentialTooltip(hoveredState(), tooltipContent, position, padding, wrapAtWidth, configure, windowPadding)
}

private fun UIComponent.positionTooltip(
    tooltip: AbstractTooltip,
    position: EssentialTooltip.Position,
    padding: Float,
    windowPadding: Float?,
) {

    var xConstraint: XConstraint = when (position) {
        EssentialTooltip.Position.LEFT -> SiblingConstraint(padding = padding, alignOpposite = true)
        EssentialTooltip.Position.RIGHT -> SiblingConstraint(padding = padding)
        EssentialTooltip.Position.ABOVE, EssentialTooltip.Position.BELOW -> CenterConstraint()
    } boundTo this@positionTooltip

    var yConstraint: YConstraint = when (position) {
        EssentialTooltip.Position.LEFT, EssentialTooltip.Position.RIGHT -> CenterConstraint()
        EssentialTooltip.Position.ABOVE -> SiblingConstraint(padding = padding, alignOpposite = true)
        EssentialTooltip.Position.BELOW -> SiblingConstraint(padding = padding)
    } boundTo this@positionTooltip

    if (windowPadding != null) {
        val minConstraint = lazyPosition { windowPadding.pixels boundTo Window.of(this) }
        val maxConstraint = lazyPosition { windowPadding.pixels(alignOpposite = true) boundTo Window.of(this) }

        xConstraint = xConstraint.coerceIn(minConstraint, maxConstraint)
        yConstraint = yConstraint.coerceIn(minConstraint, maxConstraint)
    }

    tooltip.constrain {
        x = xConstraint
        y = yConstraint
    }
}

fun <T : UIComponent> T.centered(): T = apply {
    constrain {
        x = CenterConstraint()
        y = CenterConstraint()
    }
}

inline fun <reified T : UIComponent> UIComponent.findChildOfTypeOrNull(recursive: Boolean = false): T? {
    return findChildOfTypeOrNull(T::class.java, recursive)
}

fun <T : UIComponent> UIComponent.findChildOfTypeOrNull(type: Class<T>, recursive: Boolean = false): T? {
    for (child in children) {
        if (type.isInstance(child)) {
            return type.cast(child)
        } else if (recursive) {
            return child.findChildOfTypeOrNull(type, true) ?: continue
        }
    }
    return null
}

inline fun <reified T : UIComponent> UIComponent.findChildrenOfType(recursive: Boolean = false): MutableList<T> {
    return findChildrenOfType(T::class.java, recursive)
}

fun <T : UIComponent> UIComponent.findChildrenOfType(type: Class<T>, recursive: Boolean = false): MutableList<T> {
    val found = mutableListOf<T>()

    for (child in children) {
        if (type.isInstance(child)) {
            found.add(type.cast(child))
        }
        if (recursive) {
            found.addAll(child.findChildrenOfType(type, true))
        }
    }

    return found
}

inline fun <reified T : UIComponent> UIComponent.findParentOfType(): T {
    return findParentOfTypeOrNull(T::class.java)
        ?: throw IllegalStateException("Failed to find parent of type ${T::class.java.name} in $this")
}

inline fun <reified T : UIComponent> UIComponent.findParentOfTypeOrNull(): T? {
    return findParentOfTypeOrNull(T::class.java)
}

fun <T : UIComponent> UIComponent.findParentOfTypeOrNull(type: Class<T>): T? = when {
    type.isInstance(parent) -> type.cast(parent)
    parent != this -> parent.findParentOfTypeOrNull(type)
    else -> null
}

fun ScrollComponent.getHeightState(): State<Float> {
    val height = BasicState(0f)
    addScrollAdjustEvent(false) { _, percentageOfParent ->
        height.set((1f / percentageOfParent) * getHeight())
    }
    return height
}

fun LayoutScope.scrollGradient(scroller: ScrollComponent, top: Boolean, modifier: Modifier, maxGradient: Int = 204, opposite: Boolean = false) {
    val percentState = BasicState(0f)

    scroller.addScrollAdjustEvent(false) { percent, _ ->
        percentState.set(if (opposite) percent + 1 else percent)
    }

    val heightState = scroller.getHeightState()
    val percentAndHeightState = stateBy { Pair(percentState(), heightState()) }
    val gradient = scroller.newGradient(top, 0.pixels, maxGradient = maxGradient, percentAndHeightState = percentAndHeightState)
    gradient(Modifier.fillWidth().alignVertical(if (top) Alignment.Start else Alignment.End).then(modifier))
}

fun ScrollComponent.createScrollGradient(
    top: Boolean,
    heightSize: HeightConstraint,
    color: Color = EssentialPalette.GUI_BACKGROUND,
    maxGradient: Int = 204,
    opposite: Boolean = false,
): GradientComponent {

    val percentState = BasicState(0f)

    this.addScrollAdjustEvent(false) { percent, _ ->
        percentState.set(if (opposite) percent + 1 else percent)
    }

    val heightState = getHeightState()
    return createGradient(top, heightSize, color, maxGradient, percentState, heightState)
}

fun <T : UIComponent> T.createGradient(
    top: Boolean,
    heightSize: HeightConstraint,
    color: Color = EssentialPalette.GUI_BACKGROUND,
    maxGradient: Int = 204,
    percentState: State<Float>,
    heightState: State<Float>
): GradientComponent {
    val percentAndHeightState = stateBy { Pair(percentState(), heightState()) }
    val gradient = newGradient(top, heightSize, color, maxGradient, percentAndHeightState)
    gradient.parent = this
    this.children.add(gradient)
    return gradient
}

fun <T : UIComponent> T.newGradient(
    top: Boolean,
    heightSize: HeightConstraint,
    color: Color = EssentialPalette.GUI_BACKGROUND,
    maxGradient: Int = 204,
    percentAndHeightState: State<Pair<Float, Float>> = BasicState(Pair(if (top) 1f else 0f, 0f)),
): GradientComponent {
    return object : GradientComponent(
        color.withAlpha(0),
        color.withAlpha(0)
    ) {
        // Override because the gradient should be treated as if it does not exist from an input point of view
        override fun isPointInside(x: Float, y: Float): Boolean {
            return false
        }
    }.bindStartColor(percentAndHeightState.map { (percentage, height) ->
        if (top) {
            color.withAlpha((percentage * (height).coerceAtLeast(1000f)).toInt().coerceIn(0..maxGradient))
        } else {
            color.withAlpha(0)
        }
    }).bindEndColor(percentAndHeightState.map { (percentage, height) ->
        if (top) {
            color.withAlpha(0)
        } else {
            color.withAlpha(((1 - percentage) * (height).coerceAtLeast(1000f)).toInt().coerceIn(0..maxGradient))
        }
    }).constrain {
        y = 0.pixels(alignOpposite = !top) boundTo this@newGradient
        x = CopyConstraintFloat() boundTo this@newGradient
        width = CopyConstraintFloat() boundTo this@newGradient
        height = heightSize
    }
}

infix fun ScrollComponent.scrollGradient(heightSize: HeightConstraint) = apply {
    val topGradient = createScrollGradient(true, heightSize)
    createScrollGradient(false, 100.percent boundTo topGradient)
}

/**
 * Creates and returns a scrollbar horizontally bound within the [xPositionAndWidth] component
 * and vertically bound within the [yPositionAndHeight] component.
 * The function returned will remove the created scroll container from the window component
 * when called.
 */
fun createScrollbarRelativeTo(
    display: State<Boolean>,
    xPositionAndWidth: UIComponent,
    parent: UIComponent,
    yPositionAndHeight: UIComponent,
    initializeToBottom: Boolean,
): Pair<UIContainer, () -> Unit> {
    val scrollContainer by UIContainer().constrain {
        y = CenterConstraint() boundTo yPositionAndHeight
        x = CenterConstraint() boundTo xPositionAndWidth
        width = ChildBasedSizeConstraint()
        height = 100.percent boundTo yPositionAndHeight
    } childOf parent

    val scrollbar by UIContainer().constrain {
        y = 0.pixels(alignOpposite = initializeToBottom)
        x = CenterConstraint()
        width = ChildBasedSizeConstraint() + 6.pixels // Added to increase hitbox making it easier to grab
    }.bindParent(scrollContainer, display)

    val scrollbarBlock by UIBlock(EssentialPalette.LIGHT_SCROLLBAR).constrain {
        x = CenterConstraint()
        width = 100.percent boundTo xPositionAndWidth
        height = 100.percent
    } childOf scrollbar

    return scrollbar to {
        parent.removeChild(scrollContainer)
    }
}

operator fun SuperConstraint<Float>.times(other: SuperConstraint<Float>): MasterConstraint = MultiplicativeConstraint(this, other)

operator fun SuperConstraint<Float>.div(other: SuperConstraint<Float>): SizeConstraint = DivisionConstraint(this, other)

/**
 * Holds the scroll location of [child] after performing updates to the constraints/children.
 * Note, this will call this.animationFrame() after calling [block].
 */
fun ScrollComponent.holdScrollVerticalLocation(child: UIComponent, block: () -> Unit) {
    val offset = verticalOffset
    val top = child.getTop()
    block()
    animationFrame()
    val newTop = child.getTop()
    scrollTo(verticalOffset = offset - (newTop - top), smoothScroll = false)
}

/**
 * Adjusts the scroll position of this [ScrollComponent] so that [component] is centered
 * in the component. If the component cannot scroll to the center because there is not enough
 * space on either side, it will scroll to the closest possible position.
 */
fun ScrollComponent.scrollToCenterComponent(
    component: UIComponent,
    smooth: Boolean = true
) {
    if (!component.isComponentInParentChain(this)) {
        throw IllegalArgumentException("Component ${component.componentName} is not a child of ${this.componentName}")
    }

    fun UIComponent.center(): Float {
        return (getTop() + getBottom()) / 2f
    }

    scrollTo(verticalOffset = verticalOffset - (component.center() - center()), smoothScroll = smooth)
}

/**
 * Adjusts the scroll position of this [ScrollComponent] so that [component] is at the top
 * of the scroll component. If the component cannot scroll to the center because there is not enough
 * space on either side, it will scroll to the closest possible position.
 */
fun ScrollComponent.scrollToTopOf(
    component: UIComponent,
    smooth: Boolean = true,
    offset: Float = 0f,
) {
    if (!component.isComponentInParentChain(this)) {
        throw IllegalArgumentException("Component ${component.componentName} is not a child of ${this.componentName}")
    }

    scrollTo(verticalOffset = verticalOffset - (component.getTop() - getTop()) - offset, smoothScroll = smooth)
}

inline fun <T : UIComponent> T.onLeftClick(crossinline method: UIComponent.(event: UIClickEvent) -> Unit): T {
    onLeftClick(method)
    return this
}

inline fun <T : UIComponent> T.onRightClick(crossinline method: UIComponent.(event: UIClickEvent) -> Unit): T {
    onMouseClick {
        if (it.mouseButton == 1) {
            this.method(it)
        }
    }
    return this
}

fun <T> CompletableFuture<T>.thenAcceptOnMainThread(callback: (T) -> Unit): CompletableFuture<Void?> =
    this.thenAcceptAsync({ callback(it) }, Dispatchers.Client.asExecutor())

private val ESSENTIAL_LOGGER = LogManager.getLogger("Essential Logger")

@JvmOverloads
fun <T> CompletableFuture<T>.logExceptions(logger: Logger = ESSENTIAL_LOGGER): CompletableFuture<T> =
    whenComplete { _, e ->
        if (e != null) {
            logger.error("Unhandled error:", e)
        }
    }

fun <T> CompletableFuture<T>.toState(): gg.essential.gui.elementa.state.v2.State<T?> =
    toState(Dispatchers.Client.asExecutor())

/**
 * Returns a darker shade of this color by reducing its brightness.
 *
 * @param percentage The fraction by which to darken the color, where:
 * - `0.0f` returns the original color.
 * - `1.0f` returns black.
 */
fun Color.darker(percentage: Float): Color {
    val brightnessFactor = 1.0f - percentage
    return Color(
        (red * brightnessFactor).toInt().coerceIn(0, 255),
        (green * brightnessFactor).toInt().coerceIn(0, 255),
        (blue * brightnessFactor).toInt().coerceIn(0, 255),
        alpha
    )
}
