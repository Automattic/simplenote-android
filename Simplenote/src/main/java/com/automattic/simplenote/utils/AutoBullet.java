package com.automattic.simplenote.utils;

import android.text.Editable;

import com.automattic.simplenote.widgets.CheckableSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoBullet {

    private static final String PATTERN_BULLET = "^([\\s]*)(-|\\*|\\+)[\\s]+(.*)$";
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

            // See if there's a CheckableSpan at the previous paragraph start
            CheckableSpan[] checkableSpans = editable.getSpans(prevParagraphStart, prevParagraphStart + 1, CheckableSpan.class);
            if (checkableSpans.length > 0) {
                if (prevParagraph.equals(STR_SPACE + STR_SPACE)) {
                    // Empty checklist item, insert line break
                    editable.replace(prevParagraphStart, newCursorPosition, STR_LINE_BREAK);
                } else {
                    // We can add a new checkbox!
                    editable.insert(newCursorPosition, ChecklistUtils.UncheckedMarkdown + STR_SPACE);
                }
                return;
            }

            if (metadata.isBullet) {
                String bullet;

                if (!metadata.isEmptyBullet) {
                    bullet = buildBullet(metadata);
                    editable.insert(newCursorPosition, bullet);
                } else {
                    if (metadata.numSpacesPrefixed > 0) {
                        metadata.numSpacesPrefixed -= 1;
                        bullet = buildBullet(metadata);
                    } else {
                        bullet = STR_LINE_BREAK;
                    }

                    editable.replace(prevParagraphStart, newCursorPosition, bullet);
                }
            }
        }
    }

    private static boolean isValidCursorIncrement(int oldCursorPosition, int newCursorPosition) {
        return newCursorPosition > 0 && newCursorPosition > oldCursorPosition;
    }

    private static String buildBullet(BulletMetadata metadata) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < metadata.numSpacesPrefixed; i++) {
            sb.append(STR_SPACE);
        }
        sb.append(metadata.bulletChar);
        sb.append(STR_SPACE);
        return sb.toString();
    }

    private static BulletMetadata extractBulletMetadata(String input) {
        BulletMetadata metadata = new BulletMetadata();

        Pattern pattern = Pattern.compile(PATTERN_BULLET);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            metadata.isBullet = true;
            metadata.numSpacesPrefixed = matcher.group(1).length();
            metadata.bulletChar = matcher.group(2);
            metadata.isEmptyBullet = matcher.group(3).trim().isEmpty();
        }

        return metadata;
    }

    private static class BulletMetadata {
        public boolean isBullet = false;
        public int numSpacesPrefixed;
        public String bulletChar;
        public boolean isEmptyBullet = false;
    }
}
