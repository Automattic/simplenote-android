package com.automattic.simplenote.integration

import androidx.fragment.app.testing.launchFragment
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.MediumTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.automattic.simplenote.BaseUITest
import com.automattic.simplenote.R
import com.automattic.simplenote.TagDialogFragment
import org.hamcrest.CoreMatchers.allOf
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
@MediumTest
class TagDialogFragmentTest : BaseUITest() {

    @Test
    fun editTagWithValidName() {
        createTag("tag1")
        assertEquals(tagsBucket.count(), 1)

        launchFragment("tag1")

        onView(allOf(withText("tag1"), isDescendantOfA(withId(R.id.input_tag_name)))).check(matches(isDisplayed()))
        onView(allOf(withText("tag1"), isDescendantOfA(withId(R.id.input_tag_name))))
                .perform(ViewActions.replaceText("tag5"))
        onView(allOf(withText("tag5"), isDescendantOfA(withId(R.id.input_tag_name)))).check(matches(isDisplayed()))

        val saveText = getResourceString(R.string.save)
        onView(withText(saveText)).inRoot(isDialog()).check(matches(isDisplayed())).perform(click())

        val tag1 = getTag("tag1")
        val tag5 = getTag("tag5")

        assertNull(tag1)
        assertNotNull(tag5)
        assertEquals(tagsBucket.count(), 1)
    }

    private fun launchFragment(tagName: String) {
        val scenario = launchFragment(null, R.style.Base_Theme_Simplestyle) {
            val tag = getTag(tagName)
            TagDialogFragment(
                    tag,
                    notesBucket,
                    tagsBucket
            )
        }

        // Validates the dialog is shown
        scenario.onFragment { fragment ->
            assertNotNull(fragment.dialog)
            assertEquals(true, fragment.requireDialog().isShowing)
            fragment.parentFragmentManager.executePendingTransactions()
        }
    }
}
