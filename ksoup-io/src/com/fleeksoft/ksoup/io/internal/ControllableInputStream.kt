package com.fleeksoft.ksoup.io.internal

import com.fleeksoft.io.*
import kotlin.math.min

/**
 * A Ksoup internal class (so don't use it as there is no contract API) that enables controls on a buffered input stream,
 * namely a maximum read size, and the ability to Thread.interrupt() the read.
 */

class ControllableInputStream private constructor(val buff: SimpleBufferedInput, private var maxSize: Int) :
    FilterInputStream(buff) {
    private var remaining: Int = maxSize
    private var markPos = -1
    private var interrupted = false
    private var allowClose = true
    private var readPos = 0

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        var len = len
        val capped = maxSize != 0
        if (interrupted || capped && remaining <= 0)
            return -1

        if (capped && len > remaining)
            len = remaining

        val read = super.read(b, off, len)
        if (read != -1) {
            remaining -= read
            readPos += read
        }
        return read
    }


    fun inputStream(): BufferedInputStream {
        return BufferedInputStream(buff)
    }

    override fun close() {
        if (allowClose) super.close()
    }

    fun allowClose(allowClose: Boolean) {
        this.allowClose = allowClose
    }

    fun max(): Int {
        return maxSize
    }

    fun max(newMax: Int) {
        remaining += newMax - maxSize
        maxSize = newMax
    }

    fun baseReadFully(): Boolean {
        return buff.baseReadFully()
    }

    override fun mark(readlimit: Int) {
        super.mark(readlimit)
        markPos = maxSize - remaining
    }

    override fun reset() {
        super.reset()
        remaining = maxSize - markPos
        readPos = markPos
    }

    companion object {

        fun wrap(input: InputStream, maxSize: Int): ControllableInputStream {
            return input as? ControllableInputStream ?: ControllableInputStream(SimpleBufferedInput(input), maxSize)
        }

        fun readToByteBuffer(input: InputStream, max: Int): ByteBuffer {
            require(max >= 0) { "maxSize must be 0 (unlimited) or larger" }
            val capped = max > 0
            val readBuf = SimpleBufferedInput.Companion.BufferPool.borrow()
            val outSize = (if (capped) min(max, Constants.DEFAULT_BYTE_BUFFER_SIZE) else Constants.DEFAULT_BYTE_BUFFER_SIZE).coerceAtLeast(0)
            var outBuf = ByteBufferFactory.allocate(outSize)

            try {
                var remaining = max
                var read: Int
                while (input.read(
                        readBuf,
                        0,
                        (if (capped) min(remaining, Constants.DEFAULT_BYTE_BUFFER_SIZE) else Constants.DEFAULT_BYTE_BUFFER_SIZE).coerceAtLeast(0)
                    ).also { read = it } != -1
                ) {
                    if (outBuf.remaining() < read) {
                        val newCapacity =
                            (outBuf.capacity() * 1.5).toLong().coerceAtLeast(
                                (outBuf.capacity() + read).toLong()
                            ).toInt()
                        val newBuffer = ByteBufferFactory.allocate(newCapacity)
                        outBuf.flipExt()
                        newBuffer.put(outBuf)
                        outBuf = newBuffer
                    }
                    outBuf.put(readBuf, 0, read)
                    if (capped) {
                        remaining -= read
                        if (remaining <= 0) break
                    }
                }
                outBuf.flipExt()
                return outBuf
            } finally {
                SimpleBufferedInput.Companion.BufferPool.release(readBuf)
            }
        }
    }

}