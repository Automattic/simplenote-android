package com.automattic.simplenote.widgets

import android.text.Editable
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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
@SmallTest
class SimplenoteEditTextChecklistLayoutTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun checklistConvertedDuringTextChangeKeepsLayoutInSyncWithText() {
        var textLength = 0
        var layoutEnd = 0
        var isChecked = false

        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, R.style.Style_Default)
            val editText = SimplenoteEditText(context)
            editText.layoutParams = ViewGroup.LayoutParams(WIDTH, ViewGroup.LayoutParams.WRAP_CONTENT)
            editText.setText("Title line\nBody - [ ] one\nmore text here\n- [x] two\nend")
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

            // The cursor is at the start of the text after setText().
            editText.onCreateInputConnection(EditorInfo())!!.commitText("- [x] ", 1)

            val text = editText.text!!
            val layout = editText.layout
            textLength = text.length
            layoutEnd = layout.getLineEnd(layout.lineCount - 1)
            isChecked = text.getSpans(0, 1, CheckableSpan::class.java).single().isChecked
        }

        assertTrue(isChecked)
        assertEquals(textLength, layoutEnd)
    }

    private companion object {
        const val WIDTH = 1000
    }
}
