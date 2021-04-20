package com.automattic.simplenote.utils

import com.simperium.client.Bucket
import com.simperium.client.BucketObjectMissingException
import com.simperium.client.Syncable

abstract class TestBucket<T : Syncable>(name: String) : Bucket<T>(null, name, null, null, null, null) {
    // Store objects in memory
    private val objects: MutableList<T> = mutableListOf()

    override fun newObject(key: String?): T {
        val o = build(key)
        o.bucket = this

        objects.add(o)

        return o
    }

    abstract fun build(key: String?): T

    override fun count(): Int {
        return objects.size
    }

    override fun getObject(uuid: String?): T {
        return uuid?.let {
            objects.find { it.simperiumKey == uuid }
        } ?: throw BucketObjectMissingException()
    }

    override fun sync(`object`: T?) {
        // Do not do anything
    }

    override fun remove(`object`: T?) {
        `object`?.let {
            objects.remove(it)
        }
    }

    fun clear() {
        objects.clear()
    }
}
