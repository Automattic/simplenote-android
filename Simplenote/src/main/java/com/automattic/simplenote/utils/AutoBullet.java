package com.automattic.simplenote.utils;

import android.text.Editable;

import com.automattic.simplenote.widgets.CheckableSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.automattic.simplenote.utils.ChecklistUtils.CHAR_BULLET;
import static com.automattic.simplenote.utils.ChecklistUtils.CHAR_NO_BREAK_SPACE;

public class AutoBullet {
    private static final String PATTERN_BULLET = "^([\\s]*)([-*+" + CHAR_BULLET + CHAR_NO_BREAK_SPACE + "])[\\s]+(.*)$";
    private static final String STR_SPACE = " ";

    public static void apply(Editable editable, int oldCursorPosition, int newCursorPosition) {
        if (!isValidCursorIncrement(editable, oldCursorPosition, newCursorPosition)) {
            return;
        }

        if (editable.charAt(newCursorPosition - 1) != '\n') {
            return;
        }

        int prevParagraphEnd = newCursorPosition - 1;
        int prevParagraphStart = 0;
        for (int i = prevParagraphEnd - 1; i >= 0; i--) {
            if (editable.charAt(i) == '\n') {
                prevParagraphStart = i + 1;
                break;
            }
        }

        String prevParagraph = editable.subSequence(prevParagraphStart, prevParagraphEnd).toString();
        BulletMetadata metadata = extractBulletMetadata(prevParagraph);
        // See if there's a CheckableSpan in the previous line
        CheckableSpan[] checkableSpans = editable.getSpans(prevParagraphStart, prevParagraphEnd, CheckableSpan.class);

        if (checkableSpans.length > 0) {
            if (prevParagraph.trim().equalsIgnoreCase(String.valueOf(CHAR_NO_BREAK_SPACE))) {
                // Empty checklist item, remove and place cursor at start of line
                editable.replace(prevParagraphStart, newCursorPosition, "");
            } else {
                // We can add a new checkbox!
                String leadingWhitespace = metadata.leadingWhitespace != null ? metadata.leadingWhitespace : "";
                editable.insert(newCursorPosition, leadingWhitespace + ChecklistUtils.UNCHECKED_MARKDOWN + STR_SPACE);
            }

            return;
        }

        if (metadata.isBullet) {
            if (!metadata.isEmptyBullet) {
                editable.insert(newCursorPosition, buildBullet(metadata));
            } else {
                editable.replace(prevParagraphStart, newCursorPosition, "");
            }
        }
    }

    private static boolean isValidCursorIncrement(Editable editable, int oldCursorPosition, int newCursorPosition) {
        return editable != null && newCursorPosition > 0 && newCursorPosition <= editable.length() && newCursorPosition > oldCursorPosition;
    }

    private static String buildBullet(BulletMetadata metadata) {
        return metadata.leadingWhitespace + metadata.bulletChar + STR_SPACE;
    }

    private static BulletMetadata extractBulletMetadata(String input) {
        BulletMetadata metadata = new BulletMetadata();

        Pattern pattern = Pattern.compile(PATTERN_BULLET);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            metadata.isBullet = true;
            metadata.leadingWhitespace = matcher.group(1);
            metadata.bulletChar = matcher.group(2);
            metadata.isEmptyBullet = matcher.group(3).trim().isEmpty();
        }

        return metadata;
    }

    private static class BulletMetadata {
        String bulletChar;
        String leadingWhitespace;
        boolean isBullet = false;
        boolean isEmptyBullet = false;
    }
}
