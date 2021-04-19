package com.automattic.simplenote

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.utils.TagUtils
import com.automattic.simplenote.utils.TestBucket
import org.junit.Before
import java.security.SecureRandom

open class BaseUITest {
    private val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    private lateinit var application: SimplenoteTest
    protected lateinit var tagsBucket: TestBucket<Tag>

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext() as SimplenoteTest
        tagsBucket = application.tagsBucket as TestBucket<Tag>
        tagsBucket.clear()
    }

    protected fun getResourceString(id: Int): String? {
        val targetContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
        return targetContext.resources.getString(id)
    }

    protected fun getRandomString(len: Int): String {
        val random = SecureRandom()
        val bytes = ByteArray(len)
        random.nextBytes(bytes)

        return (bytes.indices)
                .map {
                    charPool[random.nextInt(charPool.size)]
                }.joinToString("")
    }

    protected fun createTag(tagName: String) {
        TagUtils.createTagIfMissing(tagsBucket, tagName)
    }
}
