package com.automattic.simplenote.widgets;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.util.AttributeSet;

import com.automattic.simplenote.R;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.text.style.DynamicDrawableSpan.ALIGN_BASELINE;

public class SimplenoteEditText extends AppCompatEditText {

    private static String ChecklistRegex = "^- (\\[([ |x])\\])";
    private static String UncheckedMarkdown = "- [ ]";
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
            ImageSpan newImageSpan = new ImageSpan(
                    getContext(),
                    checkableSpan.isChecked() ? R.drawable.ic_checked : R.drawable.ic_unchecked,
                    ALIGN_BASELINE);
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
            ((Editable) content).replace(start, end, span.isChecked() ? "- [x]" : "- [ ]");
        }

        return content.toString();
    }

    public void processChecklists() {
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder(getText());
        if (stringBuilder.length() == 0 || mContext == null) {
            return;
        }

        Pattern p = Pattern.compile(ChecklistRegex, Pattern.MULTILINE);
        Matcher m = p.matcher(stringBuilder);

        int positionAdjustment = 0;
        int count = 0;
        while(m.find()) {
            count++;
            int start = m.start() - positionAdjustment;
            int end = m.end() - positionAdjustment;
            String match = m.group(1);
            CheckableSpan clickableSpan = new CheckableSpan();
            clickableSpan.setChecked(match.contains("x"));
            stringBuilder.replace(start, end, " ");
            ImageSpan imageSpan = new ImageSpan(
                    getContext(),
                    clickableSpan.isChecked() ? R.drawable.ic_checked : R.drawable.ic_unchecked,
                    ALIGN_BASELINE);
            stringBuilder.setSpan(imageSpan, start, start + 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            stringBuilder.setSpan(clickableSpan, start, start + 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            positionAdjustment += end - start - 1;
        }

        if (count > 0) {
            setText(stringBuilder);
        }
    }
}
