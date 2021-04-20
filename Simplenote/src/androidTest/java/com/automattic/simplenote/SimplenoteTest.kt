package com.automattic.simplenote

import com.automattic.simplenote.models.Note
import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.utils.TestBucket
import com.simperium.client.Bucket

class SimplenoteTest : Simplenote() {
    // Decides whether use the default bucket implementation or our own TestBucket
    var useDefaultBucketImpl = true

    private val tagsBucket = object : TestBucket<Tag>("tags") {
        override fun build(key: String?): Tag {
            return Tag(key)
        }
    }

    private val notesBucket = object : TestBucket<Note>("notes") {
        override fun build(key: String?): Note {
            return Note(key)
        }
    }

    override fun getTagsBucket(): Bucket<Tag> {
        if (useDefaultBucketImpl) {
            return super.getTagsBucket()
        }

        return tagsBucket
    }

    override fun getNotesBucket(): Bucket<Note> {
        if (useDefaultBucketImpl) {
            return super.getNotesBucket()
        }

        return notesBucket
    }
}
