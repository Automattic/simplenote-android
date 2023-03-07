package com.automattic.simplenote.widgets;

import static com.automattic.simplenote.utils.SimplenoteLinkify.SIMPLENOTE_LINK_ID;
import static com.automattic.simplenote.utils.SimplenoteLinkify.SIMPLENOTE_LINK_PREFIX;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.AdapterView;

import androidx.annotation.DrawableRes;
import androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView;

import com.automattic.simplenote.R;
import com.automattic.simplenote.analytics.AnalyticsTracker;
import com.automattic.simplenote.models.Note;
import com.automattic.simplenote.utils.AppLog;
import com.automattic.simplenote.utils.ChecklistUtils;
import com.automattic.simplenote.utils.DisplayUtils;
import com.automattic.simplenote.utils.DrawableUtils;
import com.automattic.simplenote.utils.LinkTokenizer;
import com.automattic.simplenote.utils.SimplenoteLinkify;
import com.automattic.simplenote.utils.ThemeUtils;
import com.simperium.client.Bucket;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimplenoteEditText extends AppCompatMultiAutoCompleteTextView implements AdapterView.OnItemClickListener {
    private static final Pattern INTERNOTE_LINK_PATTERN_EDIT = Pattern.compile("([^]]*)(]\\(" + SIMPLENOTE_LINK_PREFIX + SIMPLENOTE_LINK_ID + "\\))");
    private static final Pattern INTERNOTE_LINK_PATTERN_FULL = Pattern.compile("(?s)(.)*(\\[)" + INTERNOTE_LINK_PATTERN_EDIT);
    private static final int CHECKBOX_LENGTH = 2; // one ClickableSpan character + one space character

    private LinkTokenizer mTokenizer;
    private final List<OnSelectionChangedListener> listeners;
    private OnCheckboxToggledListener mOnCheckboxToggledListener;

    @Override
    public boolean enoughToFilter() {
        String substringCursor = getText().toString().substring(getSelectionEnd());
        Matcher matcherEdit = INTERNOTE_LINK_PATTERN_EDIT.matcher(substringCursor);

        // When an internote link title is being edited, don't show an autocomplete popup.
        if (matcherEdit.lookingAt()) {
            String substringEdit = substringCursor.substring(0, matcherEdit.end());
            Matcher matcherFull = INTERNOTE_LINK_PATTERN_FULL.matcher(substringEdit);

            if (!matcherFull.lookingAt()) {
                return false;
            }
        }

        Editable text = getText();
        int end = getSelectionEnd();

        if (end < 0) {
            return false;
        }

        int start = mTokenizer.findTokenStart(text, end);
        return start > 0 && end - start >= 1;
    }

    public interface OnCheckboxToggledListener {
        void onCheckboxToggled();
    }

    public SimplenoteEditText(Context context) {
        super(context);
        listeners = new ArrayList<>();
        setLinkTokenizer();
    }

    public SimplenoteEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        listeners = new ArrayList<>();
        setLinkTokenizer();
    }

    public SimplenoteEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        listeners = new ArrayList<>();
        setLinkTokenizer();
    }

    public void addOnSelectionChangedListener(OnSelectionChangedListener o) {
        listeners.add(o);
    }

    private void setLinkTokenizer() {
        mTokenizer = new LinkTokenizer();
        setOnItemClickListener(this);
        setTokenizer(mTokenizer);
        setThreshold(1);
    }

    private boolean shouldOverridePredictiveTextBehavior() {
        String currentKeyboard = Settings.Secure.getString(
                getContext().getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD
        );

        return "samsung".equals(Build.MANUFACTURER.toLowerCase(Locale.US)) && Build.VERSION.SDK_INT >= 33 &&
                (currentKeyboard != null && currentKeyboard.startsWith("com.samsung.android.honeyboard"));
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection baseInputConnection = super.onCreateInputConnection(outAttrs);

        if (shouldOverridePredictiveTextBehavior()) {
            AppLog.add(AppLog.Type.EDITOR, "Samsung keyboard detected, overriding predictive text behavior");
            return new SamsungInputConnection(this, baseInputConnection);
        }

        return baseInputConnection;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AnalyticsTracker.track(
                AnalyticsTracker.Stat.INTERNOTE_LINK_CREATED,
                AnalyticsTracker.CATEGORY_LINK,
                "internote_link_created"
        );
        @SuppressWarnings("unchecked")
        Bucket.ObjectCursor<Note> cursor = (Bucket.ObjectCursor<Note>) parent.getAdapter().getItem(position);
        String key = cursor.getString(cursor.getColumnIndexOrThrow(Note.KEY_PROPERTY)).replace("note", "");
        String text = SimplenoteLinkify.getNoteLink(key);
        int start = Math.max(getSelectionStart(), 0);
        int end = Math.max(getSelectionEnd(), 0);
        getEditableText().replace(Math.min(start, end), Math.max(start, end), text, 0, text.length());
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (listeners != null) {
            for (OnSelectionChangedListener l : listeners)
                l.onSelectionChanged(selStart, selEnd);
        }
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            clearFocus();
        }

        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (focused) {
            setCursorVisible(true);
        }

        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    // Updates the ImageSpan drawable to the new checked state
    public void toggleCheckbox(final CheckableSpan checkableSpan) {
        setCursorVisible(false);

        final Editable editable = getText();

        final int checkboxStart = editable.getSpanStart(checkableSpan);
        final int checkboxEnd = editable.getSpanEnd(checkableSpan);

        final int selectionStart = getSelectionStart();
        final int selectionEnd = getSelectionEnd();

        final ImageSpan[] imageSpans = editable.getSpans(checkboxStart, checkboxEnd, ImageSpan.class);
        if (imageSpans.length > 0) {
            Context context = getContext();
            // ImageSpans are static, so we need to remove the old one and replace :|
            @DrawableRes int resDrawable = checkableSpan.isChecked() ? R.drawable.ic_checkbox_editor_checked_24dp : R.drawable.ic_checkbox_editor_unchecked_24dp;
            Drawable iconDrawable = DrawableUtils.tintDrawableWithAttribute(context, resDrawable, checkableSpan.isChecked() ? R.attr.colorAccent : R.attr.notePreviewColor);
            int iconSize = DisplayUtils.getChecklistIconSize(context, false);
            iconDrawable.setBounds(0, 0, iconSize, iconSize);
            final CenteredImageSpan newImageSpan = new CenteredImageSpan(context, iconDrawable);
            new Handler().post(() -> {
                editable.setSpan(newImageSpan, checkboxStart, checkboxEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                editable.removeSpan(imageSpans[0]);
                fixLineSpacing();

                // Restore the selection
                if (selectionStart >= 0
                        && selectionStart <= editable.length()
                        && selectionEnd <= editable.length() && hasFocus()) {
                    setSelection(selectionStart, selectionEnd);
                    setCursorVisible(true);
                }

                if (mOnCheckboxToggledListener != null) {
                    mOnCheckboxToggledListener.onCheckboxToggled();
                }
            });
        }
    }

    private int findStartOfLineOfSelection() {
        int position = getSelectionStart();
        // getSelectionStart may return -1 if there is no selection nor cursor
        if (position == -1) {
            return 0;
        }
        Editable editable = getText();
        for (int i = position - 1; i >= 0; i--) {
            if (editable.charAt(i) == '\n') {
                return i + 1;
            }
        }
        return 0;
    }

    private int findEndOfLineOfSelection() {
        // getSelectionEnd may return -1 if there is no selection nor cursor
        // and as the minimum position is 0, use the max value between 0 and getSelectionEnd()
        int position = Math.max(0, getSelectionEnd());
        Editable editable = getText();
        for (int i = position; i < editable.length(); i++) {
            if (editable.charAt(i) == '\n') {
                // We return the max here, because when the cursor is at an empty line,
                // i-1 would point to the start of line
                return Math.max(i - 1, position);
            }
        }
        return editable.length();
    }

    public void insertChecklist() {
        final int start = findStartOfLineOfSelection();
        final int end = findEndOfLineOfSelection();

        SpannableStringBuilder workingString = new SpannableStringBuilder(getText().subSequence(start, end));
        Editable editable = getText();
        if (editable.length() < start || editable.length() < end) {
            return;
        }

        int previousSelection = getSelectionStart();
        CheckableSpan[] checkableSpans = workingString.getSpans(0, workingString.length(), CheckableSpan.class);
        if (checkableSpans.length > 0) {
            // Remove any CheckableSpans found
            for (CheckableSpan span: checkableSpans) {
                workingString.replace(
                        workingString.getSpanStart(span),
                        workingString.getSpanEnd(span) + 1,
                        ""
                );
                workingString.removeSpan(span);
            }

            editable.replace(start, end, workingString);

            if (checkableSpans.length == 1) {
                int newSelection = Math.max(previousSelection - CHECKBOX_LENGTH, 0);
                if (editable.length() >= newSelection) {
                    setSelection(newSelection);
                }
            }
        } else {
            // Insert a checklist for each line
            String[] lines = workingString.toString().split("(?<=\n)");
            StringBuilder resultString = new StringBuilder();

            for (String lineString: lines) {
                // Preserve the spaces before the text
                int leadingSpaceCount;
                if (lineString.trim().length() == 0) {
                    // Only whitespace content, get count of spaces
                    leadingSpaceCount = lineString.length() - lineString.replaceAll(" ", "").length();
                } else {
                    // Get count of spaces up to first non-whitespace character
                    leadingSpaceCount = lineString.indexOf(lineString.trim());
                }

                if (leadingSpaceCount > 0) {
                    resultString.append(new String(new char[leadingSpaceCount]).replace('\0', ' '));
                    lineString = lineString.substring(leadingSpaceCount);
                }

                resultString
                        .append(ChecklistUtils.UNCHECKED_MARKDOWN)
                        .append(" ")
                        .append(lineString);
            }

            editable.replace(start, end, resultString);

            int newSelection = Math.max(previousSelection, 0) + (lines.length * CHECKBOX_LENGTH);
            if (editable.length() >= newSelection) {
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

    public void fixLineSpacing() {
        // Prevents line heights from compacting
        // https://issuetracker.google.com/issues/37009353
        float lineSpacingExtra = getLineSpacingExtra();
        float lineSpacingMultiplier = getLineSpacingMultiplier();
        setLineSpacing(0.0f, 1.0f);
        setLineSpacing(lineSpacingExtra, lineSpacingMultiplier);
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

    // Replaces any CheckableSpans with their markdown preview counterpart (e.g. '- [\u2a2f]')
    public String getPreviewTextContent() {
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
                    span.isChecked() ? ChecklistUtils.CHECKED_MARKDOWN_PREVIEW : ChecklistUtils.UNCHECKED_MARKDOWN);
        }

        return content.toString();
    }


    public void processChecklists() {
        if (getText().length() == 0 || getContext() == null) {
            return;
        }

        try {
            ChecklistUtils.addChecklistSpansForRegexAndColor(
                getContext(),
                getText(),
                ChecklistUtils.CHECKLIST_REGEX_LINES_CHECKED,
                ThemeUtils.getColorResourceFromAttribute(getContext(), R.attr.colorAccent),
                false
            );
            ChecklistUtils.addChecklistSpansForRegexAndColor(
                getContext(),
                getText(),
                ChecklistUtils.CHECKLIST_REGEX_LINES_UNCHECKED,
                ThemeUtils.getColorResourceFromAttribute(getContext(), R.attr.notePreviewColor),
                false
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setOnCheckboxToggledListener(OnCheckboxToggledListener listener) {
        mOnCheckboxToggledListener = listener;
    }
}
