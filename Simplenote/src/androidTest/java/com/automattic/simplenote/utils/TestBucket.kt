package com.automattic.simplenote.utils

import android.database.AbstractCursor
import android.database.CharArrayBuffer
import com.simperium.client.*

abstract class TestBucket<T : BucketObject>(name: String) : Bucket<T>(null, name, null, null, null, null) {
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
        `object`?.let { o ->
            o.bucket = this
            // Find object by Simperium key
            val index = objects.indexOfFirst { it.simperiumKey == o.simperiumKey }
            if (index < 0) { // If object does not exists, add it
                objects.add(o)
            } else {
                // If object exists, replace it
                objects.set(index, o)
            }
        }
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

    override fun query(): Query<T> {
        return TestQuery(objects)
    }

    fun clear() {
        objects.clear()
    }

}

class TestObjectCursor<T : BucketObject>(private val objects: MutableList<T>) : AbstractCursor(), Bucket.ObjectCursor<T> {
    private val columns = arrayOf("simperiumKey", "object")

    override fun getCount(): Int {
        return objects.size
    }


    override fun getColumnNames(): Array<String> {
        return columns
    }

    override fun getColumnCount(): Int {
        return columns.size
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

class TestQuery<T : BucketObject>(private val objects:  MutableList<T>) : Query<T>() {
    override fun execute(): Bucket.ObjectCursor<T> {
        // Filter objects by the where clauses
        val filteredObjects: MutableList<T> = conditions.fold(objects, { currentObjects: MutableList<T>, condition: Condition ->
            when(condition.comparisonType) {
                ComparisonType.EQUAL_TO -> objects.filter { it.properties.get(condition.key).equals(condition.subject) }
                ComparisonType.NOT_EQUAL_TO -> objects.filter { !it.properties.get(condition.key).equals(condition.subject) }
                ComparisonType.LIKE -> objects.filter { it.properties.get(condition.key).toString().contains(condition.subject.toString()) }
                ComparisonType.NOT_LIKE -> objects.filter { !it.properties.get(condition.key).toString().contains(condition.subject.toString()) }
                else -> currentObjects // The rest of comparison types are not used in the app
            } as MutableList
        })

        return TestObjectCursor(filteredObjects)
    }
}
