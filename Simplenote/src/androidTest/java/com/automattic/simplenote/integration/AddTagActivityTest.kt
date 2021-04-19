package com.automattic.simplenote.integration

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.automattic.simplenote.AddTagActivity
import com.automattic.simplenote.BaseUITest
import com.automattic.simplenote.R
import com.automattic.simplenote.SimplenoteTest
import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.utils.TagUtils
import com.automattic.simplenote.utils.TestBucket
import com.automattic.simplenote.utils.hasTextInputLayoutErrorText
import org.hamcrest.CoreMatchers.not
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class AddActivityTest : BaseUITest() {
    private lateinit var application: SimplenoteTest
    private lateinit var tagsBucket: TestBucket<Tag>

    @Before
    fun setup() {
        application = getApplicationContext() as SimplenoteTest
        tagsBucket = application.tagsBucket as TestBucket<Tag>
        tagsBucket.clear()
    }

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
        TagUtils.createTagIfMissing(tagsBucket, "tag1")
        Assert.assertEquals(tagsBucket.count(), 1)

        ActivityScenario.launch(AddTagActivity::class.java)

        onView(withId(R.id.tag_input)).perform(replaceText("tag1"))
        onView(withId(R.id.button_positive)).check(matches(not(isEnabled())))

        val existsMessage = getResourceString(R.string.tag_error_exists) ?: ""
        onView(withId(R.id.tag_layout)).check(matches(hasTextInputLayoutErrorText(existsMessage)))

        Assert.assertEquals(tagsBucket.count(), 1)
    }

    @Test
    fun addTagTooLong() {
        TagUtils.createTagIfMissing(tagsBucket, "tag1")
        Assert.assertEquals(tagsBucket.count(), 1)

        ActivityScenario.launch(AddTagActivity::class.java)

        val newTag =
        onView(withId(R.id.tag_input)).perform(replaceText("tag1"))
        onView(withId(R.id.button_positive)).check(matches(not(isEnabled())))

        val existsMessage = getResourceString(R.string.tag_error_exists) ?: ""
        onView(withId(R.id.tag_layout)).check(matches(hasTextInputLayoutErrorText(existsMessage)))

        Assert.assertEquals(tagsBucket.count(), 1)
    }
}
