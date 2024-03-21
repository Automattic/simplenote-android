package com.automattic.simplenote.uitests.login

import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.MediumTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.automattic.simplenote.BaseUITest
import com.automattic.simplenote.R
import com.automattic.simplenote.utils.Constants
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import com.automattic.simplenote.utils.TestDriver
import org.hamcrest.Matchers.*
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.Assert

@RunWith(AndroidJUnit4ClassRunner::class)
@MediumTest
class LoginTests : BaseUITest() {

    @Test
    fun testLoginWithWrongPasswordFails() {
        onView(withId(R.id.button_login))
            .check(matches(isClickable()))
            .perform(click())

        onView(withId(R.id.button_email))
            .check(matches(isClickable()))
            .perform(click())

        onView(allOf(isDescendantOfA(withId(R.id.input_email)),
            withParent(withClassName(endsWith("android.widget.FrameLayout"))),
            withHint("Email")))
            .check(matches(isDisplayed()))
            .perform(replaceText(Constants.ACCOUNT_EMAIL), closeSoftKeyboard())

        onView(allOf(isDescendantOfA(withId(R.id.input_password)),
            withParent(withClassName(endsWith("android.widget.FrameLayout"))),
            withHint("Password")))
            .check(matches(isDisplayed()))
            .perform(replaceText(Constants.INCORRECT_ACCOUNT_PASSWORD), closeSoftKeyboard())

        onView(withId(R.id.button))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
            .perform(click())

        onView(allOf((withId(R.id.alertTitle)),
            withText("Error")))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testLoginWithCorrectPasswordSucceeds() {

        Assert.assertTrue(
            "User is not on Login page", TestDriver.isUserOnLoginPage())

        Assert.assertTrue(
            "Login was not successful",
            TestDriver.logInWithCredentials(Constants.ACCOUNT_EMAIL, Constants.ACCOUNT_PASSWORD))
    }

    @Test
    fun testLogOut(){

        onView(allOf(withParent(withId(R.id.toolbar)), withContentDescription("Open drawer")))
            .check(matches(isDisplayed()))
            .perform(click())

        onView(allOf(withId(R.id.design_menu_item_text), withText("Settings")))
                .check(matches(isDisplayed()))
                .perform(click())

        onView(allOf(withParent(withId(R.id.toolbar)), withText("Settings")))
            .check(matches(isDisplayed()))

        onView(withId(R.id.preferences_container)).perform(swipeUp())

        onView(allOf(isDescendantOfA(withId(R.id.recycler_view)),
            withText("Log out")
        ))
            .check(matches(ViewMatchers.isDisplayed()))
            .perform(click())

        Assert.assertTrue("Log Out failed",TestDriver.launchPage.signupButton.exists())
    }
}
