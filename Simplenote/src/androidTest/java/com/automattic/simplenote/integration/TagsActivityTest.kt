package com.automattic.simplenote.integration

import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.MediumTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.automattic.simplenote.BaseUITest
import com.automattic.simplenote.R
import com.automattic.simplenote.TagsActivity
import com.automattic.simplenote.utils.isToast
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

    @Test
    fun lonTabOnAddTagShouldShowToast() {
        ActivityScenario.launch(TagsActivity::class.java)

        onView(withId(R.id.button_add)).perform(longClick())
        val addTagTitle = getResourceString(R.string.add_tag)
        onView(withText(addTagTitle)).inRoot(isToast()).check(matches(isDisplayed()))
    }

    @Test
    fun clickOnEditTagShouldShowDialog() {
        createTag("tag1")
        createTag("tag2")
        createTag("tag3")
        createTag("other")

        Assert.assertEquals(tagsBucket.count(), 4)

        ActivityScenario.launch(TagsActivity::class.java)

        onView(withRecyclerView(R.id.list).atPositionOnView(1, R.id.tag_name))
                .perform(click())

        val renameTagTitle = getResourceString(R.string.rename_tag)
        onView(withText(renameTagTitle)).check(matches(isDisplayed()))
    }

    @Test
    fun deleteTagsShowRemoveTagsFromNotes() {
        createTag("other")
        createTag("tag1")
        createTag("tag2")
        createTag("tag3")
        // To edit tags, tags should belong a note
        val note1 = createNote("Hello1", listOf("tag1", "tag2"))
        val note2 = createNote("Hello2", listOf("tag2"))
        val note3 = createNote("Hello3", listOf("tag1", "tag2", "tag3"))

        Assert.assertEquals(tagsBucket.count(), 4)
        Assert.assertEquals(notesBucket.count(), 3)

        ActivityScenario.launch(TagsActivity::class.java)

        onView(withRecyclerView(R.id.list).atPositionOnView(0, R.id.tag_name))
                .check(matches(withText("other")))
        onView(withRecyclerView(R.id.list).atPositionOnView(0, R.id.tag_count))
                .check(matches(withText("")))

        onView(withRecyclerView(R.id.list).atPositionOnView(1, R.id.tag_name))
                .check(matches(withText("tag1")))
        onView(withRecyclerView(R.id.list).atPositionOnView(1, R.id.tag_count))
                .check(matches(withText("2")))

        onView(withRecyclerView(R.id.list).atPositionOnView(2, R.id.tag_name))
                .check(matches(withText("tag2")))
        onView(withRecyclerView(R.id.list).atPositionOnView(2, R.id.tag_count))
                .check(matches(withText("3")))

        onView(withRecyclerView(R.id.list).atPositionOnView(3, R.id.tag_name))
                .check(matches(withText("tag3")))
        onView(withRecyclerView(R.id.list).atPositionOnView(3, R.id.tag_count))
                .check(matches(withText("1")))

        // Delete tag other
        onView(withRecyclerView(R.id.list).atPositionOnView(0, R.id.tag_trash))
                .perform(click())

        Assert.assertEquals(tagsBucket.count(), 3)
        Assert.assertEquals(notesBucket.count(), 3)
        Assert.assertEquals(note1.tags, listOf("tag1", "tag2"))
        Assert.assertEquals(note2.tags, listOf("tag2"))
        Assert.assertEquals(note3.tags, listOf("tag1", "tag2", "tag3"))

        // Try Delete tag tag2
        onView(withRecyclerView(R.id.list).atPositionOnView(1, R.id.tag_trash))
                .perform(click())

        val deleteTagTitle = getResourceString(R.string.delete_tag)
        val confirmDeleteTagMessage = getResourceString(R.string.confirm_delete_tag)
        onView(withText(deleteTagTitle)).check(matches(isDisplayed()))
        onView(withText(confirmDeleteTagMessage)).check(matches(isDisplayed()))
        val noText = getResourceString(R.string.no)
        onView(withText(noText)).inRoot(RootMatchers.isDialog()).perform(click())

        Assert.assertEquals(tagsBucket.count(), 3)
        Assert.assertEquals(notesBucket.count(), 3)
        Assert.assertEquals(note1.tags, listOf("tag1", "tag2"))
        Assert.assertEquals(note2.tags, listOf("tag2"))
        Assert.assertEquals(note3.tags, listOf("tag1", "tag2", "tag3"))

        // Delete tag tag2
        onView(withRecyclerView(R.id.list).atPositionOnView(1, R.id.tag_trash))
                .perform(click())

        onView(withText(deleteTagTitle)).check(matches(isDisplayed()))
        onView(withText(confirmDeleteTagMessage)).check(matches(isDisplayed()))
        val yesText = getResourceString(R.string.yes)
        onView(withText(yesText)).inRoot(RootMatchers.isDialog()).perform(click())

        // Avoid flaky tests with AsyncTask
        Thread.sleep(1000)

        Assert.assertEquals(tagsBucket.count(), 2)
        Assert.assertEquals(notesBucket.count(), 3)
        Assert.assertEquals(note1.tags, listOf("tag1"))
        Assert.assertEquals(note2.tags, listOf<String>())
        Assert.assertEquals(note3.tags, listOf("tag1", "tag3"))
    }
}
