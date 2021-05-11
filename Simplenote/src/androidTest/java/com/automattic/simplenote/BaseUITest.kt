package com.automattic.simplenote

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.automattic.simplenote.models.Note
import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.utils.TagUtils
import com.automattic.simplenote.utils.TestBucket
import com.automattic.simplenote.utils.getRandomStringOfLen
import com.simperium.client.BucketObjectMissingException
import org.junit.After
import org.junit.Before
import java.security.SecureRandom

open class BaseUITest {
    protected lateinit var application: SimplenoteTest
    protected lateinit var tagsBucket: TestBucket<Tag>
    protected lateinit var notesBucket: TestBucket<Note>

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext() as SimplenoteTest
        // Make sure to use TestBucket buckets in UI tests
        application.useTestBucket = true

        tagsBucket = application.tagsBucket as TestBucket<Tag>
        tagsBucket.clear()
        tagsBucket.newObjectShouldFail = false

        notesBucket = application.notesBucket as TestBucket<Note>
        notesBucket.clear()
        notesBucket.newObjectShouldFail = false
    }

    @After
    fun cleanup() {
        application.useTestBucket = false
    }

    protected fun getResourceString(id: Int): String {
        val targetContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
        return targetContext.resources.getString(id)
    }

    protected fun getResourceStringWithArgs(id: Int, vararg args: Any): String {
        val targetContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
        return targetContext.resources.getString(id, *args)
    }

    protected fun getRandomString(len: Int): String {
        return getRandomStringOfLen(len)
    }

    protected fun createTag(tagName: String): Tag {
        TagUtils.createTagIfMissing(tagsBucket, tagName)
        return tagsBucket.getObject(TagUtils.hashTag(tagName))
    }

    protected fun createNote(content: String, tags: List<String>): Note {
        val note = notesBucket.newObject(content.hashCode().toString())
        note.content = content
        note.tags = tags

        return note
    }

    protected fun getTag(tagName: String): Tag? {
        val hashName = TagUtils.hashTag(tagName)
        return try {
            tagsBucket.getObject(hashName)
        } catch (b: BucketObjectMissingException) {
            null
        }
    }
}
