package com.automattic.simplenote.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;

import com.automattic.simplenote.R;
import com.automattic.simplenote.widgets.CenteredImageSpan;
import com.automattic.simplenote.widgets.CheckableSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChecklistUtils {

    public static String CHECKLIST_REGEX = "- (\\[([ |x])\\])";
    public static String CHECKLIST_REGEX_LINE_START = "^- (\\[([ |x])\\])";
    public static String CHECKED_MARKDOWN = "- [x]";
    public static String UNCHECKED_MARKDOWN = "- [ ]";
    public static final int CHECKLIST_OFFSET = 4;

    /***
     * Adds CheckableSpans for matching markdown formatted checklists.
     * @param context view content.
     * @param spannable the spannable string to run the regex against.
     * @param regex the regex pattern, use either CHECKLIST_REGEX or CHECKLIST_REGEX_LINE_START
     * @param color the resource id of the color to tint the checklist item
     * @return ChecklistResult - resulting spannable string and result boolean
     */
    public static ChecklistResult addChecklistSpansForRegexAndColor(
            Context context,
            SpannableStringBuilder spannable,
            String regex, int color) {
        if (spannable == null) {
            return new ChecklistResult(new SpannableStringBuilder(""), false);
        }

        Pattern p = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher m = p.matcher(spannable);

        int positionAdjustment = 0;
        boolean result = false;
        while(m.find()) {
            result = true;
            int start = m.start() - positionAdjustment;
            int end = m.end() - positionAdjustment;

            // Safety first!
            if (end >= spannable.length()) {
                continue;
            }

            String match = m.group(1);
            CheckableSpan checkableSpan = new CheckableSpan();
            checkableSpan.setChecked(match.contains("x"));
            spannable.replace(start, end, " ");

            Drawable iconDrawable = context.getResources().getDrawable(checkableSpan.isChecked() ? R.drawable.ic_checked : R.drawable.ic_unchecked);
            iconDrawable = DrawableUtils.tintDrawableWithResource(context, iconDrawable, color);
            int iconSize = DisplayUtils.getChecklistIconSize(context);
            iconDrawable.setBounds(0, 0, iconSize, iconSize);

            CenteredImageSpan imageSpan = new CenteredImageSpan(iconDrawable);
            spannable.setSpan(imageSpan, start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(checkableSpan, start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            positionAdjustment += (end - start) - 1;
        }

        return new ChecklistResult(spannable, result);
    }

    public static class ChecklistResult {
        public final SpannableStringBuilder resultStringBuilder;
        public final boolean addedChecklists;

        ChecklistResult(SpannableStringBuilder stringBuilder, boolean result) {
            resultStringBuilder = stringBuilder;
            addedChecklists = result;
        }
    }
}


