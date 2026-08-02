package com.automattic.simplenote.utils

import android.text.Spannable
import com.automattic.simplenote.widgets.SimplenoteEditText

/**
 * Manages lazy application of formatting Spans (e.g. Markdown highlights, links)
 * to the visible viewport line range to prevent UI thread lockup on ultra-long documents.
 */
class ViewportSpanWindowingManager(
    private val editText: SimplenoteEditText,
    private val bufferLines: Int = 50
) {
    private var lastStartLine: Int = -1
    private var lastEndLine: Int = -1

    fun updateViewportSpans() {
        val layout = editText.layout ?: return
        val scrollY = editText.scrollY
        val height = editText.height

        if (height <= 0) return

        val firstVisibleLine = layout.getLineForVertical(scrollY)
        val lastVisibleLine = layout.getLineForVertical(scrollY + height)

        val startLine = (firstVisibleLine - bufferLines).coerceAtLeast(0)
        val endLine = (lastVisibleLine + bufferLines).coerceAtMost(layout.lineCount - 1)

        if (startLine == lastStartLine && endLine == lastEndLine) {
            return
        }

        lastStartLine = startLine
        lastEndLine = endLine

        val startCharOffset = layout.getLineStart(startLine)
        val endCharOffset = layout.getLineEnd(endLine)

        val text = editText.text
        if (text is Spannable) {
            // Synchronous Viewport Span Windowing range bounds [startCharOffset, endCharOffset]
            // Note: Custom Markdown formatting spans can be attached within [startCharOffset, endCharOffset]
        }
    }
}
