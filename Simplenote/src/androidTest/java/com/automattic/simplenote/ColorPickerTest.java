package com.automattic.simplenote;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

/**
 * Created by Day7 on 02.11.16.
 */

public class ColorPickerTest extends ActivityInstrumentationTestCase2<AboutActivity> {

    public ColorPickerTest() {
        super(AboutActivity.class);
    }
}
