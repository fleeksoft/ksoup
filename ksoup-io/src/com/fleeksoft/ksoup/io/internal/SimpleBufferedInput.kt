package com.fleeksoft.ksoup.io.internal

import com.fleeksoft.io.Constants
import com.fleeksoft.io.FilterInputStream
import com.fleeksoft.io.InputStream
import com.fleeksoft.io.exception.IOException
import com.fleeksoft.ksoup.internal.SoftPool
import kotlin.math.max
import kotlin.math.min

class SimpleBufferedInput(private val inputStream: InputStream) : FilterInputStream(inputStream) {
    private var bufPos = 0
    private var bufLength = 0
    private var bufMark = -1
    private var inReadFully = false
    private var byteBuf: ByteArray? = null
    private var capRemaining = Int.MAX_VALUE // how many bytes we are allowed to pull from the underlying stream

    override fun read(): Int {
        if (bufPos >= bufLength) {
            fill()
            if (bufPos >= bufLength) return -1
        }
        return byteBuf!![bufPos++].toInt() and 0xff
    }

    override fun read(bytes: ByteArray, off: Int, len: Int): Int {
        if (off < 0 || len < 0 || len > bytes.size - off) {
            throw IndexOutOfBoundsException()
        } else if (len == 0) {
            return 0
        }
        var bufAvail = bufLength - bufPos
        if (bufAvail <= 0) {
            fill()
            bufAvail = bufLength - bufPos
        }
        val read = min(bufAvail, len)
        if (read <= 0) return -1
        byteBuf!!.copyInto(bytes, destinationOffset = off, startIndex = bufPos, endIndex = bufPos + read)
        bufPos += read
        return read
    }

    private fun fill() {
        if (inReadFully) return
        if (byteBuf == null) { // get one on first demand
            byteBuf = BufferPool.borrow()
        }

        compact()
        bufLength = bufPos
        var toRead: Int = min(byteBuf!!.size - bufPos, capRemaining)
        if (toRead <= 0) return
        var read: Int = inputStream.read(byteBuf!!, bufPos, toRead)
        if (read > 0) {
            bufLength = read + bufPos
            capRemaining -= read
            while (byteBuf!!.size - bufLength > 0 && capRemaining > 0) { // read in more if we have space, without blocking
                if (inputStream.available() < 1) break
                toRead = min(byteBuf!!.size - bufLength, capRemaining)
                if (toRead <= 0) break
                read = inputStream.read(byteBuf!!, bufLength, toRead)
                if (read <= 0) break
                bufLength += read
                capRemaining -= read
            }
        }
        if (read == -1) inReadFully = true
    }

    fun getBuf(): ByteArray {
        return byteBuf!!
    }

    fun baseReadFully(): Boolean {
        return inReadFully
    }

    fun resetFullyRead() {
        inReadFully = false
    }

    override fun available(): Int {
        val buffered = if (byteBuf != null) (bufLength - bufPos) else 0
        if (buffered > 0) {
            return buffered // doesn't include those in.available(), but mostly used as a block test
        }
        val avail = if (inReadFully) 0 else inputStream.available()
        return avail
    }

    fun capRemaining(newRemaining: Int) {
        capRemaining = max(0, newRemaining)
    }

    fun setMark() {
        bufMark = bufPos
    }

    fun rewindToMark() {
        if (bufMark < 0) throw IOException("Resetting to invalid mark")
        bufPos = bufMark
    }

    fun clearMark() {
        bufMark = -1
    }

    private fun compact() {
        if (byteBuf == null || bufPos == 0) return
        val keepFrom = if (bufMark >= 0) bufMark else bufPos
        if (keepFrom <= 0) return

        val remaining = bufLength - keepFrom
        if (remaining > 0) {
            byteBuf?.copyInto(
                destination = byteBuf!!,
                destinationOffset = 0,
                startIndex = keepFrom,
                endIndex = keepFrom + remaining
            )
        }
        bufLength = remaining
        bufPos -= keepFrom
        if (bufMark >= 0) {
            bufMark -= keepFrom
        }
    }

    override fun close() {
        super.close()
        if (byteBuf == null) return
        BufferPool.release(byteBuf!!)
        byteBuf = null
    }

    companion object {
        val BufferPool: SoftPool<ByteArray> = SoftPool<ByteArray> { ByteArray(Constants.DEFAULT_BYTE_BUFFER_SIZE) }
    }
}