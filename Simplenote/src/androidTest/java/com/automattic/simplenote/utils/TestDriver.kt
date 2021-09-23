package com.automattic.simplenote.utils

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiSelector
import com.automattic.simplenote.R
import com.automattic.simplenote.uipages.*
import org.hamcrest.Matchers
import org.junit.Assert
import java.lang.Error
import java.lang.Exception

object TestDriver {

    //Add var for each page object
    val allNotesHomePage = AllNotesHomePage()
    val launchPage = LaunchPage()
    val loginPage = LoginPage()
    val signUpPage = SignUpPage()

    // Returns True if Login button is visible
    fun isUserOnLoginPage(): Boolean {

        if (!loginPage.loginPageLoginButton.exists()) {
            launchPage.loginButton.click()
            launchPage.loginEmailButton.click()
        }
        return loginPage.loginPageLoginButton.exists()
    }

    /**
     * Insert the username and password in the LogIn page and does the login
     */
    @Throws(UiObjectNotFoundException::class)
    fun logInWithCredentials(username: String, password: String): Boolean {

        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.input_email)),
                ViewMatchers.withParent(ViewMatchers.withClassName(Matchers.endsWith("android.widget.FrameLayout"))),
                ViewMatchers.withHint("Email")
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .perform(ViewActions.replaceText(Constants.ACCOUNT_EMAIL), ViewActions.closeSoftKeyboard())

        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.input_password)),
                ViewMatchers.withParent(ViewMatchers.withClassName(Matchers.endsWith("android.widget.FrameLayout"))),
                ViewMatchers.withHint("Password")
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .perform(ViewActions.replaceText(Constants.ACCOUNT_PASSWORD), ViewActions.closeSoftKeyboard())

        loginPage.loginPageLoginButton.click()

        // if True (Open Drawer menu is visible) then login was successful
        return isOpenDrawerMenuVisible()
    }

    fun isOpenDrawerMenuVisible(): Boolean {

        if(allNotesHomePage.searchIcon.exists())
            return true
        else
            return false
    }

    fun logOutUser(): Boolean {
        return launchPage.signupButton.exists()
    }

    /**
     * Tap a button using text
     */
    fun tapButton(buttonName: String) {
        try {
            val uiDevice = UiDevice.getInstance(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation())
            val button = uiDevice.findObject(UiSelector().text(buttonName))
            Assert.assertTrue("Tap button $buttonName...", button.exists() && button.isEnabled)
            button.click()
        } catch (e: Error) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
