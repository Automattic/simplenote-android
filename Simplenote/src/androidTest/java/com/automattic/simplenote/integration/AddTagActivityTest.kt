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
import androidx.test.runner.AndroidJUnit4
import com.automattic.simplenote.AddTagActivity
import com.automattic.simplenote.BaseUITest
import com.automattic.simplenote.R
import com.automattic.simplenote.utils.hasTextInputLayoutErrorText
import org.hamcrest.CoreMatchers.not
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class AddActivityTest : BaseUITest() {

    @Test
    fun addNewValidTag() {
        Assert.assertEquals(tagsBucket.count(), 0)

        ActivityScenario.launch(AddTagActivity::class.java)

        onView(withId(R.id.tag_input)).perform(replaceText("tag1"))
        onView(withId(R.id.button_positive)).check(matches(isEnabled()))
        onView(withId(R.id.button_positive)).perform(click())

        Assert.assertEquals(tagsBucket.count(), 1)
    }

    @Test
    fun addTagAlreadyExists() {
        createTag("tag1")
        Assert.assertEquals(tagsBucket.count(), 1)

        ActivityScenario.launch(AddTagActivity::class.java)

        onView(withId(R.id.tag_input)).perform(replaceText("tag1"))
        onView(withId(R.id.button_positive)).check(matches(not(isEnabled())))

        val existsMessage = getResourceString(R.string.tag_error_exists)
        onView(withId(R.id.tag_layout)).check(matches(hasTextInputLayoutErrorText(existsMessage)))

        Assert.assertEquals(tagsBucket.count(), 1)
    }

    @Test
    fun addTagTooLong() {
        ActivityScenario.launch(AddTagActivity::class.java)

        val newTag = getRandomString(257)
        onView(withId(R.id.tag_input)).perform(replaceText(newTag))
        onView(withId(R.id.button_positive)).check(matches(not(isEnabled())))

        val tooLongMessage = getResourceString(R.string.tag_error_length)
        onView(withId(R.id.tag_layout)).check(matches(hasTextInputLayoutErrorText(tooLongMessage)))

        Assert.assertEquals(tagsBucket.count(), 0)
    }

    @Test
    fun addTagWithSpace() {
        Assert.assertEquals(tagsBucket.count(), 0)

        val activityScenario = ActivityScenario.launch(AddTagActivity::class.java)

        onView(withId(R.id.tag_input)).perform(replaceText("tag 1"))
        onView(withId(R.id.button_positive)).check(matches(not(isEnabled())))

        val spaceMessage = getResourceString(R.string.tag_error_spaces)
        onView(withId(R.id.tag_layout)).check(matches(hasTextInputLayoutErrorText(spaceMessage)))
        onView(withId(R.id.button_negative)).perform(click())
        assertThat(activityScenario.result, hasResultCode(Activity.RESULT_CANCELED))

        Assert.assertEquals(tagsBucket.count(), 0)
    }

    @Test
    fun addTagEmpty() {
        Assert.assertEquals(tagsBucket.count(), 0)

        val activityScenario = ActivityScenario.launch(AddTagActivity::class.java)

        onView(withId(R.id.tag_input)).perform(replaceText("tag1"))
        onView(withId(R.id.button_positive)).check(matches(isEnabled()))

        onView(withId(R.id.tag_input)).perform(replaceText(""))
        onView(withId(R.id.button_positive)).check(matches(not(isEnabled())))

        val spaceMessage = getResourceString(R.string.tag_error_empty)
        onView(withId(R.id.tag_layout)).check(matches(hasTextInputLayoutErrorText(spaceMessage)))
        onView(withId(R.id.button_negative)).perform(click())
        assertThat(activityScenario.result, hasResultCode(Activity.RESULT_CANCELED))


        Assert.assertEquals(tagsBucket.count(), 0)
    }

    @Test
    fun addTagCancel() {
        Assert.assertEquals(tagsBucket.count(), 0)

        val activityScenario = ActivityScenario.launch(AddTagActivity::class.java)

        onView(withId(R.id.tag_input)).perform(replaceText("tag1"))
        onView(withId(R.id.button_positive)).check(matches(isEnabled()))
        onView(withId(R.id.button_negative)).perform(click())

        assertThat(activityScenario.result, hasResultCode(Activity.RESULT_CANCELED))

        Assert.assertEquals(tagsBucket.count(), 0)
    }

    @Test
    fun addSecondValidTag() {
        createTag("tag5")
        Assert.assertEquals(tagsBucket.count(), 1)

        val activityScenario = ActivityScenario.launch(AddTagActivity::class.java)

        onView(withId(R.id.tag_input)).perform(replaceText("tag6"))
        onView(withId(R.id.button_positive)).check(matches(isEnabled()))
        onView(withId(R.id.button_positive)).perform(click())
        assertThat(activityScenario.result, hasResultCode(Activity.RESULT_OK))

        Assert.assertEquals(tagsBucket.count(), 2)
    }
}
