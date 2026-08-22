package com.prismspace.container.utils

import java.util.Arrays

object ArrayUtils {
    @JvmStatic
    fun <T> trimToSize(array: Array<T>?, size: Int): Array<T>? {
        return when {
            array == null || size == 0 -> null
            array.size == size -> array
            else -> Arrays.copyOf(array, size)
        }
    }

    @JvmStatic
    fun push(array: Array<Any>, item: Any): Array<Any> {
        val longer = arrayOfNulls<Any>(array.size + 1)
        System.arraycopy(array, 0, longer, 0, array.size)
        longer[array.size] = item
        @Suppress("UNCHECKED_CAST")
        return longer as Array<Any>
    }

    @JvmStatic
    fun <T> contains(array: Array<T>?, value: T): Boolean = indexOf(array, value) != -1

    @JvmStatic
    fun contains(array: IntArray?, value: Int): Boolean {
        if (array == null) return false
        for (element in array) {
            if (element == value) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun <T> indexOf(array: Array<T>?, value: T): Int {
        if (array == null) return -1
        for (i in array.indices) {
            if (array[i] == value) return i
        }
        return -1
    }

    @JvmStatic
    fun protoIndexOf(array: Array<Class<*>?>?, type: Class<*>): Int {
        if (array == null) return -1
        for (i in array.indices) {
            if (array[i] == type) return i
        }
        return -1
    }

    @JvmStatic
    fun indexOfFirst(array: Array<Any?>?, type: Class<*>): Int {
        if (!isEmpty(array)) {
            var index = -1
            for (one in array!!) {
                index++
                if (one != null && type == one.javaClass) {
                    return index
                }
            }
        }
        return -1
    }

    @JvmStatic
    fun protoIndexOf(array: Array<Class<*>?>?, type: Class<*>, sequence: Int): Int {
        if (array == null) {
            return -1
        }
        var cursor = sequence
        while (cursor < array.size) {
            if (type == array[cursor]) {
                return cursor
            }
            cursor++
        }
        return -1
    }

    @JvmStatic
    fun indexOfObject(array: Array<Any?>?, type: Class<*>, sequence: Int): Int {
        if (array == null) {
            return -1
        }
        var cursor = sequence
        while (cursor < array.size) {
            if (type.isInstance(array[cursor])) {
                return cursor
            }
            cursor++
        }
        return -1
    }

    @JvmStatic
    fun indexOf(array: Array<Any?>?, type: Class<*>, sequence: Int): Int {
        if (!isEmpty(array)) {
            var index = -1
            var remaining = sequence
            for (one in array!!) {
                index++
                if (one != null && one.javaClass == type) {
                    if (--remaining <= 0) {
                        return index
                    }
                }
            }
        }
        return -1
    }

    @JvmStatic
    fun indexOfLast(array: Array<Any?>?, type: Class<*>): Int {
        if (!isEmpty(array)) {
            for (i in array!!.size downTo 1) {
                val one = array[i - 1]
                if (one != null && one.javaClass == type) {
                    return i - 1
                }
            }
        }
        return -1
    }

    @JvmStatic
    fun <T> isEmpty(array: Array<T>?): Boolean = array == null || array.isEmpty()

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <T> getFirst(args: Array<Any?>?, clazz: Class<*>): T? {
        val index = indexOfFirst(args, clazz)
        return if (index != -1) args!![index] as T else null
    }

    @JvmStatic
    @Throws(ArrayIndexOutOfBoundsException::class)
    fun checkOffsetAndCount(arrayLength: Int, offset: Int, count: Int) {
        if ((offset or count) < 0 || offset > arrayLength || arrayLength - offset < count) {
            throw ArrayIndexOutOfBoundsException(offset)
        }
    }
}

