package com.automattic.simplenote.utils

import android.database.AbstractCursor
import android.database.CharArrayBuffer
import com.simperium.client.*
import org.json.JSONArray
import java.util.*

abstract class TestBucket<T : BucketObject>(name: String) : Bucket<T>(null, name, null, null, null, null) {
    // Store objects in memory
    private val objects: MutableList<T> = mutableListOf()
    var newObjectShouldFail = false

    private val onSaveListeners = Collections.synchronizedSet(HashSet<OnSaveObjectListener<T>>())
    private val onDeleteListeners = Collections.synchronizedSet(HashSet<OnDeleteObjectListener<T>>())
    private val onChangeListeners = Collections.synchronizedSet(HashSet<OnNetworkChangeListener<T>>())
    private val onSyncListeners = Collections.synchronizedSet(HashSet<OnSyncObjectListener<T>>())

    override fun addOnDeleteObjectListener(listener: OnDeleteObjectListener<T>?) {
        onDeleteListeners.add(listener)
    }

    override fun addOnSaveObjectListener(listener: OnSaveObjectListener<T>?) {
        onSaveListeners.add(listener)
    }

    override fun addOnNetworkChangeListener(listener: OnNetworkChangeListener<T>?) {
        onChangeListeners.add(listener)
    }

    override fun addOnSyncObjectListener(listener: OnSyncObjectListener<T>?) {
        onSyncListeners.add(listener)
    }

    override fun newObject(key: String?): T {
        if (newObjectShouldFail) {
            throw BucketObjectNameInvalid(key)
        }

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

        // notify listeners
        onSyncListeners.forEach {
            it.onSyncObject(this, `object`?.simperiumKey)
        }
    }

    override fun remove(`object`: T?) {
        `object`?.let {
            objects.removeIf { it.simperiumKey == `object`.simperiumKey }
        }

        // notify listeners
        onDeleteListeners.forEach {
            it.onDeleteObject(this, `object`)
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
        if (columnIndex == 0) {
            return position.toLong()
        }

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

class TestQuery<T : BucketObject>(private val objects: MutableList<T>) : Query<T>() {
    override fun execute(): Bucket.ObjectCursor<T> {
        // Filter objects by the where clauses
        val filteredObjects: MutableList<T> = filterObjects()

        return TestObjectCursor(filteredObjects)
    }

    private fun filterObjects(): MutableList<T> {
        return conditions.fold(objects, { currentObjects: MutableList<T>, condition: Condition ->
            when (condition.comparisonType) {
                ComparisonType.EQUAL_TO -> objects.filter { compare(it.properties.get(condition.key), condition.subject) }
                ComparisonType.NOT_EQUAL_TO -> objects.filter { !compare(it.properties.get(condition.key), condition.subject) }
                ComparisonType.LIKE -> objects.filter { compareLike(it.properties.get(condition.key), (condition.subject)) }
                ComparisonType.NOT_LIKE -> objects.filter { !compareLike(it.properties.get(condition.key), (condition.subject.toString())) }
                else -> currentObjects // The rest of comparison types are not used in the app
            } as MutableList
        })
    }

    private fun compare(left: Any, right: Any): Boolean {
        if (left is JSONArray) {
            for (i in 0 until left.length()) {
                val o = left.get(i)
                if (o == right) {
                    return true
                }
            }

            return false
        } else {
            return left == right
        }
    }

    private fun compareLike(left: Any, right: Any): Boolean {
        if (left is JSONArray) {
            for (i in 0 until left.length()) {
                val o = left.get(i)
                if (right.toString() == "%%" || o.toString().contains(right.toString())) {
                    return true
                }
            }

            return false
        } else {
            return right.toString() == "%%" || left.toString().contains(right.toString().replace("%", ""))
        }
    }

    override fun count(): Int {
        return filterObjects().size
    }
}
