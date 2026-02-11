package com.fleeksoft.ksoup.io.internal

import com.fleeksoft.io.*
import com.fleeksoft.io.exception.IOException
import kotlin.math.max
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
    private var contentLength = -1

    override fun read(bytes: ByteArray, off: Int, len: Int): Int {
        var len = len
        val capped = maxSize != 0
        if (interrupted || capped && remaining <= 0)
            return -1

        if (capped && len > remaining)
            len = remaining
        buff.capRemaining(if (capped) remaining else Int.MAX_VALUE)

        val read = super.read(bytes, off, len)
        if (read == -1) {
            contentLength = readPos
        } else {
            if (capped && read > 0) {
                remaining -= read // track bytes returned to the caller
            }
            readPos += read
        }

        return read
    }

    override fun markSupported(): Boolean {
        return true
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
        if (remaining < 0) remaining = 0;
        maxSize = newMax
        buff.capRemaining(if (newMax == 0) Int.MAX_VALUE else remaining)
    }

    override fun mark(readLimit: Int) {
        markPos = readPos
        buff.setMark()
    }

    fun baseReadFully(): Boolean {
        return buff.baseReadFully()
    }

    fun resetFullyRead() {
        buff.resetFullyRead()
    }

    override fun reset() {
        if (markPos < 0) throw IOException("Resetting to invalid mark")
        buff.rewindToMark()
        buff.clearMark()
        if (maxSize != 0) {
            remaining = maxSize - markPos
            buff.capRemaining(remaining)
        } else {
            remaining = 0
            buff.capRemaining(Int.MAX_VALUE)
        }
        readPos = markPos // readPos is used for progress emits
        markPos = -1
    }

    companion object {

        fun wrap(input: InputStream, maxSize: Int): ControllableInputStream {
            return input as? ControllableInputStream ?: ControllableInputStream(SimpleBufferedInput(input), maxSize)
        }

        fun readToByteBuffer(input: InputStream, max: Int): ByteBuffer {
            require(max >= 0) { "maxSize must be 0 (unlimited) or larger" }
            val capped = max > 0
            val readBuf = SimpleBufferedInput.BufferPool.borrow()
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
                        val newCapacity = max(outBuf.capacity() * 1.5, (outBuf.capacity() + read).toDouble()).toInt()
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
                SimpleBufferedInput.BufferPool.release(readBuf)
            }
        }
    }

}