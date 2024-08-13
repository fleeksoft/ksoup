/*
 * Copyright (C) 2018 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:OptIn(InternalIoApi::class)

package com.fleeksoft.ksoup.ported.io

import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.InternalIoApi
import kotlinx.io.RawSource
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlin.math.max
import kotlin.math.min

/**
 * Builds a buffered source that can rewind to a marked position earlier in the stream.
 *
 *
 * Mark potential positions to rewind back to with [.mark]; rewind back to these positions
 * with [.reset]. Both operations apply to the position in the [buffered][.source]; resetting will impact the buffer.
 *
 *
 * When marking it is necessary to specify how much data to retain. Once you advance above this
 * limit, the mark is discarded and resetting is not permitted. This may be used to lookahead a
 * fixed number of bytes without loading an entire stream into memory. To reset an arbitrary
 * number of bytes use `mark(Long#MAX_VALUE)`.
 */
class SourceMarker(source: Source) {
    /*
     * This class wraps the underlying source in a MarkSource to support mark and reset. It creates a
     * BufferedSource for the caller so that it can track its offsets and manipulate its buffer.
     */
    /**
     * The offset into the underlying source. To compute the user's offset start with this and
     * subtract userBuffer.size().
     */
    private var offset: Long = 0

    /** The offset of the earliest mark, or -1 for no mark.  */
    private var mark: Long = -1L

    /** The offset of the latest readLimit, or -1 for no mark.  */
    private var limit: Long = -1L

    private var closed: Boolean = false

    private val markSource: MarkSource = MarkSource(source)
    private val userSource: Source = markSource.buffered()

    /** A copy of the underlying source's data beginning at `mark`.  */
    private val markBuffer: Buffer = Buffer()

    /** Just the userSource's buffer.  */
    private val userBuffer: Buffer = userSource.buffer

    fun source(): Source {
        return userSource
    }

    /**
     * Marks the current position in the stream as one to potentially return back to. Returns the
     * offset of this position. Call [.reset] with this position to return to it later. It
     * is an error to call [.reset] after consuming more than `readLimit` bytes from
     * [the source][.source].
     */
    fun mark(readLimit: Long): Long {
        require(readLimit >= 0L) { "readLimit < 0: $readLimit" }

        check(!closed) { "closed" }

        // Mark the current position in the buffered source.
        val userOffset: Long = offset - userBuffer.size

        // If this is a new mark promote userBuffer data into the markBuffer.
        if (mark == -1L) {
            markBuffer.write(userBuffer, userBuffer.size)
            mark = userOffset
            offset = userOffset
        }

        // Grow the limit if necessary.
        var newMarkBufferLimit = userOffset + readLimit
        if (newMarkBufferLimit < 0) newMarkBufferLimit = Long.MAX_VALUE // Long overflow!

        limit = max(limit, newMarkBufferLimit)

        return userOffset
    }

    /** Resets [the source][.source] to `userOffset`.  */
    fun reset(userOffset: Long = offset) {
        check(!closed) { "closed" }

        if (userOffset < mark // userOffset is before mark.
            || userOffset > limit // userOffset is beyond limit.
            || userOffset > mark + markBuffer.size // userOffset is in the future.
            || offset - userBuffer.size > limit
        ) { // Stream advanced beyond limit.
            throw IOException("cannot reset to $userOffset: out of range")
        }

        // Clear userBuffer to cause data at 'offset' to be returned by the next read.
        offset = userOffset
        userBuffer.clear()
    }

    inner class MarkSource(private val source: Source) : RawSource {

        override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
            check(!closed) { "closed" }

            // If there's no mark, go to the underlying source.
            if (mark == -1L) {
                val result: Long = source.readAtMostTo(sink, byteCount)
                if (result == -1L) return -1L
                offset += result
                return result
            }

            // If we can read from markBuffer, do that.
            if (offset < mark + markBuffer.size) {
                val posInBuffer = offset - mark
                val result: Long = min(byteCount, markBuffer.size - posInBuffer)
                markBuffer.copyTo(sink, posInBuffer, result)
                offset += result
                return result
            }

            // If we can write to markBuffer, do that.
            if (offset < limit) {
                val byteCountBeforeLimit: Long = limit - (mark + markBuffer.size)
                val result: Long = source.readAtMostTo(markBuffer, min(byteCount, byteCountBeforeLimit))
                if (result == -1L) return -1L
                markBuffer.copyTo(sink, markBuffer.size - result, result)
                offset += result
                return result
            }

            // Attempt to read past the limit. Data will not be saved.
            val result: Long = source.readAtMostTo(sink, byteCount)
            if (result == -1L) return -1L

            // We read past the limit. Discard marked data.
            markBuffer.clear()
            mark = -1L
            limit = -1L
            return result
        }

        override fun close() {
            if (closed) return

            closed = true
            markBuffer.clear()
            source.close()
        }
    }
}