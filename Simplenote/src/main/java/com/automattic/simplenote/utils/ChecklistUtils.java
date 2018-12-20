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

    public static String ChecklistRegex = "- (\\[([ |x])\\])";
    public static String ChecklistRegexLineStart = "^- (\\[([ |x])\\])";
    public static String CheckedMarkdown = "- [x]";
    public static String UncheckedMarkdown = "- [ ]";

    public static SpannableStringBuilder addChecklistSpansForRegexAndColor(
            Context context,
            SpannableStringBuilder spannable,
            String regex, int color) {
        Pattern p = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher m = p.matcher(spannable);

        int positionAdjustment = 0;
        int count = 0;
        while(m.find()) {
            count++;
            int start = m.start() - positionAdjustment;
            int end = m.end() - positionAdjustment;
            String match = m.group(1);
            CheckableSpan checkableSpan = new CheckableSpan();
            checkableSpan.setChecked(match.contains("x"));
            spannable.replace(start, end, " ");

            Drawable iconDrawable = context.getResources().getDrawable(checkableSpan.isChecked() ? R.drawable.ic_checked : R.drawable.ic_unchecked);
            iconDrawable = DrawableUtils.tintDrawableWithResource(context, iconDrawable, color);
            int iconSize = DisplayUtils.getChecklistIconSize(context);
            iconDrawable.setBounds(0, 0, iconSize, iconSize);

            CenteredImageSpan imageSpan = new CenteredImageSpan(iconDrawable);
            spannable.setSpan(imageSpan, start, start + 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            spannable.setSpan(checkableSpan, start, start + 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            positionAdjustment += (end - start) - 1;
        }

        if (count > 0) {
            return spannable;
        }

        return null;
    }
}
