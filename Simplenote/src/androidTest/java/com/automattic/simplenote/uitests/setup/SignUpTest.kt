package com.automattic.simplenote.uitests.setup

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.MediumTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.automattic.simplenote.BaseUITest
import com.automattic.simplenote.R
import com.automattic.simplenote.utils.Constants
import com.automattic.simplenote.utils.TestDriver
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
@MediumTest
class SignUpTest : BaseUITest(){

    @Test
    fun newUserSignUpWithMixedCapitalization() {

        onView(withId(R.id.button_signup))
            .check(matches(isClickable()))
            .perform(click())

        onView(withId(R.id.email_edit_text))
            .check(matches(isClickable()))
            .perform(click())

        onView(withId(R.id.email_edit_text))
         .perform(ViewActions.replaceText(Constants.SIGNUP_ACCOUNT_EMAIL), ViewActions.closeSoftKeyboard())

        TestDriver.tapButton(Constants.SIGNUP_BUTTON_STR)
    }
}
