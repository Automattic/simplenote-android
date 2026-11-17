package com.automattic.simplenote.widgets

import android.text.Editable
import android.text.Layout
import android.text.TextWatcher
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.test.filters.SmallTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import com.automattic.simplenote.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A checklist conversion runs inside the text change that adds the markdown. After a pure insertion at offset 0,
 * the conversion could miss the layout, which then kept line offsets beyond the end of the text (#1914).
 */
@RunWith(AndroidJUnit4ClassRunner::class)
@SmallTest
class SimplenoteEditTextChecklistLayoutTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun imeCommitAtStartOfNoteKeepsLayoutInSyncWithText() {
        val result = editAtStartOfNote("Title line\n$BODY") { editText ->
            editText.onCreateInputConnection(EditorInfo())?.commitText("- [x] ", 1)
        }

        result.assertLayoutInSync()
        assertEquals(listOf(true), result.checkboxesAtStart)
    }

    @Test
    fun checklistButtonOnEmptyFirstLineKeepsLayoutInSyncWithText() {
        val result = editAtStartOfNote("\n$BODY") { it.insertChecklist() }

        result.assertLayoutInSync()
        assertEquals(listOf(false), result.checkboxesAtStart)
    }

    @Test
    fun checklistButtonOnOneCharacterFirstLineKeepsLayoutInSyncWithText() {
        val result = editAtStartOfNote("a\n$BODY") { it.insertChecklist() }

        result.assertLayoutInSync()
        assertEquals(listOf(false), result.checkboxesAtStart)
    }

    private fun editAtStartOfNote(content: String, edit: (SimplenoteEditText) -> Unit): EditResult {
        lateinit var result: EditResult

        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, R.style.Style_Default)
            val editText = SimplenoteEditText(context)
            editText.layoutParams = ViewGroup.LayoutParams(WIDTH, ViewGroup.LayoutParams.WRAP_CONTENT)
            editText.setLineSpacing(context.resources.getDimension(R.dimen.line_spacing), 1.0f)
            editText.setText(content)
            editText.measure(
                View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            editText.processChecklists()
            // Same as NoteEditorFragment.onTextChanged()
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    editText.removeTextChangedListener(this)
                    editText.processChecklists(start, count)
                    editText.addTextChangedListener(this)
                }

                override fun afterTextChanged(s: Editable) = Unit
            })
            editText.setSelection(0)
            val layoutBefore = editText.layout

            edit(editText)

            val text = editText.text
            val layout = editText.layout
            result = EditResult(
                layoutBefore = layoutBefore,
                layoutAfter = layout,
                textLength = text?.length ?: -1,
                layoutEnd = layout?.getLineEnd(layout.lineCount - 1) ?: -1,
                checkboxesAtStart = text?.getSpans(0, 1, CheckableSpan::class.java)?.map { it.isChecked }.orEmpty()
            )
        }

        return result
    }

    private class EditResult(
        val layoutBefore: Layout?,
        val layoutAfter: Layout?,
        val textLength: Int,
        val layoutEnd: Int,
        val checkboxesAtStart: List<Boolean>
    ) {
        fun assertLayoutInSync() {
            // A new layout means that SimplenoteEditText discarded an out-of-sync layout.
            assertSame("The editor discarded its layout", layoutBefore, layoutAfter)
            assertEquals("Layout end", textLength, layoutEnd)
        }
    }

    private companion object {
        const val WIDTH = 1000
        const val BODY = "Body - [ ] one\nmore text here\n- [x] two\nend"
    }
}
