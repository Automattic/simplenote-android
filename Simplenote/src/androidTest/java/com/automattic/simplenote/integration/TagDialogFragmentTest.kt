package com.automattic.simplenote.integration

import androidx.fragment.app.DialogFragment
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
import com.automattic.simplenote.utils.TagUtils
import com.automattic.simplenote.utils.hasTextInputLayoutErrorText
import com.automattic.simplenote.utils.launchDialogFragmentInHiltContainer
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
@MediumTest
class TagDialogFragmentTest : BaseUITest() {

    @Test
    fun editTagWithValidName() {
        createTag("tag1")
        // To edit tags, tags should belong a note
        val note = createNote("Hello World", listOf("tag1"))

        assertEquals(tagsBucket.count(), 1)
        assertEquals(notesBucket.count(), 1)

        launchFragment("tag1")

        val renameTagTitle = getResourceString(R.string.rename_tag)
        onView(withText(renameTagTitle))
                .check(matches(isDisplayed()))

        onView(allOf(withText("tag1"), isDescendantOfA(withId(R.id.input_tag_name))))
                .check(matches(isDisplayed()))
        onView(allOf(withText("tag1"), isDescendantOfA(withId(R.id.input_tag_name))))
                .perform(ViewActions.replaceText("tag5"))
        onView(allOf(withText("tag5"), isDescendantOfA(withId(R.id.input_tag_name))))
                .check(matches(isDisplayed()))

        val saveText = getResourceString(R.string.save)
        onView(withText(saveText))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
                .perform(click())

        val tag1 = getTag("tag1")
        val tag5 = getTag("tag5")

        assertNull(tag1)
        assertNotNull(tag5)
        assertEquals(tagsBucket.count(), 1)
        assertEquals(note.tags, listOf("tag5"))
    }

    @Test
    fun editTagTooLong() {
        createTag("tag1")
        // To edit tags, tags should belong a note
        val note = createNote("Hello World", listOf("tag1"))

        assertEquals(tagsBucket.count(), 1)
        assertEquals(notesBucket.count(), 1)

        launchFragment("tag1")

        val renameTagTitle = getResourceString(R.string.rename_tag)
        onView(withText(renameTagTitle)).check(matches(isDisplayed()))

        onView(allOf(withText("tag1"), isDescendantOfA(withId(R.id.input_tag_name))))
                .check(matches(isDisplayed()))

        val longName = getRandomString(258)
        onView(allOf(withText("tag1"), isDescendantOfA(withId(R.id.input_tag_name))))
                .perform(ViewActions.replaceText(longName))

        val tooLongMessage = getResourceString(R.string.tag_error_length)
        onView(withId(R.id.input_tag_name))
                .check(matches(hasTextInputLayoutErrorText(tooLongMessage)))

        val saveText = getResourceString(R.string.save)
        onView(withText(saveText))
                .inRoot(isDialog())
                .check(matches(not(isEnabled())))
        val cancelText = getResourceString(R.string.cancel)
        onView(withText(cancelText))
                .inRoot(isDialog())
                .perform(click())

        assertEquals(tagsBucket.count(), 1)
        assertEquals(note.tags, listOf("tag1"))
    }

    @Test
    fun editTagWithSpace() {
        createTag("tag10")
        // To edit tags, tags should belong a note
        val note = createNote("Hello World", listOf("tag10"))

        assertEquals(tagsBucket.count(), 1)
        assertEquals(notesBucket.count(), 1)

        launchFragment("tag10")

        val renameTagTitle = getResourceString(R.string.rename_tag)
        onView(withText(renameTagTitle))
                .check(matches(isDisplayed()))

        onView(allOf(withText("tag10"), isDescendantOfA(withId(R.id.input_tag_name))))
                .check(matches(isDisplayed()))

        val tagWithSpace = "tag 3"
        onView(allOf(withText("tag10"), isDescendantOfA(withId(R.id.input_tag_name))))
                .perform(ViewActions.replaceText(tagWithSpace))

        val tagWithSpaceMessage = getResourceString(R.string.tag_error_spaces)
        onView(withId(R.id.input_tag_name))
                .check(matches(hasTextInputLayoutErrorText(tagWithSpaceMessage)))

        val saveText = getResourceString(R.string.save)
        onView(withText(saveText))
                .inRoot(isDialog())
                .check(matches(not(isEnabled())))
        val cancelText = getResourceString(R.string.cancel)
        onView(withText(cancelText))
                .inRoot(isDialog())
                .perform(click())

        assertEquals(tagsBucket.count(), 1)
        assertEquals(note.tags, listOf("tag10"))
    }

    @Test
    fun editTagWithEmpty() {
        createTag("tag10")
        // To edit tags, tags should belong a note
        val note = createNote("Hello World", listOf("tag10"))

        assertEquals(tagsBucket.count(), 1)
        assertEquals(notesBucket.count(), 1)

        launchFragment("tag10")

        val renameTagTitle = getResourceString(R.string.rename_tag)
        onView(withText(renameTagTitle)).check(matches(isDisplayed()))

        onView(allOf(withText("tag10"), isDescendantOfA(withId(R.id.input_tag_name))))
                .check(matches(isDisplayed()))

        val emptyTag = ""
        onView(allOf(withText("tag10"), isDescendantOfA(withId(R.id.input_tag_name))))
                .perform(ViewActions.replaceText(emptyTag))

        val tagEmpty = getResourceString(R.string.tag_error_empty)
        onView(withId(R.id.input_tag_name))
                .check(matches(hasTextInputLayoutErrorText(tagEmpty)))

        val saveText = getResourceString(R.string.save)
        onView(withText(saveText))
                .inRoot(isDialog())
                .check(matches(not(isEnabled())))
        val cancelText = getResourceString(R.string.cancel)
        onView(withText(cancelText))
                .inRoot(isDialog())
                .perform(click())

        assertEquals(tagsBucket.count(), 1)
        assertEquals(note.tags, listOf("tag10"))
    }

    @Test
    fun editTagInMultipleNotes() {
        createTag("tag1")
        createTag("tag2")
        createTag("tag3")
        // To edit tags, tags should belong a note
        val note1 = createNote("Hello1", listOf("tag1", "tag2"))
        val note2 = createNote("Hello2", listOf("tag2", "tag3"))
        val note3 = createNote("Hello3", listOf("tag1", "tag3"))

        assertEquals(tagsBucket.count(), 3)
        assertEquals(notesBucket.count(), 3)

        launchFragment("tag2")

        val renameTagTitle = getResourceString(R.string.rename_tag)
        onView(withText(renameTagTitle)).check(matches(isDisplayed()))

        onView(allOf(withText("tag2"), isDescendantOfA(withId(R.id.input_tag_name))))
                .check(matches(isDisplayed()))

        onView(allOf(withText("tag2"), isDescendantOfA(withId(R.id.input_tag_name))))
                .perform(ViewActions.replaceText("tag10"))

        onView(allOf(withText("tag10"), isDescendantOfA(withId(R.id.input_tag_name))))
                .check(matches(isDisplayed()))

        val saveText = getResourceString(R.string.save)
        onView(withText(saveText))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
                .perform(click())

        val tag1 = getTag("tag1")
        val tag2 = getTag("tag2")
        val tag3 = getTag("tag3")
        val tag10 = getTag("tag10")

        assertNull(tag2)
        assertNotNull(tag1)
        assertNotNull(tag3)
        assertNotNull(tag10)

        assertEquals(note1.tags, listOf("tag1", "tag10"))
        assertEquals(note2.tags, listOf("tag3", "tag10"))
        assertEquals(note3.tags, listOf("tag1", "tag3"))
    }

    @Test
    fun editTryMergeTagInMultipleNotes() {
        createTag("tag1")
        createTag("tag2")
        createTag("tag3")
        // To edit tags, tags should belong a note
        val note1 = createNote("Hello1", listOf("tag1", "tag2"))
        val note2 = createNote("Hello2", listOf("tag2", "tag3"))
        val note3 = createNote("Hello3", listOf("tag1", "tag3"))

        assertEquals(tagsBucket.count(), 3)
        assertEquals(notesBucket.count(), 3)

        launchFragment("tag2")

        val renameTagTitle = getResourceString(R.string.rename_tag)
        onView(withText(renameTagTitle))
                .check(matches(isDisplayed()))

        onView(allOf(withText("tag2"), isDescendantOfA(withId(R.id.input_tag_name))))
                .check(matches(isDisplayed()))

        onView(allOf(withText("tag2"), isDescendantOfA(withId(R.id.input_tag_name))))
                .perform(ViewActions.replaceText("tag3"))

        onView(allOf(withText("tag3"), isDescendantOfA(withId(R.id.input_tag_name))))
                .check(matches(isDisplayed()))

        val saveText = getResourceString(R.string.save)
        onView(withText(saveText))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
                .perform(click())

        val titleMergeDialog = getResourceString(R.string.dialog_tag_conflict_title)
        onView(withText(titleMergeDialog)).check(matches(isDisplayed()))

        val canonical = TagUtils.getCanonicalFromLexical(tagsBucket, "tag3")
        val mergeMessage = getResourceStringWithArgs(R.string.dialog_tag_conflict_message, canonical, "tag2", canonical)
        onView(withId(R.id.message))
                .check(matches(withText(mergeMessage)))

        val backText = getResourceString(R.string.back)
        onView(withText(backText)).perform(click())

        val cancelText = getResourceString(R.string.cancel)
        onView(withText(cancelText))
                .inRoot(isDialog())
                .perform(click())

        val tag1 = getTag("tag1")
        val tag2 = getTag("tag2")
        val tag3 = getTag("tag3")

        assertNotNull(tag2)
        assertNotNull(tag1)
        assertNotNull(tag3)

        assertEquals(note1.tags, listOf("tag1", "tag2"))
        assertEquals(note2.tags, listOf("tag2", "tag3"))
        assertEquals(note3.tags, listOf("tag1", "tag3"))
    }

    @Test
    fun editMergeTagInMultipleNotes() {
        createTag("tag1")
        createTag("tag2")
        createTag("tag3")
        // To edit tags, tags should belong a note
        val note1 = createNote("Hello1", listOf("tag1", "tag2"))
        val note2 = createNote("Hello2", listOf("tag2", "tag3"))
        val note3 = createNote("Hello3", listOf("tag1", "tag3"))

        assertEquals(tagsBucket.count(), 3)
        assertEquals(notesBucket.count(), 3)

        launchFragment("tag2")

        val renameTagTitle = getResourceString(R.string.rename_tag)
        onView(withText(renameTagTitle)).check(matches(isDisplayed()))

        onView(allOf(withText("tag2"), isDescendantOfA(withId(R.id.input_tag_name))))
                .check(matches(isDisplayed()))

        onView(allOf(withText("tag2"), isDescendantOfA(withId(R.id.input_tag_name))))
                .perform(ViewActions.replaceText("tag1"))

        onView(allOf(withText("tag1"), isDescendantOfA(withId(R.id.input_tag_name))))
                .check(matches(isDisplayed()))

        val saveText = getResourceString(R.string.save)
        onView(withText(saveText)).inRoot(isDialog()).check(matches(isDisplayed()))
                .perform(click())

        val titleMergeDialog = getResourceString(R.string.dialog_tag_conflict_title)
        onView(withText(titleMergeDialog))
                .check(matches(isDisplayed()))

        val canonical = TagUtils.getCanonicalFromLexical(tagsBucket, "tag1")
        val mergeMessage = getResourceStringWithArgs(R.string.dialog_tag_conflict_message, canonical, "tag2", canonical)
        onView(withId(R.id.message))
                .check(matches(withText(mergeMessage)))

        val mergeText = getResourceString(R.string.dialog_tag_conflict_button_positive)
        onView(withText(mergeText))
                .perform(click())

        val tag1 = getTag("tag1")
        val tag2 = getTag("tag2")
        val tag3 = getTag("tag3")

        assertNull(tag2)
        assertNotNull(tag1)
        assertNotNull(tag3)

        assertEquals(note1.tags, listOf("tag1"))
        assertEquals(note2.tags, listOf("tag3", "tag1"))
        assertEquals(note3.tags, listOf("tag1", "tag3"))
    }

    @Test
    fun editTagFails() {
        createTag("tag1")
        // To edit tags, tags should belong a note
        val note = createNote("Hello World", listOf("tag1"))

        assertEquals(tagsBucket.count(), 1)
        assertEquals(notesBucket.count(), 1)

        launchFragment("tag1")

        val renameTagTitle = getResourceString(R.string.rename_tag)
        onView(withText(renameTagTitle)).check(matches(isDisplayed()))

        onView(allOf(withText("tag1"), isDescendantOfA(withId(R.id.input_tag_name))))
                .check(matches(isDisplayed()))
        onView(allOf(withText("tag1"), isDescendantOfA(withId(R.id.input_tag_name))))
                .perform(ViewActions.replaceText("tag5"))
        onView(allOf(withText("tag5"), isDescendantOfA(withId(R.id.input_tag_name))))
                .check(matches(isDisplayed()))

        tagsBucket.newObjectShouldFail = true

        val saveText = getResourceString(R.string.save)
        onView(withText(saveText))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
                .perform(click())

        val okText = getResourceString(android.R.string.ok)
        onView(withText(okText))
                .check(matches(isDisplayed()))
                .perform(click())

        val tag1 = getTag("tag1")
        val tag5 = getTag("tag5")

        assertNotNull(tag1)
        assertNull(tag5)
        assertEquals(tagsBucket.count(), 1)
        assertEquals(note.tags, listOf("tag1"))
    }

    private fun launchFragment(tagName: String) {
        val activityScenario = launchDialogFragmentInHiltContainer(R.style.Base_Theme_Simplestyle) {
            TagDialogFragment(getTag(tagName)!!)
        }

        activityScenario.onActivity {
            val fragment = it.supportFragmentManager.fragments[0] as DialogFragment
            assertNotNull(fragment.dialog)
            assertEquals(true, fragment.requireDialog().isShowing)
            it.supportFragmentManager.executePendingTransactions()
        }
    }
}
