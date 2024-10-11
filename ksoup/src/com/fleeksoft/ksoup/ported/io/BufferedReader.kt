package com.fleeksoft.ksoup.ported.io

import com.fleeksoft.ksoup.internal.SharedConstants
import com.fleeksoft.ksoup.exception.IOException
import kotlin.math.min


/**
 * Creates a buffering character-input stream that uses an input buffer of
 * the specified size.
 *
 * @param  reader A Reader
 * @param sz Input-buffer size
 *
 * @throws IllegalArgumentException  If `sz <= 0`
 */
class BufferedReader(reader: Reader, sz: Int = SharedConstants.DEFAULT_CHAR_BUFFER_SIZE) : Reader() {
    companion object {
        private const val INVALIDATED: Int = -2
        private const val UNMARKED: Int = -1

        private const val DEFAULT_EXPECTED_LINE_LENGTH: Int = 80
    }

    private var reader: Reader? = reader

    private var cb: CharArray? = null
    private var nChars = 0
    private var nextChar: Int = 0

    private var markedChar = UNMARKED
    private var readAheadLimit = 0 /* Valid only when markedChar > 0 */

    /** If the next character is a line feed, skip it  */
    private var skipLF = false

    /** The skipLF flag when the mark was set  */
    private var markedSkipLF = false


    init {
        require(sz > 0) { "Buffer size <= 0" }
        cb = CharArray(sz)
        nChars = 0
        nextChar = nChars
    }

    /** Checks to make sure that the stream has not been closed  */
    private fun ensureOpen() {
        if (reader == null) throw IOException("Stream closed")
    }

    /**
     * Fills the input buffer, taking the mark into account if it is valid.
     */
    private fun fill() {
        val dst: Int
        if (markedChar <= UNMARKED) {/* No mark */
            dst = 0
        } else {/* Marked */
            val delta = nextChar - markedChar
            if (delta >= readAheadLimit) {/* Gone past read-ahead limit: Invalidate mark */
                markedChar = INVALIDATED
                readAheadLimit = 0
                dst = 0
            } else {
                if (readAheadLimit <= cb!!.size) {/* Shuffle in the current buffer */

//                    todo://test it
                    cb!!.copyInto(destination = cb!!, destinationOffset = 0, startIndex = markedChar, endIndex = delta + markedChar)
                    markedChar = 0
                    dst = delta
                } else {/* Reallocate buffer to accommodate read-ahead limit */
                    val ncb = CharArray(readAheadLimit)
                    cb!!.copyInto(destination = ncb, destinationOffset = 0, startIndex = markedChar, endIndex = delta + markedChar)
                    cb = ncb
                    markedChar = 0
                    dst = delta
                }
                nChars = delta
                nextChar = nChars
            }
        }

        var n: Int
        do {
            n = reader!!.read(cb!!, dst, cb!!.size - dst)
        } while (n == 0)
        if (n > 0) {
            nChars = dst + n
            nextChar = dst
        }
    }

    /**
     * Reads a single character.
     *
     * @return The character read, as an integer in the range
     * 0 to 65535 (`0x00-0xffff`), or -1 if the
     * end of the stream has been reached
     * @throws     IOException  If an I/O error occurs
     */
    override fun read(): Int {
        return implRead()
    }

    private fun implRead(): Int {
        ensureOpen()
        while (true) {
            if (nextChar >= nChars) {
                fill()
                if (nextChar >= nChars) return -1
            }
            if (skipLF) {
                skipLF = false
                if (cb!![nextChar] == '\n') {
                    nextChar++
                    continue
                }
            }
            return cb!![nextChar++].code
        }
    }


    /**
     * Reads characters into a portion of an array, reading from the underlying
     * stream if necessary.
     */
    private fun read1(cbuf: CharArray, off: Int, len: Int): Int {
        if (nextChar >= nChars) {/* If the requested length is at least as large as the buffer, and
               if there is no mark/reset activity, and if line feeds are not
               being skipped, do not bother to copy the characters into the
               local buffer.  In this way buffered streams will cascade
               harmlessly. */
            if (len >= cb!!.size && markedChar <= UNMARKED && !skipLF) {
                return reader!!.read(cbuf, off, len)
            }
            fill()
        }
        if (nextChar >= nChars) return -1
        if (skipLF) {
            skipLF = false
            if (cb!![nextChar] == '\n') {
                nextChar++
                if (nextChar >= nChars) fill()
                if (nextChar >= nChars) return -1
            }
        }
        val n = min(len.toDouble(), (nChars - nextChar).toDouble()).toInt()
        cb!!.copyInto(destination = cbuf, destinationOffset = off, startIndex = nextChar, endIndex = n + nextChar)
        nextChar += n
        return n
    }


    /**
     * Reads characters into a portion of an array.
     *
     * <p> This method implements the general contract of the corresponding
     * {@link Reader#read(char[], int, int) read} method of the
     * {@link Reader} class.  As an additional convenience, it
     * attempts to read as many characters as possible by repeatedly invoking
     * the {@code read} method of the underlying stream.  This iterated
     * {@code read} continues until one of the following conditions becomes
     * true:
     * <ul>
     *
     *   <li> The specified number of characters have been read,
     *
     *   <li> The {@code read} method of the underlying stream returns
     *   {@code -1}, indicating end-of-file, or
     *
     *   <li> The {@code ready} method of the underlying stream
     *   returns {@code false}, indicating that further input requests
     *   would block.
     *
     * </ul>
     * If the first {@code read} on the underlying stream returns
     * {@code -1} to indicate end-of-file then this method returns
     * {@code -1}.  Otherwise this method returns the number of characters
     * actually read.
     *
     * <p> Subclasses of this class are encouraged, but not required, to
     * attempt to read as many characters as possible in the same fashion.
     *
     * <p> Ordinarily this method takes characters from this stream's character
     * buffer, filling it from the underlying stream as necessary.  If,
     * however, the buffer is empty, the mark is not valid, and the requested
     * length is at least as large as the buffer, then this method will read
     * characters directly from the underlying stream into the given array.
     * Thus redundant {@code BufferedReader}s will not copy data
     * unnecessarily.
     *
     * @param      cbuf  {@inheritDoc}
     * @param      offset   {@inheritDoc}
     * @param      length   {@inheritDoc}
     *
     * @return     {@inheritDoc}
     *
     * @throws     IndexOutOfBoundsException {@inheritDoc}
     * @throws     IOException  {@inheritDoc}
     */
    override fun read(cbuf: CharArray, offset: Int, length: Int): Int {
        return implRead(cbuf, offset, length)
    }

    private fun implRead(cbuf: CharArray, off: Int, len: Int): Int {
//        println("lockedRead: offset: $off, length: $len, cbuf: ${cbuf.slice(0..10)}, size: ${cbuf.size}")
        ensureOpen()
        ObjHelper.checkFromIndexSize(off, len, cbuf.size)
        if (len == 0) {
            return 0
        }

        var n = read1(cbuf, off, len)
        if (n <= 0) return n
        while ((n < len) && reader!!.ready()) {
            val n1 = read1(cbuf, off + n, len - n)
            if (n1 <= 0) break
            n += n1
        }
        return n
    }

    /**
     * Reads a line of text.  A line is considered to be terminated by any one
     * of a line feed ('\n'), a carriage return ('\r'), a carriage return
     * followed immediately by a line feed, or by reaching the end-of-file
     * (EOF).
     *
     * @param      ignoreLF  If true, the next '\n' will be skipped
     * @param      term      Output: Whether a line terminator was encountered
     * while reading the line; may be `null`.
     *
     * @return     A String containing the contents of the line, not including
     * any line-termination characters, or null if the end of the
     * stream has been reached without reading any characters
     *
     * @see LineNumberReader.readLine
     * @throws     IOException  If an I/O error occurs
     */
    private fun readLine(ignoreLF: Boolean, term: BooleanArray?): String? {
        return implReadLine(ignoreLF, term)
    }

    private fun implReadLine(ignoreLF: Boolean, term: BooleanArray?): String? {
        var s: StringBuilder? = null
        var startChar: Int

        ensureOpen()
        var omitLF = ignoreLF || skipLF
        if (term != null) term[0] = false

        bufferLoop@ while (true) {
            if (nextChar >= nChars) fill()
            if (nextChar >= nChars) { /* EOF */
                return if (s != null && s.isNotEmpty()) s.toString()
                else null
            }
            var eol = false
            var c = 0.toChar()

            /* Skip a leftover '\n', if necessary */
            if (omitLF && (cb!![nextChar] == '\n')) nextChar++
            skipLF = false
            omitLF = false

            var i = nextChar
            charLoop@ while (i < nChars) {
                c = cb!![i]
                if ((c == '\n') || (c == '\r')) {
                    if (term != null) term[0] = true
                    eol = true
                    break@charLoop
                }
                i++
            }
            startChar = nextChar
            nextChar = i

            if (eol) {
                val str: String
                if (s == null) {
                    str = cb!!.concatToString(startChar, startChar + (i - startChar))
                } else {
                    s.appendRange(cb!!, startChar, startChar + (i - startChar))
                    str = s.toString()
                }
                nextChar++
                if (c == '\r') {
                    skipLF = true
                }
                return str
            }

            if (s == null) s = StringBuilder(DEFAULT_EXPECTED_LINE_LENGTH)
            s.appendRange(cb!!, startChar, startChar + (i - startChar))
        }
    }

    /**
     * Reads a line of text.  A line is considered to be terminated by any one
     * of a line feed ('\n'), a carriage return ('\r'), a carriage return
     * followed immediately by a line feed, or by reaching the end-of-file
     * (EOF).
     *
     * @return     A String containing the contents of the line, not including
     * any line-termination characters, or null if the end of the
     * stream has been reached without reading any characters
     *
     * @throws     IOException  If an I/O error occurs
     *
     * @see Files.readAllLines
     */
    fun readLine(): String? {
        return readLine(false, null)
    }

    /**
     * {@inheritDoc}
     */
    override fun skip(n: Long): Long {
        require(n >= 0L) { "skip value is negative" }
        return implSkip(n)
    }

    private fun implSkip(n: Long): Long {
        ensureOpen()
        var r = n
        while (r > 0) {
            if (nextChar >= nChars) fill()
            if (nextChar >= nChars)  /* EOF */ break
            if (skipLF) {
                skipLF = false
                if (cb!![nextChar] == '\n') {
                    nextChar++
                }
            }
            val d = (nChars - nextChar).toLong()
            if (r <= d) {
                nextChar += r.toInt()
                r = 0
                break
            } else {
                r -= d
                nextChar = nChars
            }
        }
        return n - r
    }


    /**
     * Tells whether this stream is ready to be read.  A buffered character
     * stream is ready if the buffer is not empty, or if the underlying
     * character stream is ready.
     *
     * @throws     IOException  If an I/O error occurs
     */
    override fun ready(): Boolean {
        return implReady();
    }

    private fun implReady(): Boolean {
        ensureOpen()

        /*
         * If newline needs to be skipped and the next char to be read
         * is a newline character, then just skip it right away.
         */
        if (skipLF) {/* Note that in.ready() will return true if and only if the next
             * read on the stream will not block.
             */
            if (nextChar >= nChars && reader!!.ready()) {
                fill()
            }
            if (nextChar < nChars) {
                if (cb!![nextChar] == '\n') nextChar++
                skipLF = false
            }
        }
        return (nextChar < nChars) || reader!!.ready()
    }

    /**
     * Tells whether this stream supports the mark() operation, which it does.
     */
    override fun markSupported(): Boolean {
        return true
    }

    /**
     * Marks the present position in the stream.  Subsequent calls to reset()
     * will attempt to reposition the stream to this point.
     *
     * @param readAheadLimit   Limit on the number of characters that may be
     * read while still preserving the mark. An attempt
     * to reset the stream after reading characters
     * up to this limit or beyond may fail.
     * A limit value larger than the size of the input
     * buffer will cause a new buffer to be allocated
     * whose size is no smaller than limit.
     * Therefore large values should be used with care.
     *
     * @throws     IllegalArgumentException  If `readAheadLimit < 0`
     * @throws     IOException  If an I/O error occurs
     */
    override fun mark(readAheadLimit: Int) {
        require(readAheadLimit >= 0) { "Read-ahead limit < 0" }
        implMark(readAheadLimit)
    }

    private fun implMark(readAheadLimit: Int) {
        ensureOpen()
        this.readAheadLimit = readAheadLimit
        markedChar = nextChar
        markedSkipLF = skipLF
    }

    /**
     * Resets the stream to the most recent mark.
     *
     * @throws     IOException  If the stream has never been marked,
     * or if the mark has been invalidated
     */
    override fun reset() {
        implReset()
    }

    private fun implReset() {
        ensureOpen()
        if (markedChar < 0) throw IOException(
            if ((markedChar == INVALIDATED)) "Mark invalid"
            else "Stream not marked"
        )
        nextChar = markedChar
        skipLF = markedSkipLF
    }

    override fun close() {
        implClose()
    }

    private fun implClose() {
        try {
            reader?.close()
        } finally {
            reader = null
            cb = null
        }
    }
}