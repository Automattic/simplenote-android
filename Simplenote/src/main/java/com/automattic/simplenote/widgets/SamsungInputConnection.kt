package com.automattic.simplenote.widgets

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.Selection
import android.text.Spanned
import android.text.style.SuggestionSpan
import android.view.KeyEvent
import android.view.inputmethod.*
import com.automattic.simplenote.utils.AppLog

/**
 * Wrapper around proprietary Samsung InputConnection. Forwards all the calls to it, except for getExtractedText and
 * some custom logic in commitText
 */
class SamsungInputConnection(
    private val mTextView: SimplenoteEditText,
    private val baseInputConnection: InputConnection,
) : BaseInputConnection(mTextView, true) {

    override fun getEditable(): Editable {
        return mTextView.editableText
    }

    override fun beginBatchEdit(): Boolean {
        return baseInputConnection.beginBatchEdit()
    }

    override fun endBatchEdit(): Boolean {
        return baseInputConnection.endBatchEdit()
    }

    override fun clearMetaKeyStates(states: Int): Boolean {
        return baseInputConnection.clearMetaKeyStates(states)
    }

    override fun sendKeyEvent(event: KeyEvent?): Boolean {
        return super.sendKeyEvent(event)
    }

    override fun commitCompletion(text: CompletionInfo?): Boolean {
        return baseInputConnection.commitCompletion(text)
    }

    override fun commitCorrection(correctionInfo: CorrectionInfo?): Boolean {
        return baseInputConnection.commitCorrection(correctionInfo)
    }

    override fun performEditorAction(actionCode: Int): Boolean {
        return baseInputConnection.performEditorAction(actionCode)
    }

    override fun performContextMenuAction(id: Int): Boolean {
        return baseInputConnection.performContextMenuAction(id)
    }

    // Extracted text on Samsung devices on Android 13 is somehow used for Grammarly suggestions which causes a lot of
    // issues with spans and cursors. IF we return null, then it causes text duplication, so we implement the
    override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText? {
        val et = ExtractedText()
        if (mTextView.extractText(request, et)) {
            return et
        }
        return null
    }

    override fun performPrivateCommand(action: String?, data: Bundle?): Boolean {
        return baseInputConnection.performPrivateCommand(action, data)
    }

    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
        return baseInputConnection.setComposingText(text, newCursorPosition)
    }

    /**
     * The protective branch in [commitText] exists only to keep checklist spans ([CheckableSpan]) from being
     * corrupted when the Samsung keyboard replaces editor content. If the region the keyboard is about to edit
     * (the composing region, or the current selection if there is none) contains no checklist span, there is
     * nothing to protect, so we let the keyboard apply its normal autocorrect/suggestion behavior instead of
     * suppressing it.
     */
    private fun editedRegionHasCheckableSpan(): Boolean {
        val editable = editable
        var start = getComposingSpanStart(editable)
        var end = getComposingSpanEnd(editable)

        if (start == -1 || end == -1) {
            start = Selection.getSelectionStart(editable)
            end = Selection.getSelectionEnd(editable)
        }

        if (start < 0) start = 0
        if (end < 0) end = 0
        if (end < start) {
            val tmp = start
            start = end
            end = tmp
        }

        return editable.getSpans(start, end, CheckableSpan::class.java).isNotEmpty()
    }

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        val incomingTextHasSuggestions = text is Spanned &&
                text.getSpans(0, text.length, SuggestionSpan::class.java).isNotEmpty()

        // Sometime spellchecker tries to commit partial text with suggestions. This mostly works ok,
        // but CheckableSpan spans are finicky, and tend to get messed when content of the editor is replaced.
        // In this method we do everything replaceText method of EditableInputConnection does, apart from actually
        // replacing text. Instead we copy the suggestions from incoming text into editor directly.
        //
        // We only take this path when the edited region actually contains a checklist span; for plain text there
        // is nothing to protect, so we fall through to the keyboard's normal autocorrect/suggestion handling
        // instead of swallowing the correction (which previously broke autocorrect on every note).
        if (incomingTextHasSuggestions && editedRegionHasCheckableSpan()) {
            AppLog.add(
                AppLog.Type.EDITOR,
                "Detected spellchecker trying to commit partial text with suggestions"
            )
            // delete composing text set previously.
            var composingSpanStart = getComposingSpanStart(editable)
            var composingSpanEnd = getComposingSpanEnd(editable)

            if (composingSpanEnd < composingSpanStart) {
                val tmp = composingSpanStart
                composingSpanStart = composingSpanEnd
                composingSpanEnd = tmp
            }

            if (composingSpanStart != -1 && composingSpanEnd != -1) {
                removeComposingSpans(editable)
            } else {
                composingSpanStart = Selection.getSelectionStart(editable)
                composingSpanEnd = Selection.getSelectionEnd(editable)
                if (composingSpanStart < 0) composingSpanStart = 0
                if (composingSpanEnd < 0) composingSpanEnd = 0
                if (composingSpanEnd < composingSpanStart) {
                    val tmp = composingSpanStart
                    composingSpanStart = composingSpanEnd
                    composingSpanEnd = tmp
                }
            }

            var cursorPosition = newCursorPosition
            cursorPosition += if (cursorPosition > 0) {
                composingSpanEnd - 1
            } else {
                composingSpanStart
            }
            if (newCursorPosition < 0) cursorPosition = 0
            if (newCursorPosition > editable.length) cursorPosition = editable.length
            Selection.setSelection(editable, cursorPosition)

            (text as Spanned).getSpans(0, text.length, SuggestionSpan::class.java).forEach {
                val st: Int = text.getSpanStart(it)
                val en: Int = text.getSpanEnd(it)
                val fl: Int = text.getSpanFlags(it)

                if (editable.length > composingSpanStart + en) {
                    editable.setSpan(it, composingSpanStart + st, composingSpanStart + en, fl)
                }
            }

            return true
        }

        return baseInputConnection.commitText(text, newCursorPosition)
    }

    override fun commitContent(inputContentInfo: InputContentInfo, flags: Int, opts: Bundle?): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            baseInputConnection.commitContent(inputContentInfo, flags, opts)
        } else {
            super.commitContent(inputContentInfo, flags, opts)
        }
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        return baseInputConnection.deleteSurroundingText(beforeLength, afterLength)
    }

    override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean {
        return baseInputConnection.requestCursorUpdates(cursorUpdateMode)
    }

    override fun reportFullscreenMode(enabled: Boolean): Boolean {
        return baseInputConnection.reportFullscreenMode(enabled)
    }

    override fun setSelection(start: Int, end: Int): Boolean {
        return baseInputConnection.setSelection(start, end)
    }

    override fun finishComposingText(): Boolean {
        return baseInputConnection.finishComposingText()
    }

    override fun setComposingRegion(start: Int, end: Int): Boolean {
        return baseInputConnection.setComposingRegion(start, end)
    }

    override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            baseInputConnection.deleteSurroundingTextInCodePoints(beforeLength, afterLength)
        } else {
            // We should not reach this code on Android < N, but just in case we do, we call the old method.
            baseInputConnection.deleteSurroundingText(beforeLength, afterLength)
        }
    }

    override fun getCursorCapsMode(reqModes: Int): Int {
        return baseInputConnection.getCursorCapsMode(reqModes)
    }

    override fun getSelectedText(flags: Int): CharSequence? {
        return baseInputConnection.getSelectedText(flags)
    }

    override fun getTextAfterCursor(length: Int, flags: Int): CharSequence? {
        return baseInputConnection.getTextAfterCursor(length, flags)
    }

    override fun getTextBeforeCursor(length: Int, flags: Int): CharSequence? {
        return baseInputConnection.getTextBeforeCursor(length, flags)
    }
}
