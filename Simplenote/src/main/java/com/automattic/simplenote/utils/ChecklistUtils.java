package com.automattic.simplenote.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import com.automattic.simplenote.R;
import com.automattic.simplenote.widgets.CenteredImageSpan;
import com.automattic.simplenote.widgets.CheckableSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChecklistUtils {
    public static final char CHAR_BALLOT_BOX = '\u2610';
    public static final char CHAR_BALLOT_BOX_CHECK = '\u2611';
    public static final char CHAR_BULLET = '\u2022';
    public static final char CHAR_NO_BREAK_SPACE = '\u00a0';
    public static final char CHAR_VECTOR_CROSS_PRODUCT = '\u2a2f';
    public static final int CHECKLIST_OFFSET = 3;

    public static String CHECKLIST_REGEX = "(\\s+)?(-[ \\t]+\\[[xX\\s]?\\])";
    public static String CHECKLIST_REGEX_LINES = "^(\\s+)?(-[ \\t]+\\[[xX\\s]?\\])";
    public static String CHECKLIST_REGEX_LINES_CHECKED = "^(\\s+)?(-[ \\t]+\\[[xX]?\\])";
    public static String CHECKLIST_REGEX_LINES_UNCHECKED = "^(\\s+)?(-[ \\t]+\\[[\\s]?\\])";
    public static String CHECKED_MARKDOWN_PREVIEW = "- [" + CHAR_VECTOR_CROSS_PRODUCT + "]";
    public static String CHECKED_MARKDOWN = "- [x]";
    public static String UNCHECKED_MARKDOWN = "- [ ]";

    /***
     * Adds CheckableSpans for matching markdown formatted checklists.
     *
     * @param context   {@link Context} from which to get the checkbox drawable, color, and size.
     * @param editable  {@link Editable} spannable string to match with the regular expression.
     * @param regex     {@link String} regular expression; CHECKLIST_REGEX or CHECKLIST_REGEX_LINES.
     * @param color     {@link Integer} resource id of the color to tint the checkbox.
     * @param isList    {@link Boolean} if checkbox is in list to determine size.
     *
     * @return          {@link Editable} spannable string with checkbox spans.
     */
    public static Editable addChecklistSpansForRegexAndColor(Context context, Editable editable, String regex, int color, boolean isList) {
        if (editable == null) {
            return new SpannableStringBuilder("");
        }

        Pattern p = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher m = p.matcher(editable);
        int positionAdjustment = 0;

        while (m.find()) {
            int start = m.start() - positionAdjustment;
            int end = m.end() - positionAdjustment;

            // Safety first!
            if (end > editable.length()) {
                continue;
            }

            String leadingSpaces = m.group(1);
            String match = m.group(2);

            if (!TextUtils.isEmpty(leadingSpaces)) {
                start += leadingSpaces.length();
            }

            if (match == null) {
                continue;
            }

            CheckableSpan checkableSpan = new CheckableSpan();
            checkableSpan.setChecked(match.contains("x") || match.contains("X"));
            editable.replace(start, end, String.valueOf(CHAR_NO_BREAK_SPACE));

            Drawable iconDrawable = ContextCompat.getDrawable(
                context,
                checkableSpan.isChecked()
                    ? isList ? R.drawable.ic_checkbox_list_checked_24dp : R.drawable.ic_checkbox_editor_checked_24dp
                    : isList ? R.drawable.ic_checkbox_list_unchecked_24dp : R.drawable.ic_checkbox_editor_unchecked_24dp
            );
            iconDrawable = DrawableUtils.tintDrawableWithResource(context, iconDrawable, color);
            int iconSize = DisplayUtils.getChecklistIconSize(context, isList);
            iconDrawable.setBounds(0, 0, iconSize, iconSize);

            CenteredImageSpan imageSpan = new CenteredImageSpan(context, iconDrawable);
            editable.setSpan(imageSpan, start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            editable.setSpan(checkableSpan, start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            positionAdjustment += (end - start) - 1;
        }

        return editable;
    }

    /***
     * Adds CheckableSpans for matching markdown formatted checklists.
     * @param editable the spannable string to run the regex against.
     * @param regex the regex pattern, use either CHECKLIST_REGEX or CHECKLIST_REGEX_LINES
     * @return Editable - resulting spannable string
     */
    public static Editable addChecklistUnicodeSpansForRegex(Editable editable, String regex) {
        if (editable == null) {
            return new SpannableStringBuilder("");
        }

        Pattern p = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher m = p.matcher(editable);

        int positionAdjustment = 0;
        while(m.find()) {
            int start = m.start() - positionAdjustment;
            int end = m.end() - positionAdjustment;

            // Safety first!
            if (end > editable.length()) {
                continue;
            }

            String leadingSpaces = m.group(1);
            if (!TextUtils.isEmpty(leadingSpaces)) {
                start += leadingSpaces.length();
            }

            String match = m.group(2);
            if (match == null) {
                continue;
            }

            CheckableSpan checkableSpan = new CheckableSpan();
            checkableSpan.setChecked(match.contains("x") || match.contains("X"));
            editable.replace(start, end, checkableSpan.isChecked() ? String.valueOf(CHAR_BALLOT_BOX_CHECK) : String.valueOf(CHAR_BALLOT_BOX));
            editable.setSpan(checkableSpan, start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            positionAdjustment += (end - start) - 1;
        }

        return editable;
    }
}
