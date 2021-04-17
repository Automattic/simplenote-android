package com.automattic.simplenote

import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.utils.TestBucket
import com.simperium.client.Bucket

class SimplenoteTest : Simplenote() {
    private val tagsBucket = object : TestBucket<Tag>("tags") {
        override fun build(key: String?): Tag {
            return Tag(key)
        }
    }

    override fun getTagsBucket(): Bucket<Tag> {
        return tagsBucket
    }
}
