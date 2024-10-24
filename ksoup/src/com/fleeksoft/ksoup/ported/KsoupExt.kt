package com.fleeksoft.ksoup.ported

import com.fleeksoft.ksoup.KsoupEngineInstance
import com.fleeksoft.ksoup.internal.SharedConstants
import com.fleeksoft.ksoup.io.FileSource
import com.fleeksoft.ksoup.io.SourceReader
import com.fleeksoft.ksoup.io.from
import com.fleeksoft.ksoup.ported.io.BufferedReader
import com.fleeksoft.ksoup.ported.io.InputSourceReader
import com.fleeksoft.ksoup.ported.io.Reader
import com.fleeksoft.charset.Charset
import com.fleeksoft.charset.Charsets
import com.fleeksoft.charset.toByteArray

fun String.openSourceReader(charset: Charset? = null): SourceReader =
    SourceReader.from(charset?.let { this.toByteArray(it) } ?: this.encodeToByteArray())

fun ByteArray.openSourceReader(): SourceReader = SourceReader.from(this)
fun SourceReader.toReader(charset: Charset = Charsets.UTF8, chunkSize: Int = SharedConstants.DEFAULT_BYTE_BUFFER_SIZE): Reader =
    BufferedReader(InputSourceReader(this, charset = charset), chunkSize)

fun String.toByteArray(): ByteArray = this.encodeToByteArray()

fun String.toSourceFile(): FileSource = KsoupEngineInstance.ksoupEngine.pathToFileSource(this)


inline fun <T : Comparable<T>> Array<out T?>.binarySearch(element: T): Int {
    var low = 0
    var high = this.size - 1

    while (low <= high) {
        val mid = (low + high).ushr(1) // safe from overflows
        val midVal = get(mid)
        val cmp = compareValues(midVal, element)

        if (cmp < 0)
            low = mid + 1
        else if (cmp > 0)
            high = mid - 1
        else
            return mid // key found
    }
    return -(low + 1)  // key not found
}

inline fun IntArray.binarySearch(key: Int): Int {
    var low = 0
    var high = this.size - 1

    while (low <= high) {
        val mid = (low + high) ushr 1
        val midVal = this[mid]

        if (midVal < key) low = mid + 1
        else if (midVal > key) high = mid - 1
        else return mid // key found
    }
    return -(low + 1) // key not found.
}


inline fun <T> Array<T>.binarySearchBy(comparison: (T) -> Int): Int {

    var low = 0
    var high = size - 1

    while (low <= high) {
        val mid = (low + high).ushr(1) // safe from overflows
        val midVal = get(mid)
        val cmp = comparison(midVal)

        if (cmp < 0)
            low = mid + 1
        else if (cmp > 0)
            high = mid - 1
        else
            return mid // key found
    }
    return -(low + 1)  // key not found
}