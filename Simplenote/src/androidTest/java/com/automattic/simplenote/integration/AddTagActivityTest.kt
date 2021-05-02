package com.automattic.simplenote.integration

import android.app.Activity
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.ActivityResultMatchers.hasResultCode
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.MediumTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.automattic.simplenote.AddTagActivity
import com.automattic.simplenote.BaseUITest
import com.automattic.simplenote.R
import com.automattic.simplenote.utils.hasTextInputLayoutErrorText
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
@MediumTest
class AddTagActivityTest : BaseUITest() {

    @Test
    fun addNewValidTag() {
        assertEquals(tagsBucket.count(), 0)

        ActivityScenario.launch(AddTagActivity::class.java)

        onView(withId(R.id.button_positive)).check(matches(not(isEnabled())))
        onView(withId(R.id.tag_input)).perform(replaceText("tag1"))
        onView(withId(R.id.button_positive)).check(matches(isEnabled()))
        onView(withId(R.id.button_positive)).perform(click())

        assertEquals(tagsBucket.count(), 1)
    }

    @Test
    fun addTagAlreadyExists() {
        createTag("tag1")
        assertEquals(tagsBucket.count(), 1)

        ActivityScenario.launch(AddTagActivity::class.java)

        onView(withId(R.id.button_positive)).check(matches(not(isEnabled())))

        onView(withId(R.id.tag_input)).perform(replaceText("tag1"))
        onView(withId(R.id.button_positive)).check(matches(not(isEnabled())))

        val existsMessage = getResourceString(R.string.tag_error_exists)
        onView(withId(R.id.tag_layout)).check(matches(hasTextInputLayoutErrorText(existsMessage)))

        assertEquals(tagsBucket.count(), 1)
    }

    @Test
    fun addTagTooLong() {
        ActivityScenario.launch(AddTagActivity::class.java)

        onView(withId(R.id.button_positive)).check(matches(not(isEnabled())))

        val newTag = getRandomString(257)
        onView(withId(R.id.tag_input)).perform(replaceText(newTag))
        onView(withId(R.id.button_positive)).check(matches(not(isEnabled())))

        val tooLongMessage = getResourceString(R.string.tag_error_length)
        onView(withId(R.id.tag_layout)).check(matches(hasTextInputLayoutErrorText(tooLongMessage)))

        assertEquals(tagsBucket.count(), 0)
    }

    @Test
    fun addTagWithSpace() {
        assertEquals(tagsBucket.count(), 0)

        val activityScenario = ActivityScenario.launch(AddTagActivity::class.java)

        onView(withId(R.id.button_positive)).check(matches(not(isEnabled())))

        onView(withId(R.id.tag_input)).perform(replaceText("tag 3"))
        onView(withId(R.id.button_positive)).check(matches(not(isEnabled())))

        val spaceMessage = getResourceString(R.string.tag_error_spaces)
        onView(withId(R.id.tag_layout)).check(matches(hasTextInputLayoutErrorText(spaceMessage)))
        onView(withId(R.id.button_negative)).perform(click())
        assertThat(activityScenario.result, hasResultCode(Activity.RESULT_CANCELED))

        assertEquals(tagsBucket.count(), 0)
    }

    @Test
    fun addTagEmpty() {
        assertEquals(tagsBucket.count(), 0)

        val activityScenario = ActivityScenario.launch(AddTagActivity::class.java)

        onView(withId(R.id.button_positive)).check(matches(not(isEnabled())))

        onView(withId(R.id.tag_input)).perform(replaceText("tag1"))
        onView(withId(R.id.button_positive)).check(matches(isEnabled()))

        onView(withId(R.id.tag_input)).perform(replaceText(""))
        onView(withId(R.id.button_positive)).check(matches(not(isEnabled())))

        val spaceMessage = getResourceString(R.string.tag_error_empty)
        onView(withId(R.id.tag_layout)).check(matches(hasTextInputLayoutErrorText(spaceMessage)))
        onView(withId(R.id.button_negative)).perform(click())
        assertThat(activityScenario.result, hasResultCode(Activity.RESULT_CANCELED))


        assertEquals(tagsBucket.count(), 0)
    }

    @Test
    fun addTagCancel() {
        assertEquals(tagsBucket.count(), 0)

        val activityScenario = ActivityScenario.launch(AddTagActivity::class.java)

        onView(withId(R.id.button_positive)).check(matches(not(isEnabled())))

        onView(withId(R.id.tag_input)).perform(replaceText("tag"))
        onView(withId(R.id.button_positive)).check(matches(isEnabled()))
        onView(withId(R.id.button_negative)).perform(click())

        assertThat(activityScenario.result, hasResultCode(Activity.RESULT_CANCELED))

        assertEquals(tagsBucket.count(), 0)
    }

    @Test
    fun addSecondValidTag() {
        createTag("tag5")
        assertEquals(tagsBucket.count(), 1)

        val activityScenario = ActivityScenario.launch(AddTagActivity::class.java)

        onView(withId(R.id.button_positive)).check(matches(not(isEnabled())))

        onView(withId(R.id.tag_input)).perform(replaceText("tag6"))
        onView(withId(R.id.button_positive)).check(matches(isEnabled()))
        onView(withId(R.id.button_positive)).perform(click())
        assertThat(activityScenario.result, hasResultCode(Activity.RESULT_OK))

        assertEquals(tagsBucket.count(), 2)
    }
}
