package com.automattic.simplenote.integration

import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.MediumTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.automattic.simplenote.BaseUITest
import com.automattic.simplenote.R
import com.automattic.simplenote.TagsActivity
import com.automattic.simplenote.utils.withRecyclerView
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
@MediumTest
class TagsActivityTest : BaseUITest() {

    @Test
    fun listTagsShouldShowCompleteListTags() {
        createTag("tag1")
        createTag("tag2")
        createTag("tag3")
        createTag("other")
        // To edit tags, tags should belong a note
        createNote("Hello1", listOf("tag1", "tag2"))
        createNote("Hello2", listOf("tag2"))
        createNote("Hello3", listOf("tag1", "tag2", "tag3"))

        Assert.assertEquals(tagsBucket.count(), 4)
        Assert.assertEquals(notesBucket.count(), 3)

        ActivityScenario.launch(TagsActivity::class.java)

        onView(withRecyclerView(R.id.list).atPositionOnView(0, R.id.tag_name))
                .check(matches(withText("tag1")))
        onView(withRecyclerView(R.id.list).atPositionOnView(0, R.id.tag_count))
                .check(matches(withText("2")))

        onView(withRecyclerView(R.id.list).atPositionOnView(1, R.id.tag_name))
                .check(matches(withText("tag2")))
        onView(withRecyclerView(R.id.list).atPositionOnView(1, R.id.tag_count))
                .check(matches(withText("3")))

        onView(withRecyclerView(R.id.list).atPositionOnView(2, R.id.tag_name))
                .check(matches(withText("tag3")))
        onView(withRecyclerView(R.id.list).atPositionOnView(2, R.id.tag_count))
                .check(matches(withText("1")))

        onView(withRecyclerView(R.id.list).atPositionOnView(3, R.id.tag_name))
                .check(matches(withText("other")))
        onView(withRecyclerView(R.id.list).atPositionOnView(3, R.id.tag_count))
                .check(matches(withText("")))
    }

    @Test
    fun searchTagsShouldReturnMatchedTags() {
        createTag("tag1")
        createTag("tag2")
        createTag("tag3")
        createTag("other")
        createNote("Hello1", listOf("tag1", "tag2"))
        createNote("Hello2", listOf("tag2"))
        createNote("Hello3", listOf("tag1", "tag2", "tag3"))

        Assert.assertEquals(tagsBucket.count(), 4)
        Assert.assertEquals(notesBucket.count(), 3)

        ActivityScenario.launch(TagsActivity::class.java)

        onView(withId(R.id.menu_search)).perform(click())

        onView(isAssignableFrom(EditText::class.java)).perform(typeText("tag"))

        onView(withRecyclerView(R.id.list).atPositionOnView(0, R.id.tag_name))
                .check(matches(withText("tag1")))
        onView(withRecyclerView(R.id.list).atPositionOnView(0, R.id.tag_count))
                .check(matches(withText("2")))

        onView(withRecyclerView(R.id.list).atPositionOnView(1, R.id.tag_name))
                .check(matches(withText("tag2")))
        onView(withRecyclerView(R.id.list).atPositionOnView(1, R.id.tag_count))
                .check(matches(withText("3")))

        onView(withRecyclerView(R.id.list).atPositionOnView(2, R.id.tag_name))
                .check(matches(withText("tag3")))
        onView(withRecyclerView(R.id.list).atPositionOnView(2, R.id.tag_count))
                .check(matches(withText("1")))

        onView(isAssignableFrom(EditText::class.java)).perform(replaceText("ot"))
        onView(withRecyclerView(R.id.list).atPositionOnView(0, R.id.tag_name))
                .check(matches(withText("other")))
        onView(withRecyclerView(R.id.list).atPositionOnView(0, R.id.tag_count))
                .check(matches(withText("")))
    }

    @Test
    fun clickOnAddTagShouldShowDialog() {
        ActivityScenario.launch(TagsActivity::class.java)

        onView(withId(R.id.button_add)).perform(click())
        val addTagTitle = getResourceString(R.string.add_tag)
        onView(withText(addTagTitle)).check(matches(isDisplayed()))
    }
}
