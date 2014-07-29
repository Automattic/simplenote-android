package com.automattic.simplenote.utils;

import android.text.Editable;

import junit.framework.TestCase;

public class AutoBulletTest extends TestCase {
    private Editable buildEditable(CharSequence source) {
        return Editable.Factory.getInstance().newEditable(source);
    }

    public void testLineBreakOnly() {
        String source = "\n";
        String target = "\n";
        int oldPos = 0;
        int newPos = 1;

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);
        assertEquals(target, editable.toString());
    }

    public void testMultipleLineBreakOnly() {
        String source = "\n\n\n";
        String target = "\n\n\n";
        int oldPos = 1;
        int newPos = 2;

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);
        assertEquals(target, editable.toString());
    }

    public void testBlankString() {
        String source = " ";
        String target = " ";
        int oldPos = 0;
        int newPos = 1;

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);
        assertEquals(target, editable.toString());
    }

    public void testEmptyStringNoIncrement() {
        String source = "";
        String target = "";
        int oldPos = 0;
        int newPos = 0;

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);
        assertEquals(target, editable.toString());
    }

    public void testNoIncrement() {
        String source = "Test";
        String target = "Test";
        int oldPos = 0;
        int newPos = 0;

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);
        assertEquals(target, editable.toString());
    }

    public void testLineBreakWithoutPreceedingList() {
        String source = "Hello\nWorld\n";
        String target = "Hello\nWorld\n";
        int oldPos = source.length() - 1;
        int newPos = source.length();

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);
        assertEquals(target, editable.toString());
    }

    public void testNonLineBreakPosition() {
        String source = "Hello\nWorld\n- list";
        String target = "Hello\nWorld\n- list";
        int oldPos = source.length() - 1;
        int newPos = source.length();

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);
        assertEquals(target, editable.toString());
    }

    public void testTopLevelBullet() {
        String source = "- Hello\n";
        String target = "- Hello\n- ";
        int oldPos = source.length() - 1;
        int newPos = source.length();

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);
        assertEquals(target, editable.toString());
    }

    public void testStarBullet() {
        String source = "* Hello\n";
        String target = "* Hello\n* ";
        int oldPos = source.length() - 1;
        int newPos = source.length();

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);
        assertEquals(target, editable.toString());
    }

    public void testPlusBullet() {
        String source = "+ Hello\n";
        String target = "+ Hello\n+ ";
        int oldPos = source.length() - 1;
        int newPos = source.length();

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);
        assertEquals(target, editable.toString());
    }

    public void testOrderedBullet() {
        String source = "1. Hello\n";
        String target = "1. Hello\n1. ";
        int oldPos = source.length() - 1;
        int newPos = source.length();

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);
        assertEquals(target, editable.toString());
    }

    public void testInvalidBulletChar() {
        String source = "# Hello\n";
        String target = source;
        int oldPos = source.length() - 1;
        int newPos = source.length();

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);
        assertEquals(target, editable.toString());
    }

    public void testSecondLevelBullet() {
        String source = "- Hello\n - World\n";
        String target = "- Hello\n - World\n - ";
        int oldPos = source.length() - 1;
        int newPos = source.length();

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);
        assertEquals(target, editable.toString());
    }

    public void testMultiLevelBullet() {
        String source = "- Hello\n - World\n  - Again\n";
        String target = "- Hello\n - World\n  - Again\n  - ";
        int oldPos = source.length() - 1;
        int newPos = source.length();

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);
        assertEquals(target, editable.toString());
    }

    public void testBulletBreak() {
        String source = "- Hello\n- \n";
        String target = "- Hello\n\n";
        int oldPos = source.length() - 1;
        int newPos = source.length();

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);
        assertEquals(target, editable.toString());
    }

    public void testCursorMovedBackwardsToList() {
        String source = "- Hello\n";
        String target = "- Hello\n";
        int oldPos = source.length();
        int newPos = source.length() - 1;

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);
        assertEquals(target, editable.toString());
    }

    public void testCursorMovedBackwardsToNonList() {
        String source = "Hello\n";
        String target = "Hello\n";
        int oldPos = source.length();
        int newPos = source.length() - 1;

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);
        assertEquals(target, editable.toString());
    }

    public void testCursorMovedForwardMultipleToList() {
        String source = "Hello\n- list\n";
        String target = "Hello\n- list\n- ";
        int oldPos = 6;
        int newPos = 13;

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);
        assertEquals(target, editable.toString());
    }

    public void testCursorMovedForwardMultipleToNonList() {
        String source = "Hello\nWorld";
        String target = "Hello\nWorld";
        int oldPos = 6;
        int newPos = 11;

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);
        assertEquals(target, editable.toString());
    }

    public void testBulletMiddleOfString() {
        String source = "Hello World\n- list\n\nHula Mundo";
        String target = "Hello World\n- list\n- \nHula Mundo";
        int oldPos = 18;
        int newPos = 19; // index of the line break after "list"

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);
        assertEquals(target, editable.toString());
    }

    public void testEmptyTopLevelBullet() {
        String source = "- first\n- \n";
        String target = "- first\n\n";
        int oldPos = source.length() - 1;
        int newPos = source.length();

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);
        assertEquals(target, editable.toString());
    }

    public void testEmptySecondLevelBullet() {
        String source = "- first\n - second\n - \n";
        String target = "- first\n - second\n- ";
        int oldPos = source.length() - 1;
        int newPos = source.length();

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);
        assertEquals(target, editable.toString());
    }

    public void testBulletNoSpace() {
        String source = "-first\n";
        String target = "-first\n";
        int oldPos = source.length() - 1;
        int newPos = source.length();

        Editable editable = buildEditable(source);
        AutoBullet.apply(editable, oldPos, newPos);
        assertEquals(target, editable.toString());
    }
}
