package com.fleeksoft.ksoup.internal

/**
 * A com.fleeksoft.ksoup internal class (so don't use it as there is no contract API) that enables constraints on an Input Stream,
 * namely a maximum read size, and the ability to Thread.interrupt() the read.
 */
import okio.Buffer
import com.fleeksoft.ksoup.ported.BufferReader
import com.fleeksoft.ksoup.ported.System
import kotlin.math.min

class ConstrainableSource(
    bufferReader: BufferReader,
    maxSize: Int
) : BufferReader(bufferReader, maxSize) {

    companion object {
        private const val DEFAULT_SIZE = 1024 * 32

        fun wrap(bufferReader: BufferReader, maxSize: Int): ConstrainableSource {
            return if (bufferReader is ConstrainableSource) {
                bufferReader
            } else {
                ConstrainableSource(bufferReader, maxSize)
            }
        }
    }

    private val capped: Boolean = maxSize != 0
    private var startTime = System.nanoTime()
    private var timeout: Long = 0 // optional max time of request
    private var remaining = maxSize
    private var interrupted = false

    init {
        require(maxSize >= 0) { "maxSize must be 0 (unlimited) or larger" }
    }

    fun fullyRead(): Boolean = this.exhausted()

    override fun read(sink: ByteArray, offset: Int, byteCount: Int): Int {
        if (interrupted || capped && remaining <= 0) {
            return -1
        }
        if (expired()) {
            throw Exception("Read timeout")
        }

        val toRead = if (capped && byteCount > remaining) remaining else byteCount

        return try {
            val read = getActiveBuffer().read(
                sink = sink,
                offset = 0,
                byteCount = min(toRead, getActiveBuffer().size.toInt())
            )
            if (!this.exhausted()) {
                remaining -= read
            }
            read
        } catch (e: Exception) {
            0
        }
    }

    fun readToByteBuffer(max: Int): BufferReader {
        require(max >= 0) { "maxSize must be 0 (unlimited) or larger" }
        val localCapped = max > 0
        val bufferSize = if (localCapped && max < DEFAULT_SIZE) max else DEFAULT_SIZE

        var read: Int
        var remaining = max

        val buffer = Buffer()

        while (true) {
            val size: Int = min(bufferSize, this.getActiveBuffer().size.toInt())
            val readBuffer = ByteArray(size)
            read = this.read(readBuffer, 0, size)
            if (read > 0) {
                buffer.write(readBuffer, 0, read)
            }
            if (this.exhausted()) break
            if (localCapped) {
                if (read >= remaining) {
                    break
                }
                remaining -= read
            }

        }
        return BufferReader(buffer)
    }

    fun timeout(startTimeNanos: Long, timeoutMillis: Long): ConstrainableSource {
        this.startTime = startTimeNanos
        this.timeout = timeoutMillis * 1_000_000
        return this
    }

    private fun expired(): Boolean {
        if (timeout == 0L) {
            return false
        }
        val now = System.nanoTime()
        val dur = now - startTime
        return dur > timeout
    }
}
