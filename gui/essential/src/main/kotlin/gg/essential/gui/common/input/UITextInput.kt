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
package gg.essential.gui.common.input

import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.gui.EssentialPalette
import gg.essential.universal.UMatrixStack
import java.awt.Color

open class UITextInput @JvmOverloads constructor(
    placeholder: String = "",
    shadow: Boolean = true,
    shadowColor: Color? = null,
    selectionBackgroundColor: Color = EssentialPalette.TEXT_HIGHLIGHT_BACKGROUND,
    selectionForegroundColor: Color = EssentialPalette.TEXT_HIGHLIGHT,
    allowInactiveSelection: Boolean = false,
    inactiveSelectionBackgroundColor: Color = Color(176, 176, 176),
    inactiveSelectionForegroundColor: Color = Color.WHITE,
    cursorColor: Color = EssentialPalette.TEXT_HIGHLIGHT,
    maxLength: Int = Int.MAX_VALUE
) : AbstractTextInput(
    placeholder,
    shadow,
    shadowColor,
    selectionBackgroundColor,
    selectionForegroundColor,
    allowInactiveSelection,
    inactiveSelectionBackgroundColor,
    inactiveSelectionForegroundColor,
    cursorColor,
    maxLength
) {
    protected var minWidth: WidthConstraint? = null
    protected var maxWidth: WidthConstraint? = null

    protected val placeholderWidth = placeholder.width()

    fun setMinWidth(constraint: WidthConstraint) = apply {
        minWidth = constraint
    }

    fun setMaxWidth(constraint: WidthConstraint) = apply {
        maxWidth = constraint
    }

    override fun getText() = textualLines.first().text

    protected open fun getTextForRender(): String = getText()

    protected open fun setCursorPos() {
        cursorComponent.unhide()
        val (cursorPosX, _) = cursor.toScreenPos()
        cursorComponent.setX((cursorPosX).pixels())
    }

    override fun textToLines(text: String): List<String> {
        return listOf(text.replace('\n', ' '))
    }

    override fun scrollIntoView(pos: LinePosition) {
        val column = pos.column
        val lineText = getTextForRender()
        if (column < 0 || column > lineText.length)
            return

        val widthBeforePosition = lineText.substring(0, column).width(getTextScale(), getFontProvider())
        val widthTotal = widthBeforePosition + (if (active) cursorComponent.getWidth() else 0f)

        when {
            getTextForRender().width(getTextScale(), getFontProvider()) < getWidth() -> {
                horizontalScrollingOffset = 0f
            }

            horizontalScrollingOffset > widthBeforePosition -> {
                horizontalScrollingOffset = widthBeforePosition
            }

            widthTotal - horizontalScrollingOffset > getWidth() -> {
                horizontalScrollingOffset = widthTotal - getWidth()
            }
        }
    }

    override fun screenPosToVisualPos(x: Float, y: Float): LinePosition {
        val targetXPos = x + horizontalScrollingOffset
        var currentX = 0f

        val line = getTextForRender()

        for (i in line.indices) {
            val charWidth = line[i].toString().width(getTextScale(), getFontProvider())
            if (currentX + (charWidth / 2) >= targetXPos) return LinePosition(0, i, isVisual = true)
            currentX += charWidth
        }

        return LinePosition(0, line.length, isVisual = true)
    }

    override fun recalculateDimensions() {
        val minWidth = minWidth ?: return
        val maxWidth = maxWidth ?: return

        val newWidth = basicWidthConstraint {
            if (!hasText() && !this.active) {
                placeholderWidth
            } else {
                getTextForRender().width(getTextScale(), getFontProvider()) + 1 /* cursor */
            }
        }

        setWidth(newWidth.coerceIn(minWidth, maxWidth))
    }

    override fun splitTextForWrapping(text: String, maxLineWidth: Float): List<String> {
        return listOf(text)
    }

    override fun onEnterPressed() {
        activateAction(getText())
    }

    override fun draw(matrixStack: UMatrixStack) {
        beforeDrawCompat(matrixStack)

        if (!active && !hasText()) {
            drawPlaceholder(matrixStack)
            return super.draw(matrixStack)
        }

        val lineText = getTextForRender()

        if (hasSelection()) {
            var currentX = getLeft()
            cursorComponent.hide(instantly = true)

            if (!selectionStart().isAtLineStart) {
                val preSelectionText = lineText.substring(0, selectionStart().column)
                drawUnselectedText(matrixStack, preSelectionText, currentX, row = 0)
                currentX += preSelectionText.width(getTextScale(), getFontProvider())
            }

            val selectedText = lineText.substring(selectionStart().column, selectionEnd().column)
            val selectedTextWidth = selectedText.width(getTextScale(), getFontProvider())
            drawSelectedText(matrixStack, selectedText, currentX, currentX + selectedTextWidth, row = 0)
            currentX += selectedTextWidth

            if (!selectionEnd().isAtLineEnd) {
                drawUnselectedText(matrixStack, lineText.substring(selectionEnd().column), currentX, row = 0)
            }
        } else {
            if (active) {
                cursorComponent.setY(basicYConstraint {
                    getTop()
                })
                setCursorPos()
            }

            drawUnselectedText(matrixStack, lineText, getLeft(), 0)
        }

        super.draw(matrixStack)
    }
}
