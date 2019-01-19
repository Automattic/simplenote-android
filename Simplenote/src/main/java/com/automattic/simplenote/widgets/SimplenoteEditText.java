package com.automattic.simplenote.widgets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.AttributeSet;

import com.automattic.simplenote.NotesActivity;
import com.automattic.simplenote.R;
import com.automattic.simplenote.utils.ChecklistUtils;
import com.automattic.simplenote.utils.DisplayUtils;
import com.automattic.simplenote.utils.DrawableUtils;
import com.automattic.simplenote.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.List;

public class SimplenoteEditText extends AppCompatEditText {
    private List<OnSelectionChangedListener> listeners;
    private final int CHECKBOX_LENGTH = 2; // One CheckableSpan + a space character

    public SimplenoteEditText(Context context) {
        super(context);
        listeners = new ArrayList<>();
        setTypeface(TypefaceCache.getTypeface(context, TypefaceCache.TYPEFACE_NAME_ROBOTO_REGULAR));
    }

    public SimplenoteEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        listeners = new ArrayList<>();
        setTypeface(TypefaceCache.getTypeface(context, TypefaceCache.TYPEFACE_NAME_ROBOTO_REGULAR));
    }

    public SimplenoteEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        listeners = new ArrayList<>();
        setTypeface(TypefaceCache.getTypeface(context, TypefaceCache.TYPEFACE_NAME_ROBOTO_REGULAR));
    }

    public void addOnSelectionChangedListener(OnSelectionChangedListener o) {
        listeners.add(o);
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (listeners != null) {
            for (OnSelectionChangedListener l : listeners)
                l.onSelectionChanged(selStart, selEnd);
        }
    }

    // Updates the ImageSpan drawable to the new checked state
    public void toggleCheckbox(CheckableSpan checkableSpan) {
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder(getText());

        int checkboxStart = stringBuilder.getSpanStart(checkableSpan);
        int checkboxEnd = stringBuilder.getSpanEnd(checkableSpan);

        final int selectionStart = getSelectionStart();
        final int selectionEnd = getSelectionEnd();

        ImageSpan[] imageSpans = stringBuilder.getSpans(checkboxStart, checkboxEnd, ImageSpan.class);
        if (imageSpans.length > 0) {
            // ImageSpans are static, so we need to remove the old one and replace :|
            stringBuilder.removeSpan(imageSpans[0]);

            Drawable iconDrawable = getContext().getResources().getDrawable(
                    checkableSpan.isChecked()
                            ? R.drawable.ic_check_box_24px
                            : R.drawable.ic_check_box_outline_blank_24px);
            iconDrawable = DrawableUtils.tintDrawableWithResource(getContext(), iconDrawable, R.color.simplenote_text_preview);
            int iconSize = DisplayUtils.getChecklistIconSize(getContext());
            iconDrawable.setBounds(0, 0, iconSize, iconSize);
            CenteredImageSpan newImageSpan = new CenteredImageSpan(getContext(), iconDrawable);
            stringBuilder.setSpan(newImageSpan, checkboxStart, checkboxEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            setText(stringBuilder);
            if (selectionStart < 0) {
                // Prevents soft keyboard from showing when clicking on a checkbox
                clearFocus();
            } else {
                // Restore the selection if necessary
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        setSelection(selectionStart, selectionEnd);
                    }
                });
            }
        }
    }

    // Returns the line position of the text cursor
    private int getCurrentCursorLine() {
        int selectionStart = getSelectionStart();
        Layout layout = getLayout();

        if (!(selectionStart == -1)) {
            return layout.getLineForOffset(selectionStart);
        }

        return 0;
    }

    public void insertChecklist() {
        int start, end;
        int lineNumber = getCurrentCursorLine();
        start = getLayout().getLineStart(lineNumber);
        if (getSelectionEnd() > getSelectionStart() && !selectionIsOnSameLine()) {
            end = getSelectionEnd();
        } else {
            end = getLayout().getLineEnd(lineNumber);
        }

        SpannableStringBuilder workingString = new SpannableStringBuilder(getText().subSequence(start, end));

        int previousSelection = getSelectionStart();
        CheckableSpan[] checkableSpans = workingString.getSpans(0, workingString.length(), CheckableSpan.class);
        if (checkableSpans.length > 0) {
            // Remove any checkablespans found
            for(CheckableSpan span: checkableSpans) {
                workingString.replace(
                        workingString.getSpanStart(span),
                        workingString.getSpanEnd(span) + 1,
                        ""
                );
                workingString.removeSpan(span);
            }

            getText().replace(start, end, workingString);

            if (checkableSpans.length == 1) {
                int newSelection = Math.max(previousSelection, 0) - CHECKBOX_LENGTH;
                if (getText().length() >= newSelection) {
                    setSelection(newSelection);
                }
            }
        } else {
            // Insert a checklist for each line
            String[] lines = workingString.toString().split("(?<=\n)");
            StringBuilder resultString = new StringBuilder();

            for (String lineString: lines) {
                resultString
                        .append(ChecklistUtils.UNCHECKED_MARKDOWN)
                        .append(" ")
                        .append(lineString);
            }

            getText().replace(start, end, resultString, 0, resultString.length());

            int newSelection = Math.max(previousSelection, 0) + (lines.length * CHECKBOX_LENGTH);
            if (getText().length() >= newSelection) {
                setSelection(newSelection);
            }
        }
    }

    // Returns true if the current editor selection is on the same line
    private boolean selectionIsOnSameLine() {
        int selectionStart = getSelectionStart();
        int selectionEnd = getSelectionEnd();
        Layout layout = getLayout();

        if (selectionStart >= 0 && selectionEnd >= 0) {
            return layout.getLineForOffset(selectionStart) == layout.getLineForOffset(selectionEnd);
        }

        return false;
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selStart, int selEnd);
    }

    // Replaces any CheckableSpans with their markdown counterpart (e.g. '- [ ]')
    public String getPlainTextContent() {
        if (getText() == null) {
            return "";
        }

        SpannableStringBuilder content = new SpannableStringBuilder(getText());
        CheckableSpan[] spans = content.getSpans(0, content.length(), CheckableSpan.class);

        for(CheckableSpan span: spans) {
            int start = content.getSpanStart(span);
            int end = content.getSpanEnd(span);
            ((Editable) content).replace(
                    start,
                    end,
                    span.isChecked() ? ChecklistUtils.CHECKED_MARKDOWN : ChecklistUtils.UNCHECKED_MARKDOWN);
        }

        return content.toString();
    }

    public void processChecklists() {
        if (getText().length() == 0 || getContext() == null) {
            return;
        }

        ChecklistUtils.addChecklistSpansForRegexAndColor(
                getContext(),
                getText(),
                ChecklistUtils.CHECKLIST_REGEX_LINE_START,
                R.color.simplenote_text_preview);
    }
}
