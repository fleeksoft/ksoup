package com.fleeksoft.ksoup.internal

import com.fleeksoft.io.ByteArrayInputStream
import com.fleeksoft.io.FilterInputStream
import com.fleeksoft.io.InputStream
import com.fleeksoft.ksoup.io.internal.ControllableInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal class ControllableInputStreamTest {
    @Test
    fun respectsMaxCapDuringFill() {
        val data: ByteArray = "0123456789".encodeToByteArray() // 10 bytes
        val counting = CountingInputStream(ByteArrayInputStream(data))

        val input = ControllableInputStream.wrap(counting, 5) // cap at 5 bytes
        val buf = ByteArray(10)

        val read: Int = input.read(buf)
        assertEquals(5, read, "should only read up to cap")
        assertEquals(5, counting.count, "underlying stream should not be pulled past cap")
        assertFalse(input.baseReadFully(), "cap hit is not EOF")

        val second: Int = input.read(buf)
        assertEquals(-1, second, "further reads return -1 once cap is exhausted")
        assertFalse(input.baseReadFully(), "still not true EOF")
        input.close()
    }

    @Test
    fun compactsBufferWithActiveMark() {
        val size: Int = SharedConstants.DefaultBufferSize * 2
        val data = ByteArray(size)
        for (i in 0..<size) data[i] = (i % 256).toByte()

        val `in`: ControllableInputStream = ControllableInputStream.wrap(ByteArrayInputStream(data), 0)

        val first = ByteArray(500)
        assertEquals(500, `in`.read(first))

        `in`.mark(SharedConstants.DefaultBufferSize) // mark at logical pos 500

        val consume = ByteArray(SharedConstants.DefaultBufferSize)
        val firstRead: Int = `in`.read(consume) // serves remainder of current buffer (BufferSize - 500)
        assertEquals(SharedConstants.DefaultBufferSize - 500, firstRead)

        val more = ByteArray(1000)
        val secondRead: Int = `in`.read(more) // triggers fill() with active mark, then consumes from freshly filled buffer
        assertEquals(SharedConstants.DefaultBufferSize - firstRead, secondRead)

        `in`.reset() // should rewind to mark despite prior compaction

        val reread = ByteArray(1000)
        assertEquals(1000, `in`.read(reread))
        for (i in reread.indices) {
            assertEquals(data[500 + i], reread[i], "byte mismatch at $i")
        }
        `in`.close()
    }

    private class CountingInputStream(input: InputStream?) : FilterInputStream(input) {
        var count: Int = 0

        override fun read(bytes: ByteArray, off: Int, len: Int): Int {
            val r: Int = super.read(bytes, off, len)
            if (r > 0) count += r
            return r
        }

        override fun read(): Int {
            val r: Int = super.read()
            if (r != -1) count++
            return r
        }
    }
}
