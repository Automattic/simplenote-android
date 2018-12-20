package com.automattic.simplenote.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.AttributeSet;

import com.automattic.simplenote.R;
import com.automattic.simplenote.utils.ChecklistUtils;
import com.automattic.simplenote.utils.DisplayUtils;
import com.automattic.simplenote.utils.DrawableUtils;
import com.automattic.simplenote.utils.PrefUtils;
import com.automattic.simplenote.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.List;

public class SimplenoteEditText extends AppCompatEditText {
    private Context mContext;

    private List<OnSelectionChangedListener> listeners;

    public SimplenoteEditText(Context context) {
        super(context);
        mContext = context;
        listeners = new ArrayList<>();
        setTypeface(TypefaceCache.getTypeface(context, TypefaceCache.TYPEFACE_NAME_ROBOTO_REGULAR));
    }

    public SimplenoteEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        listeners = new ArrayList<>();
        setTypeface(TypefaceCache.getTypeface(context, TypefaceCache.TYPEFACE_NAME_ROBOTO_REGULAR));
    }

    public SimplenoteEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        listeners = new ArrayList<>();
        setTypeface(TypefaceCache.getTypeface(context, TypefaceCache.TYPEFACE_NAME_ROBOTO_REGULAR));
    }

    public void addOnSelectionChangedListener(OnSelectionChangedListener o) {
        listeners.add(o);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);

        processChecklists();
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

        ImageSpan[] imageSpans = stringBuilder.getSpans(checkboxStart, checkboxEnd, ImageSpan.class);
        if (imageSpans.length > 0) {
            // ImageSpans are static, so we need to remove the old one and replace :|
            stringBuilder.removeSpan(imageSpans[0]);

            Drawable iconDrawable = mContext.getResources().getDrawable(checkableSpan.isChecked() ? R.drawable.ic_checked : R.drawable.ic_unchecked);
            iconDrawable = DrawableUtils.tintDrawableWithResource(mContext, iconDrawable, ThemeUtils.getThemeTextColorId(mContext));
            int iconSize = DisplayUtils.getChecklistIconSize(mContext);
            iconDrawable.setBounds(0, 0, iconSize, iconSize);
            CenteredImageSpan newImageSpan = new CenteredImageSpan(iconDrawable);
            stringBuilder.setSpan(newImageSpan, checkboxStart, checkboxEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            setText(stringBuilder);
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
                    span.isChecked() ? ChecklistUtils.CheckedMarkdown : ChecklistUtils.UncheckedMarkdown);
        }

        return content.toString();
    }

    public void processChecklists() {
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder(getText());
        if (stringBuilder.length() == 0 || mContext == null) {
            return;
        }

        int[] attrs = {R.attr.noteEditorTextColor};
        TypedArray ta = mContext.obtainStyledAttributes(attrs);
        int tintColor = ta.getResourceId(0, android.R.color.black);

        stringBuilder = ChecklistUtils.addChecklistSpansForRegexAndColor(
                getContext(),
                stringBuilder,
                ChecklistUtils.ChecklistRegexLineStart,
                tintColor);
        if (stringBuilder != null) {
            setText(stringBuilder);
        }
    }
}
