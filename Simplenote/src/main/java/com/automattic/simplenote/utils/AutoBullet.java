package com.automattic.simplenote.utils;

import android.text.Editable;

import com.automattic.simplenote.widgets.CheckableSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.automattic.simplenote.utils.ChecklistUtils.CHAR_BULLET;
import static com.automattic.simplenote.utils.ChecklistUtils.CHAR_NO_BREAK_SPACE;

public class AutoBullet {
    private static final String PATTERN_BULLET = "^([\\s]*)([-*+" + CHAR_BULLET + CHAR_NO_BREAK_SPACE + "])[\\s]+(.*)$";
    private static final String STR_LINE_BREAK = System.getProperty("line.separator");
    private static final String STR_SPACE = " ";

    public static void apply(Editable editable, int oldCursorPosition, int newCursorPosition) {
        if (!isValidCursorIncrement(oldCursorPosition, newCursorPosition)) {
            return;
        }

        String noteContent = editable.toString();
        String prevChar = noteContent.substring(newCursorPosition - 1, newCursorPosition);

        if (prevChar.equals(STR_LINE_BREAK)) {
            int prevParagraphEnd = newCursorPosition - 1;
            int prevParagraphStart = noteContent.lastIndexOf(STR_LINE_BREAK, prevParagraphEnd - 1);
            prevParagraphStart++; // ++ because we don't actually include the previous linebreak
            String prevParagraph = noteContent.substring(prevParagraphStart, prevParagraphEnd);
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
    }

    private static boolean isValidCursorIncrement(int oldCursorPosition, int newCursorPosition) {
        return newCursorPosition > 0 && newCursorPosition > oldCursorPosition;
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
