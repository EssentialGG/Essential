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
package gg.essential.gui.screenshot.components

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.constraints.resolution.ConstraintVisitor
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.elementa.utils.bindLast
import gg.essential.universal.UKeyboard
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UMouse
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs

/**
 * Based on Elementa's scroll component but modified to add a gap of the first [topGap] pixels on top of the scroll
 */
class ScreenshotScrollComponent @JvmOverloads constructor(
    emptyString: String = "",
    private val innerPadding: Float = 0f,
    private val scrollIconColor: Color = Color.WHITE,
    private val horizontalScrollEnabled: Boolean = false,
    private val verticalScrollEnabled: Boolean = true,
    private val horizontalScrollOpposite: Boolean = false,
    private val verticalScrollOpposite: Boolean = false,
    private val pixelsPerScroll: Float = 15f,
    private val scrollAcceleration: Float = 1.0f,
    customScissorBoundingBox: UIComponent? = null
) : UIContainer() {
    private var animationFPS: Int? = null

    private var topGap = verticalScreenshotPadding

    private val actualHolder = UIContainer().constrain {
        x = innerPadding.pixels()
        y = (innerPadding - topGap).pixels()
        width = RelativeConstraint(1f) - innerPadding.pixels()
        height = RelativeConstraint(1f)
    }

    // Exposed so its position and value can be adjusted by user
    val emptyText = UIWrappedText(emptyString, centered = true).constrain {
        x = CenterConstraint()
        y = SiblingConstraint() + 4.pixels() + topGap.pixels
    }

    private val scrollIconComponent = getScrollImage().constrain {
        width = 10.pixels()
        height = 16.pixels()
        color = scrollIconColor.toConstraint()
    }

    var horizontalOffset = innerPadding
        private set
    var verticalOffset = innerPadding - topGap
        private set


    private var horizontalScrollBarGrip: UIComponent? = null
    private var horizontalHideScrollWhenUseless = false
    private var verticalScrollBarGrip: UIComponent? = null
    private var verticalHideScrollWhenUseless = false

    private var horizontalDragBeginPos = -1f
    private var verticalDragBeginPos = -1f

    private val horizontalScrollAdjustEvents: MutableList<(Float, Float) -> Unit> =
        mutableListOf(::updateScrollBar.bindLast(true))
    private val verticalScrollAdjustEvents: MutableList<(Float, Float) -> Unit> =
        mutableListOf(::updateScrollBar.bindLast(false))
    private var needsUpdate = true

    private var isAutoScrolling = false
    private var autoScrollBegin: Pair<Float, Float> = -1f to -1f
    private var currentScrollAcceleration: Float = 1.0f

    val allChildren = CopyOnWriteArrayList<UIComponent>()

    init {
        this.constrain {
            width = ScrollChildConstraint() coerceAtMost 100.percentOfWindow()
            height = ScrollChildConstraint() coerceAtMost 100.percentOfWindow()
        }

        if (!horizontalScrollEnabled && !verticalScrollEnabled)
            throw IllegalArgumentException("ScrollComponent must have at least one direction of scrolling enabled")

        super.addChild(actualHolder)
        actualHolder.addChild(emptyText)
        this.enableEffects(ScissorEffect(customScissorBoundingBox))
        emptyText.setFontProvider(getFontProvider())
        super.addChild(scrollIconComponent)
        scrollIconComponent.hide(instantly = true)

        onMouseScroll {
            if (UKeyboard.isShiftKeyDown() && horizontalScrollEnabled) {
                onScroll(it.delta.toFloat(), isHorizontal = true)
            } else if (!UKeyboard.isShiftKeyDown() && verticalScrollEnabled) {
                onScroll(it.delta.toFloat(), isHorizontal = false)
            }

            it.stopPropagation()
        }

        onMouseClick { event ->
            onClick(event.relativeX, event.relativeY, event.mouseButton)
        }
    }

    private var lastActualWidth = 0f
    private var lastActualHeight = 0f

    override fun draw(matrixStack: UMatrixStack) {
        val actualWidth = calculateActualWidth()
        val actualHeight = calculateActualHeight()
        if (actualWidth != lastActualWidth || actualHeight != lastActualHeight) {
            lastActualWidth = actualWidth
            lastActualHeight = actualHeight
            needsUpdate = true
        }

        if (needsUpdate) {
            needsUpdate = false
            val horizontalRange = calculateOffsetRange(isHorizontal = true)
            val verticalRange = calculateOffsetRange(isHorizontal = false)

            // Recalculate our scroll box and move the content inside if needed.
            actualHolder.animate {
                horizontalOffset = if (horizontalRange.isEmpty()) {
                    innerPadding
                } else {
                    horizontalOffset.coerceIn(horizontalRange)
                }
                verticalOffset = if (verticalRange.isEmpty()) {
                    (innerPadding - topGap)
                } else {
                    verticalOffset.coerceIn(verticalRange)
                }
                setXAnimation(Animations.IN_SIN, 0.1f, horizontalOffset.pixels())
                setYAnimation(Animations.IN_SIN, 0.1f, verticalOffset.pixels())
            }
            // Run our scroll adjust event, normally updating [scrollBarGrip]
            var percent = (innerPadding - horizontalOffset) / horizontalRange.width()
            var percentageOfParent = this.getWidth() / actualWidth
            horizontalScrollAdjustEvents.forEach { it(percent, percentageOfParent) }

            percent = (innerPadding - verticalOffset - topGap) / verticalRange.width()
            percentageOfParent = this.getHeight() / actualHeight
            verticalScrollAdjustEvents.forEach { it(percent, percentageOfParent) }
        }

        super.draw(matrixStack)
    }

    override fun afterInitialization() {
        super.afterInitialization()

        animationFPS = Window.of(this).animationFPS
    }

    fun scrollTo(
        horizontalOffset: Float = this.horizontalOffset,
        verticalOffset: Float = this.verticalOffset,
        smoothScroll: Boolean = true
    ) {
        val horizontalRange = calculateOffsetRange(isHorizontal = true)
        val verticalRange = calculateOffsetRange(isHorizontal = false)
        this.horizontalOffset =
            if (horizontalRange.isEmpty()) innerPadding else horizontalOffset.coerceIn(horizontalRange)
        this.verticalOffset = if (verticalRange.isEmpty()) {
            innerPadding - topGap
        } else {
            verticalOffset.coerceIn(verticalRange)
        }

        if (smoothScroll) {
            needsUpdate = true
            return
        }

        actualHolder.setX(this.horizontalOffset.pixels())
        actualHolder.setY(this.verticalOffset.pixels())
        val horizontalFraction = (innerPadding - this.horizontalOffset) / horizontalRange
        val verticalFraction = (innerPadding -this.verticalOffset - topGap) / verticalRange
        horizontalScrollAdjustEvents.forEach { it(horizontalFraction, this.getWidth() / calculateActualWidth()) }
        verticalScrollAdjustEvents.forEach { it(verticalFraction, this.getHeight() / calculateActualHeight()) }
    }

    private operator fun Float.div(range: ClosedFloatingPointRange<Float>): Float {
        val width = range.width()
        return if (width == 0f) 0f else this / width
    }

    /**
     * Sets the text that appears when no items are shown
     */
    fun setEmptyText(text: String) {
        emptyText.setText(text)
    }

    fun addScrollAdjustEvent(
        isHorizontal: Boolean,
        event: (scrollPercentage: Float, percentageOfParent: Float) -> Unit
    ) {
        if (isHorizontal) horizontalScrollAdjustEvents.add(event) else verticalScrollAdjustEvents.add(event)
    }

    @JvmOverloads
    fun setHorizontalScrollBarComponent(component: UIComponent, hideWhenUseless: Boolean = false) {
        setScrollBarComponent(component, hideWhenUseless, isHorizontal = true)
    }

    @JvmOverloads
    fun setVerticalScrollBarComponent(component: UIComponent, hideWhenUseless: Boolean = false) {
        setScrollBarComponent(component, hideWhenUseless, isHorizontal = false)
    }

    /**
     * A scroll bar component is an optional component that can visually display the scroll status
     * of this scroll component (just like any scroll bar).
     *
     * The utility here is that it is automatically updated by this component with no extra work on the user-end.
     *
     * The hierarchy for this scrollbar grip component must be as follows:
     *  - Have a containing parent being the full height range of this scroll bar.
     *
     *  [component]'s mouse events will all be overridden by this action.
     *
     *  If [hideWhenUseless] is enabled, [component] will have [hide] called on it when the scrollbar is full height
     *  and dragging it would do nothing.
     */
    fun setScrollBarComponent(component: UIComponent, hideWhenUseless: Boolean, isHorizontal: Boolean) {
        if (isHorizontal) {
            horizontalScrollBarGrip = component
            horizontalHideScrollWhenUseless = hideWhenUseless
        } else {
            verticalScrollBarGrip = component
            verticalHideScrollWhenUseless = hideWhenUseless
        }

        component.onMouseScroll {
            if (isHorizontal && horizontalScrollEnabled && UKeyboard.isShiftKeyDown()) {
                onScroll(it.delta.toFloat(), isHorizontal = true)
            } else if (!isHorizontal && verticalScrollEnabled) {
                onScroll(it.delta.toFloat(), isHorizontal = false)
            }

            it.stopPropagation()
        }

        component.onMouseClick { event ->
            if (isHorizontal) {
                horizontalDragBeginPos = event.relativeX
            } else {
                verticalDragBeginPos = event.relativeY
            }
        }

        component.onMouseDrag { mouseX, mouseY, _ ->
            if (isHorizontal) {
                if (horizontalDragBeginPos == -1f)
                    return@onMouseDrag
                updateGrip(component, mouseX, isHorizontal = true)
            } else {
                if (verticalDragBeginPos == -1f)
                    return@onMouseDrag
                updateGrip(component, mouseY, isHorizontal = false)
            }
        }

        component.onMouseRelease {
            if (isHorizontal) {
                horizontalDragBeginPos = -1f
            } else {
                verticalDragBeginPos = -1f
            }
        }

        needsUpdate = true
    }

    fun scrollToTop(smoothScroll: Boolean = true) {
        scrollTo(verticalOffset = Float.POSITIVE_INFINITY, smoothScroll = smoothScroll)
    }

    fun scrollToBottom(smoothScroll: Boolean = true) {
        scrollTo(verticalOffset = Float.NEGATIVE_INFINITY, smoothScroll = smoothScroll)
    }

    fun filterChildren(filter: (component: UIComponent) -> Boolean) {
        actualHolder.children.clear()
        actualHolder.children.addAll(allChildren.filter(filter).ifEmpty { listOf(emptyText) })
        actualHolder.children.forEach { it.parent = actualHolder }

        needsUpdate = true
    }

    @JvmOverloads
    fun <T : Comparable<T>> sortChildren(descending: Boolean = false, comparator: (UIComponent) -> T) {
        if (descending) {
            actualHolder.children.sortByDescending(comparator)
        } else {
            actualHolder.children.sortBy(comparator)
        }
    }

    fun sortChildren(comparator: Comparator<UIComponent>) {
        actualHolder.children.sortWith(comparator)
    }

    private fun updateGrip(component: UIComponent, mouseCoord: Float, isHorizontal: Boolean) {
        if (isHorizontal) {
            val minCoord = component.parent.getLeft()
            val maxCoord = component.parent.getRight()
            val dragDelta = mouseCoord - horizontalDragBeginPos

            horizontalOffset = if (horizontalScrollOpposite) {
                val newPos = maxCoord - component.getRight() - dragDelta
                val percentage = newPos / (maxCoord - minCoord)

                percentage * calculateActualWidth()
            } else {
                val newPos = component.getLeft() + dragDelta - minCoord
                val percentage = newPos / (maxCoord - minCoord)

                -(percentage * calculateActualWidth())
            }
        } else {
            val minCoord = component.parent.getTop()
            val maxCoord = component.parent.getBottom()
            val dragDelta = mouseCoord - verticalDragBeginPos

            verticalOffset = -topGap + if (verticalScrollOpposite) {
                val newPos = maxCoord - component.getBottom() - dragDelta
                val percentage = newPos / (maxCoord - minCoord)

                percentage * calculateActualHeight()
            } else {
                val newPos = component.getTop() + dragDelta - minCoord
                val percentage = newPos / (maxCoord - minCoord)

                -(percentage * calculateActualHeight())
            }
        }

        needsUpdate = true
    }

    private fun onScroll(delta: Float, isHorizontal: Boolean) {
        if (isHorizontal) {
            horizontalOffset += delta * pixelsPerScroll * currentScrollAcceleration
        } else {
            verticalOffset += delta * pixelsPerScroll * currentScrollAcceleration
        }

        currentScrollAcceleration =
            (currentScrollAcceleration + (scrollAcceleration - 1.0f) * 0.15f).coerceIn(0f, scrollAcceleration)

        needsUpdate = true
    }

    private fun updateScrollBar(scrollPercentage: Float, percentageOfParent: Float, isHorizontal: Boolean) {
        val component = if (isHorizontal) {
            horizontalScrollBarGrip ?: return
        } else {
            verticalScrollBarGrip ?: return
        }

        val clampedPercentage = percentageOfParent.coerceAtMost(1f)

        if ((isHorizontal && horizontalHideScrollWhenUseless) || (!isHorizontal && verticalHideScrollWhenUseless)) {
            if (clampedPercentage == 1f) {
                Window.enqueueRenderOperation(component::hide)
                return
            } else {
                Window.enqueueRenderOperation(component::unhide)
            }
        }

        val relativeConstraint = RelativeConstraint(clampedPercentage)
        if (isHorizontal) {
            component.setWidth(ScrollBarGripMinSizeConstraint(relativeConstraint))
        } else {
            component.setHeight(ScrollBarGripMinSizeConstraint(relativeConstraint))
        }

        component.animate {
            if (isHorizontal) {
                setXAnimation(
                    Animations.IN_SIN, 0.1f, basicXConstraint { component ->
                        val offset = (component.parent.getWidth() - component.getWidth()) * scrollPercentage

                        if (horizontalScrollOpposite) component.parent.getRight() - component.getHeight() - offset
                        else component.parent.getLeft() + offset
                    }
                )
            } else {
                setYAnimation(
                    Animations.IN_SIN, 0.1f, basicYConstraint { component ->
                        val offset = (component.parent.getHeight() - component.getHeight()) * scrollPercentage

                        if (verticalScrollOpposite) component.parent.getBottom() - component.getHeight() - offset
                        else component.parent.getTop() + offset
                    }
                )
            }
        }
    }

    private fun calculateActualWidth(): Float {
        if (actualHolder.children.isEmpty()) return 0f

        return actualHolder.children.let { c ->
            c.maxOf { it.getRight() } - c.minOf { it.getLeft() }
        }
    }

    private fun calculateActualHeight(): Float {
        if (actualHolder.children.isEmpty()) return 0f

        return actualHolder.children.let { c ->
            c.maxOf { it.getBottom() } - c.minOf { it.getTop() + topGap }
        }
    }

    private fun calculateOffsetRange(isHorizontal: Boolean): ClosedFloatingPointRange<Float> {
        return if (isHorizontal) {
            val actualWidth = calculateActualWidth()
            val maxNegative = this.getWidth() - actualWidth - innerPadding
            if (horizontalScrollOpposite) (-innerPadding)..-maxNegative else maxNegative..(innerPadding)
        } else {
            val actualHeight = calculateActualHeight()
            val maxNegative = this.getHeight() - actualHeight - innerPadding - topGap
            if (verticalScrollOpposite) (-innerPadding)..-maxNegative else maxNegative..(innerPadding - topGap)
        }
    }

    private fun onClick(mouseX: Float, mouseY: Float, mouseButton: Int) {
        if (isAutoScrolling) {
            isAutoScrolling = false
            scrollIconComponent.hide()
            return
        }

        if (mouseButton == 2) {
            // Middle click, begin the auto scroll
            isAutoScrolling = true
            autoScrollBegin = mouseX to mouseY

            scrollIconComponent.constrain {
                x = (mouseX - 5).pixels()
                y = (mouseY - 8).pixels()
            }

            scrollIconComponent.unhide(useLastPosition = false)
        }
    }

    override fun animationFrame() {
        super.animationFrame()

        currentScrollAcceleration =
            (currentScrollAcceleration - ((scrollAcceleration - 1.0f) / (animationFPS ?: 244).toFloat()))
                .coerceAtLeast(1.0f)

        if (!isAutoScrolling) return

        if (horizontalScrollEnabled) {
            val xBegin = autoScrollBegin.first + getLeft()
            val currentX = UMouse.Scaled.x

            if (currentX in getLeft()..getRight()) {
                val deltaX = currentX - xBegin
                val percentX = deltaX / (-getWidth() / 2)
                horizontalOffset += (percentX.toFloat() * 5f)
                needsUpdate = true
            }
        }

        if (verticalScrollEnabled) {
            val yBegin = autoScrollBegin.second + getTop()
            val currentY = UMouse.Scaled.y

            if (currentY in getTop()..getBottom()) {
                val deltaY = currentY - yBegin
                val percentY = deltaY / (-getHeight() / 2)
                verticalOffset += (percentY.toFloat() * 5f)
                needsUpdate = true
            }
        }

        needsUpdate = true
    }

    override fun addChild(component: UIComponent) = apply {
        actualHolder.removeChild(emptyText)

        actualHolder.addChild(component)
        allChildren.add(component)

        needsUpdate = true
    }

    override fun insertChildAt(component: UIComponent, index: Int) = apply {
        if (index < 0 || index > allChildren.size) {
            println("Bad index given to insertChildAt (index: $index, children size: ${allChildren.size}")
            return@apply
        }

        actualHolder.removeChild(emptyText)

        component.parent = actualHolder
        actualHolder.children.add(index, component)
        allChildren.add(index, component)

        needsUpdate = true
    }

    override fun insertChildBefore(newComponent: UIComponent, targetComponent: UIComponent) = apply {
        val indexOfExisting = allChildren.indexOf(targetComponent)
        if (indexOfExisting == -1) {
            println("targetComponent given to insertChildBefore is not a child of this component")
            return@apply
        }

        insertChildAt(newComponent, indexOfExisting)
    }

    override fun insertChildAfter(newComponent: UIComponent, targetComponent: UIComponent) = apply {
        val indexOfExisting = allChildren.indexOf(targetComponent)
        if (indexOfExisting == -1) {
            println("targetComponent given to insertChildAfter is not a child of this component")
            return@apply
        }

        insertChildAt(newComponent, indexOfExisting + 1)
    }

    override fun replaceChild(newComponent: UIComponent, componentToReplace: UIComponent) = apply {
        val indexOfExisting = allChildren.indexOf(componentToReplace)
        if (indexOfExisting == -1) {
            println("componentToReplace given to replaceChild is not a child of this component")
            return@apply
        }

        actualHolder.removeChild(emptyText)

        actualHolder.children.removeAt(indexOfExisting)
        allChildren.removeAt(indexOfExisting)

        newComponent.parent = actualHolder
        actualHolder.children.add(indexOfExisting, newComponent)
        allChildren.add(indexOfExisting, newComponent)

        needsUpdate = true
    }

    override fun removeChild(component: UIComponent) = apply {
        if (component == scrollIconComponent) {
            super.removeChild(component)
            return@apply
        }

        actualHolder.removeChild(component)
        allChildren.remove(component)

        if (allChildren.isEmpty())
            actualHolder.addChild(emptyText)

        needsUpdate = true
    }

    override fun clearChildren() = apply {
        allChildren.clear()
        actualHolder.clearChildren()
        actualHolder.addChild(emptyText)

        needsUpdate = true
    }

    override fun alwaysDrawChildren(): Boolean {
        return true
    }

    override fun <T> childrenOfType(clazz: Class<T>): List<T> {
        return actualHolder.childrenOfType(clazz)
    }

    override fun mouseClick(mouseX: Double, mouseY: Double, button: Int) {
        actualHolder.mouseClick(mouseX, mouseY, button)
    }

    override fun hitTest(x: Float, y: Float): UIComponent {
        return actualHolder.hitTest(x, y)
    }

    fun setChildren(components: List<UIComponent>) = apply {
        actualHolder.children.clear()
        actualHolder.children.addAll(components.ifEmpty { listOf(emptyText) })
        actualHolder.children.forEach { it.parent = actualHolder }

        allChildren.clear()
        allChildren.addAll(actualHolder.children)

        needsUpdate = true
    }

    private fun ClosedFloatingPointRange<Double>.width() = abs(this.start - this.endInclusive)
    private fun ClosedFloatingPointRange<Float>.width() = abs(this.start - this.endInclusive)

    /**
     * Constrains a scroll component to either the widest/tallest child or to the sum of their widths/heights and
     * padding. If [horizontalScrollEnabled] or [verticalScrollEnabled] are true, they will change the constraint to
     * use the total of their children's corresponding measurements and padding. Otherwise, it will use the maximum
     * measurement for the corresponding direction.
     *
     * This is the default width and height constraint for scroll components.
     *
     * @param padding Pixels of padding to add to each component
     */
    inner class ScrollChildConstraint(val padding: Float = 0f) : WidthConstraint, HeightConstraint {
        override var cachedValue = 0f
        override var recalculate = true
        override var constrainTo: UIComponent? = null

        private val sumConstraint = ChildBasedSizeConstraint(padding)
        private val maxConstraint = ChildBasedMaxSizeConstraint()

        override fun getWidthImpl(component: UIComponent): Float {
            val constraint = if (horizontalScrollEnabled) sumConstraint else maxConstraint
            constraint.constrainTo = this.constrainTo ?: actualHolder
            return constraint.getWidthImpl(component)
        }

        override fun getHeightImpl(component: UIComponent): Float {
            val constraint = if (verticalScrollEnabled) sumConstraint else maxConstraint
            constraint.constrainTo = this.constrainTo ?: actualHolder
            return constraint.getHeightImpl(component)
        }

        override fun visitImpl(visitor: ConstraintVisitor, type: ConstraintType) {
            when (type) {
                ConstraintType.WIDTH -> visitor.visitChildren(ConstraintType.WIDTH)
                ConstraintType.HEIGHT -> visitor.visitChildren(ConstraintType.HEIGHT)
                else -> throw IllegalArgumentException(type.prettyName)
            }
        }

    }

    /**
     * Constraints the scrollbar grip's size to be a certain minimum size, or the [desiredSize].
     * This is the default constraint for horizontal scrollbar grips if [ElementaVersion.V6] is used.
     *
     * @param desiredSize The intended size for the scrollbar grip.
     */
    private class ScrollBarGripMinSizeConstraint(
        private val desiredSize: SizeConstraint
    ) : SizeConstraint {
        override var cachedValue: Float = 0f
        override var recalculate: Boolean = true
        override var constrainTo: UIComponent? = null

        override fun animationFrame() {
            super.animationFrame()
            desiredSize.animationFrame()
        }

        override fun getWidthImpl(component: UIComponent): Float {
            val parent = component.parent
            val minimumWidthPercentage = if (parent.getWidth() < 200) { 0.15f } else { 0.10f }
            val minimumWidth = parent.getWidth() * minimumWidthPercentage

            return desiredSize.getWidth(component).coerceAtLeast(minimumWidth)
        }

        override fun getHeightImpl(component: UIComponent): Float {
            val parent = component.parent
            val minimumHeightPercentage = if (parent.getHeight() < 200) { 0.15f } else { 0.10f }
            val minimumHeight = parent.getHeight() * minimumHeightPercentage

            return desiredSize.getHeight(component).coerceAtLeast(minimumHeight)
        }

        override fun visitImpl(visitor: ConstraintVisitor, type: ConstraintType) {
        }

        override fun getRadiusImpl(component: UIComponent): Float {
            throw IllegalStateException("`ScrollBarGripMinSizeConstraint` does not support `getRadiusImpl`.")
        }
    }

    companion object {

        fun getScrollImage(): UIImage {
            return UIImage.ofResourceCached("/vertical-scroll.png")
        }
    }
}
