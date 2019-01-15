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

            Drawable iconDrawable = getContext().getResources().getDrawable(checkableSpan.isChecked() ? R.drawable.ic_checked : R.drawable.ic_unchecked);
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
        int line = getCurrentCursorLine();
        int start = getLayout().getLineStart(line);
        int end = getLayout().getLineEnd(line);

        boolean shouldAdjustCursor = false;
        SpannableStringBuilder currentLine = new SpannableStringBuilder(getText().subSequence(start, end));
        if (currentLine.getSpans(0, 0, CheckableSpan.class).length > 0) {
            currentLine.replace(0, CHECKBOX_LENGTH, "");
        } else {
            shouldAdjustCursor = true;
            String newChecklistString = ChecklistUtils.UNCHECKED_MARKDOWN + " ";
            currentLine.insert(0, newChecklistString);
        }

        getText().replace(start, end, currentLine, 0, currentLine.length());

        // Adjust cursor position if necessary
        if (shouldAdjustCursor) {
            int newSelection = Math.max(getSelectionStart(), 0) + CHECKBOX_LENGTH;
            if (getText().length() >= newSelection) {
                setSelection(newSelection);
            }
        }
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
