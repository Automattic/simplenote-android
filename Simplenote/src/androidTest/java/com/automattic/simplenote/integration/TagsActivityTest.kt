package com.automattic.simplenote.integration

import android.widget.EditText
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.MediumTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.automattic.simplenote.BaseUITest
import com.automattic.simplenote.R
import com.automattic.simplenote.TagsActivity
import com.automattic.simplenote.models.Note
import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.utils.isToast
import com.automattic.simplenote.utils.withItemCount
import com.automattic.simplenote.utils.withRecyclerView
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
@MediumTest
class TagsActivityTest : BaseUITest() {
    private lateinit var activityScenario: ActivityScenario<TagsActivity>

    @Test
    fun listTagsWithEmptyTagsShouldShowEmptyView() {
        launchTagsActivityEmptyTags()
        onView(withId(R.id.empty)).check(matches(isDisplayed()))

        val noTagsText = getResourceString(R.string.empty_tags)
        onView(withId(R.id.text)).check(matches(withText(noTagsText)))
    }

    @Test
    fun listTagsShouldShowCompleteListTags() {
        val testData = launchTagsActivityWithTestData()

        testData.tags.forEachIndexed { index, tagAndCounter ->
            onView(withRecyclerView(R.id.list).atPositionOnView(index, R.id.tag_name))
                .check(matches(withText(tagAndCounter.tag.name)))
            onView(withRecyclerView(R.id.list).atPositionOnView(index, R.id.tag_count))
                .check(matches(withText(tagAndCounter.counter)))
        }

        onView(withId(R.id.empty)).check(matches(not(isDisplayed())))
    }

    @Test
    fun searchTagsShouldReturnMatchedTags() {
        val testData = launchTagsActivityWithTestData()

        val firstSearchPhrase = "tag"
        val secondSearchPhrase = "ot"

        // Activate search bar
        onView(withId(R.id.menu_search)).perform(click())

        // Type in the first search phrase
        onView(isAssignableFrom(EditText::class.java)).perform(typeText(firstSearchPhrase))

        // All tags starting with tag should be shown
        val filteredTagsFirstPhrase = testData.tags.filter { it.tag.name.startsWith(firstSearchPhrase) }
        filteredTagsFirstPhrase.forEachIndexed { index, tagAndCounter ->
            onView(withRecyclerView(R.id.list).atPositionOnView(index, R.id.tag_name))
                .check(matches(withText(tagAndCounter.tag.name)))
            onView(withRecyclerView(R.id.list).atPositionOnView(index, R.id.tag_count))
                .check(matches(withText(tagAndCounter.counter)))
        }

        onView(withId(R.id.list)).check(withItemCount(filteredTagsFirstPhrase.count()))

        // Type in the second search phrase
        onView(isAssignableFrom(EditText::class.java)).perform(replaceText(secondSearchPhrase))

        // Jus the tag other should be shown
        val filteredTagsSecondPhrase = testData.tags.filter { it.tag.name.startsWith(secondSearchPhrase) }
        filteredTagsSecondPhrase.forEachIndexed { index, tagAndCounter ->
            onView(withRecyclerView(R.id.list).atPositionOnView(index, R.id.tag_name))
                .check(matches(withText(tagAndCounter.tag.name)))
            onView(withRecyclerView(R.id.list).atPositionOnView(index, R.id.tag_count))
                .check(matches(withText(tagAndCounter.counter)))
        }

        onView(withId(R.id.list)).check(withItemCount(filteredTagsSecondPhrase.count()))

        onView(withId(R.id.empty)).check(matches(not(isDisplayed())))
    }

    @Test
    fun searchTagsShouldKeepFilteredListAtReenter() {
        val testData = launchTagsActivityWithTestData()

        val firstSearchPhrase = "tag"

        // Activate search bar
        onView(withId(R.id.menu_search)).perform(click())

        // Type in the first search phrase
        onView(isAssignableFrom(EditText::class.java)).perform(typeText(firstSearchPhrase))

        // All tags starting with tag should be shown
        val filteredTags = testData.tags.filter { it.tag.name.startsWith(firstSearchPhrase) }
        filteredTags.forEachIndexed { index, tagAndCounter ->
            onView(withRecyclerView(R.id.list).atPositionOnView(index, R.id.tag_name))
                .check(matches(withText(tagAndCounter.tag.name)))
            onView(withRecyclerView(R.id.list).atPositionOnView(index, R.id.tag_count))
                .check(matches(withText(tagAndCounter.counter)))
        }

        onView(withId(R.id.list)).check(withItemCount(filteredTags.count()))


        // Change the state of the activity to CREATED which calls onPause and onStop
        activityScenario.moveToState(Lifecycle.State.CREATED)

        // Change the state of the activity to RESUMED
        activityScenario.moveToState(Lifecycle.State.RESUMED)

        // All tags starting with tag should be shown when activity becomes RESUMED again
        filteredTags.forEachIndexed { index, tagAndCounter ->
            onView(withRecyclerView(R.id.list).atPositionOnView(index, R.id.tag_name))
                .check(matches(withText(tagAndCounter.tag.name)))
            onView(withRecyclerView(R.id.list).atPositionOnView(index, R.id.tag_count))
                .check(matches(withText(tagAndCounter.counter)))
        }

        onView(withId(R.id.list)).check(withItemCount(filteredTags.count()))
    }

    @Test
    fun searchTagsWithNoResultsShouldShowEmptyView() {
        launchTagsActivityWithTestData()

        onView(withId(R.id.empty)).check(matches(not(isDisplayed())))

        val notMatchesPhrase = "nomatches"
        // Activate search bar
        onView(withId(R.id.menu_search)).perform(click())

        // Type in the first search phrase
        onView(isAssignableFrom(EditText::class.java)).perform(typeText(notMatchesPhrase))

        onView(withId(R.id.empty)).check(matches(isDisplayed()))
        val noTagsFoundText = getResourceString(R.string.empty_tags_search)
        onView(withId(R.id.text)).check(matches(withText(noTagsFoundText)))
    }

    @Test
    fun clickOnAddTagShouldShowDialog() {
        ActivityScenario.launch(TagsActivity::class.java)

        onView(withId(R.id.button_add)).perform(click())
        val addTagTitle = getResourceString(R.string.add_tag)
        onView(withText(addTagTitle)).check(matches(isDisplayed()))
    }

    @Test
    fun longTabOnAddTagShouldShowToast() {
        ActivityScenario.launch(TagsActivity::class.java)

        onView(withId(R.id.button_add)).perform(longClick())
        val addTagTitle = getResourceString(R.string.add_tag)
        onView(withText(addTagTitle)).inRoot(isToast()).check(matches(isDisplayed()))
    }

    @Test
    fun clickOnEditTagShouldShowDialog() {
        launchTagsActivityWithTestData()

        onView(withRecyclerView(R.id.list).atPositionOnView(1, R.id.tag_name))
            .perform(click())

        val renameTagTitle = getResourceString(R.string.rename_tag)
        onView(withText(renameTagTitle)).check(matches(isDisplayed()))
    }

    @Test
    fun deleteTagsShowRemoveTagsFromNotes() {
        val testData = launchTagsActivityWithTestData()

        // Check data is displayed
        testData.tags.forEachIndexed { index, tagAndCounter ->
            onView(withRecyclerView(R.id.list).atPositionOnView(index, R.id.tag_name))
                .check(matches(withText(tagAndCounter.tag.name)))
            onView(withRecyclerView(R.id.list).atPositionOnView(index, R.id.tag_count))
                .check(matches(withText(tagAndCounter.counter)))
        }

        // Delete tag other
        var tagToDelete = "other"
        testData.tags.forEachIndexed { index, tagAndCounter ->
            if (tagAndCounter.tag.name == tagToDelete) {
                onView(withRecyclerView(R.id.list).atPositionOnView(index, R.id.tag_trash))
                    .perform(click())
            }
        }

        assertEquals(tagsBucket.count(), 3)
        assertEquals(notesBucket.count(), 3)
        assertEquals(testData.notes[0].tags, listOf("tag1", "tag2"))
        assertEquals(testData.notes[1].tags, listOf("tag2"))
        assertEquals(testData.notes[2].tags, listOf("tag1", "tag2", "tag3"))

        // Try Delete tag tag2
        val deleteTagTitle = getResourceString(R.string.delete_tag)
        val confirmDeleteTagMessage = getResourceString(R.string.confirm_delete_tag)
        tagToDelete = "tag2"
        testData.tags.forEachIndexed { index, tagAndCounter ->
            if (tagAndCounter.tag.name == tagToDelete) {
                onView(withRecyclerView(R.id.list).atPositionOnView(index, R.id.tag_trash))
                    .perform(click())


                onView(withText(deleteTagTitle)).check(matches(isDisplayed()))
                onView(withText(confirmDeleteTagMessage)).check(matches(isDisplayed()))
                val noText = getResourceString(R.string.no)
                onView(withText(noText)).inRoot(RootMatchers.isDialog()).perform(click())
            }
        }

        assertEquals(tagsBucket.count(), 3)
        assertEquals(notesBucket.count(), 3)
        assertEquals(testData.notes[0].tags, listOf("tag1", "tag2"))
        assertEquals(testData.notes[1].tags, listOf("tag2"))
        assertEquals(testData.notes[2].tags, listOf("tag1", "tag2", "tag3"))

        // Delete tag tag2
        testData.tags.forEachIndexed { index, tagAndCounter ->
            if (tagAndCounter.tag.name == tagToDelete) {
                onView(withRecyclerView(R.id.list).atPositionOnView(1, R.id.tag_trash))
                    .perform(click())

                onView(withText(deleteTagTitle)).check(matches(isDisplayed()))
                onView(withText(confirmDeleteTagMessage)).check(matches(isDisplayed()))
                val yesText = getResourceString(R.string.yes)
                onView(withText(yesText)).inRoot(RootMatchers.isDialog()).perform(click())

                // Avoid flaky tests with AsyncTask
                Thread.sleep(1000)

                assertEquals(tagsBucket.count(), 2)
                assertEquals(notesBucket.count(), 3)
                assertEquals(testData.notes[0].tags, listOf("tag1"))
                assertEquals(testData.notes[1].tags, listOf<String>())
                assertEquals(testData.notes[2].tags, listOf("tag1", "tag3"))
            }
        }

    }

    @Test
    fun scrollTags() {
        for (i in 1 until 51) {
            val tagName = "tag${i}"
            createTag(tagName)
        }

        assertEquals(tagsBucket.count(), 50)

        ActivityScenario.launch(TagsActivity::class.java)

        onView(withId(R.id.list))
            .perform(scrollToPosition<RecyclerView.ViewHolder>(49))

        onView(withRecyclerView(R.id.list).atPositionOnView(49, R.id.tag_name))
            .perform(click())

        val renameTagTitle = getResourceString(R.string.rename_tag)
        onView(withText(renameTagTitle)).check(matches(isDisplayed()))
        onView(CoreMatchers.allOf(withText("tag50"), isDescendantOfA(withId(R.id.input_tag_name))))
            .check(matches(isDisplayed()))

        assertEquals(tagsBucket.count(), 50)
    }

    private fun launchTagsActivityWithTestData(): TestData {
        val tags = mutableListOf<TagAndCounter>()
        val notes = mutableListOf<Note>()

        tags.add(TagAndCounter(createTag("tag1"), "2"))
        tags.add(TagAndCounter(createTag("tag2"), "3"))
        tags.add(TagAndCounter(createTag("tag3"), "1"))
        tags.add(TagAndCounter(createTag("other"), ""))
        // To edit tags, tags should belong a note
        notes.add(createNote("Hello1", listOf("tag1", "tag2")))
        notes.add(createNote("Hello2", listOf("tag2")))
        notes.add(createNote("Hello3", listOf("tag1", "tag2", "tag3")))

        assertEquals(tagsBucket.count(), 4)
        assertEquals(notesBucket.count(), 3)

        activityScenario = ActivityScenario.launch(TagsActivity::class.java)

        return TestData(tags, notes)
    }

    private fun launchTagsActivityEmptyTags() {
        ActivityScenario.launch(TagsActivity::class.java)
    }

    inner class TagAndCounter(val tag: Tag, val counter: String)
    inner class TestData(val tags: List<TagAndCounter>, val notes: List<Note>)
}
