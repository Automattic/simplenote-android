package com.automattic.simplenote.utils;

import android.text.Editable;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static org.hamcrest.CoreMatchers.is;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AutoBulletTest {
    private Editable buildEditable(CharSequence source) {
        return Editable.Factory.getInstance().newEditable(source);
    }

    @Test
    public void testLineBreakOnly() {
        String source = "\n";
        String target = "\n";
        int oldPos = 0;
        int newPos = 1;

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);

        assertThat(target, is(editable.toString()));
    }

    @Test
    public void testMultipleLineBreakOnly() {
        String source = "\n\n\n";
        String target = "\n\n\n";
        int oldPos = 1;
        int newPos = 2;

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);

        assertThat(editable.toString(), is(target));
    }

    @Test
    public void testBlankString() {
        String source = " ";
        String target = " ";
        int oldPos = 0;
        int newPos = 1;

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);

        assertThat(editable.toString(), is(target));
    }

    @Test
    public void testEmptyStringNoIncrement() {
        String source = "";
        String target = "";
        int oldPos = 0;
        int newPos = 0;

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);

        assertThat(editable.toString(), is(target));
    }

    @Test
    public void testNoIncrement() {
        String source = "Test";
        String target = "Test";
        int oldPos = 0;
        int newPos = 0;

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);

        assertThat(editable.toString(), is(target));
    }

    @Test
    public void testLineBreakWithoutPreceedingList() {
        String source = "Hello\nWorld\n";
        String target = "Hello\nWorld\n";
        int oldPos = source.length() - 1;
        int newPos = source.length();

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);

        assertThat(editable.toString(), is(target));
    }

    @Test
    public void testNonLineBreakPosition() {
        String source = "Hello\nWorld\n- list";
        String target = "Hello\nWorld\n- list";
        int oldPos = source.length() - 1;
        int newPos = source.length();

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);

        assertThat(editable.toString(), is(target));
    }

    @Test
    public void testTopLevelBullet() {
        String source = "- Hello\n";
        String target = "- Hello\n- ";
        int oldPos = source.length() - 1;
        int newPos = source.length();

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);

        assertThat(editable.toString(), is(target));
    }

    @Test
    public void testStarBullet() {
        String source = "* Hello\n";
        String target = "* Hello\n* ";
        int oldPos = source.length() - 1;
        int newPos = source.length();

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);

        assertThat(editable.toString(), is(target));
    }

    @Test
    public void testPlusBullet() {
        String source = "+ Hello\n";
        String target = "+ Hello\n+ ";
        int oldPos = source.length() - 1;
        int newPos = source.length();

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);

        assertThat(editable.toString(), is(target));
    }

    @Test
    public void testInvalidBulletChar() {
        String source = "# Hello\n";
        String target = source;
        int oldPos = source.length() - 1;
        int newPos = source.length();

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);

        assertThat(editable.toString(), is(target));
    }

    @Test
    public void testSecondLevelBullet() {
        String source = "- Hello\n - World\n";
        String target = "- Hello\n - World\n - ";
        int oldPos = source.length() - 1;
        int newPos = source.length();

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);

        assertThat(editable.toString(), is(target));
    }

    @Test
    public void testMultiLevelBullet() {
        String source = "- Hello\n - World\n  - Again\n";
        String target = "- Hello\n - World\n  - Again\n  - ";
        int oldPos = source.length() - 1;
        int newPos = source.length();

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);

        assertThat(editable.toString(), is(target));
    }

    @Test
    public void testBulletBreak() {
        String source = "- Hello\n- \n";
        String target = "- Hello\n";
        int oldPos = source.length() - 1;
        int newPos = source.length();

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);

        assertThat(editable.toString(), is(target));
    }

    @Test
    public void testCursorMovedBackwardsToList() {
        String source = "- Hello\n";
        String target = "- Hello\n";
        int oldPos = source.length();
        int newPos = source.length() - 1;

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);

        assertThat(editable.toString(), is(target));
    }

    @Test
    public void testCursorMovedBackwardsToNonList() {
        String source = "Hello\n";
        String target = "Hello\n";
        int oldPos = source.length();
        int newPos = source.length() - 1;

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);

        assertThat(editable.toString(), is(target));
    }

    @Test
    public void testCursorMovedForwardMultipleToList() {
        String source = "Hello\n- list\n";
        String target = "Hello\n- list\n- ";
        int oldPos = 6;
        int newPos = 13;

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);

        assertThat(editable.toString(), is(target));
    }

    @Test
    public void testCursorMovedForwardMultipleToNonList() {
        String source = "Hello\nWorld";
        String target = "Hello\nWorld";
        int oldPos = 6;
        int newPos = 11;

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);

        assertThat(editable.toString(), is(target));
    }

    @Test
    public void testBulletMiddleOfString() {
        String source = "Hello World\n- list\n\nHula Mundo";
        String target = "Hello World\n- list\n- \nHula Mundo";
        int oldPos = 18;
        int newPos = 19; // index of the line break after "list"

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);

        assertThat(editable.toString(), is(target));
    }

    @Test
    public void testEmptyTopLevelBullet() {
        String source = "- first\n- \n";
        String target = "- first\n";
        int oldPos = source.length() - 1;
        int newPos = source.length();

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);

        assertThat(editable.toString(), is(target));
    }

    @Test
    public void testEmptySecondLevelBullet() {
        String source = "- first\n - second\n - \n";
        String target = "- first\n - second\n- ";
        int oldPos = source.length() - 1;
        int newPos = source.length();

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);

        assertThat(editable.toString(), is(target));
    }

    @Test
    public void testBulletNoSpace() {
        String source = "-first\n";
        String target = "-first\n";
        int oldPos = source.length() - 1;
        int newPos = source.length();

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);

        assertThat(editable.toString(), is(target));
    }

    @Test
    public void testAutoBulletCursorOutOfBounds() {
        String source = "- item\n";
        Editable editable = buildEditable(source);
        // newCursorPosition beyond editable.length()
        AutoBullet.apply(editable, 0, 100);
        assertThat(editable.toString(), is(source));
    }

    @Test
    public void testAutoBulletCursorAtStart() {
        String source = "- item\n";
        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, 0, 0);
        assertThat(editable.toString(), is(source));
    }

    @Test
    public void testAutoBulletCursorMovedBackwards() {
        String source = "- item\n";
        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, 5, 3);
        assertThat(editable.toString(), is(source));
    }

    @Test
    public void testAutoBulletInPlaceScanLargeBuffer() {
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            largeText.append("Line ").append(i).append(" with some text\n");
        }
        largeText.append("- Item in list\n");

        AllocationTrackingSpannableStringBuilder trackingBuilder = new AllocationTrackingSpannableStringBuilder(largeText.toString());
        int oldPos = trackingBuilder.length() - 1;
        int newPos = trackingBuilder.length();

        trackingBuilder.setTrackingEnabled(true);
        AutoBullet.apply(trackingBuilder, oldPos, newPos);
        trackingBuilder.setTrackingEnabled(false);

        assertThat(trackingBuilder.getAllocationCount(), is(0));
        assertThat(trackingBuilder.toString().endsWith("- Item in list\n- "), is(true));
    }
}
