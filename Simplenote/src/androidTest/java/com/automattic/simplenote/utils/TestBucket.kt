package com.automattic.simplenote.utils

import android.database.AbstractCursor
import android.database.CharArrayBuffer
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

    override fun allObjects(): ObjectCursor<T> {
        return TestObjectCursor(objects)
    }

    override fun reset() {
        clear()
    }

    fun clear() {
        objects.clear()
    }
}

class TestObjectCursor<T : Syncable>(private val objects: MutableList<T>) : AbstractCursor(), Bucket.ObjectCursor<T> {
    private val columns = arrayOf("simperiumKey", "object")

    override fun getCount(): Int {
        return objects.size
    }


    override fun getColumnNames(): Array<String> {
        return columns
    }

    override fun getColumnCount(): Int {
        TODO("Not yet implemented")
    }

    override fun getBlob(columnIndex: Int): ByteArray {
        throw RuntimeException("not implemented")
    }

    override fun getString(columnIndex: Int): String {
        throw RuntimeException("not implemented")
    }

    override fun copyStringToBuffer(columnIndex: Int, buffer: CharArrayBuffer?) {
        throw RuntimeException("not implemented")
    }

    override fun getShort(columnIndex: Int): Short {
        throw RuntimeException("not implemented")
    }

    override fun getInt(columnIndex: Int): Int {
        throw RuntimeException("not implemented")
    }

    override fun getLong(columnIndex: Int): Long {
        throw RuntimeException("not implemented")
    }

    override fun getFloat(columnIndex: Int): Float {
        throw RuntimeException("not implemented")
    }

    override fun getDouble(columnIndex: Int): Double {
        throw RuntimeException("not implemented")
    }

    override fun getType(columnIndex: Int): Int {
        throw RuntimeException("not implemented")
    }

    override fun isNull(column: Int): Boolean {
        throw RuntimeException("not implemented")
    }

    override fun getSimperiumKey(): String {
        return objects[position].simperiumKey
    }

    override fun getObject(): T {
        return objects[position]
    }

}
