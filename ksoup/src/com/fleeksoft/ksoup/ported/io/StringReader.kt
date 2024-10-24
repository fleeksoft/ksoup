package com.fleeksoft.ksoup.ported.io

import com.fleeksoft.ksoup.exception.IOException
import kotlin.math.max
import kotlin.math.min


/**
 * Creates a new string reader.
 *
 * @param s  String providing the character stream.
 */
class StringReader(s: String) : Reader() {
//        todo:// most functions need synchronize lock


    private val length: Int = s.length
    private var str: String? = s
    private var next = 0
    private var mark = 0

    /** Check to make sure that the stream has not been closed  */
    private fun ensureOpen() {
        if (str == null) throw IOException("Stream closed")
    }

    /**
     * Reads a single character.
     *
     * @return     The character read, or -1 if the end of the stream has been
     * reached
     *
     * @throws     IOException  If an I/O error occurs
     */
    override fun read(): Int {
        ensureOpen()
        if (next >= length) return -1
        return str!![next++].code
    }

    override fun read(cbuf: CharArray, offset: Int, length: Int): Int {
        ensureOpen()
        ObjHelper.checkFromIndexSize(offset, length, cbuf.size)
        if (length == 0) {
            return 0
        }
        if (next >= this.length) return -1
        val n: Int = min(this.length - next, length)
        for (i in 0 until n) {
            cbuf[offset + i] = str!![next + i]
        }
        next += n
        return n
    }


    /**
     * Skips characters. If the stream is already at its end before this method
     * is invoked, then no characters are skipped and zero is returned.
     *
     *
     * The `n` parameter may be negative, even though the
     * `skip` method of the [Reader] superclass throws
     * an exception in this case. Negative values of `n` cause the
     * stream to skip backwards. Negative return values indicate a skip
     * backwards. It is not possible to skip backwards past the beginning of
     * the string.
     *
     *
     * If the entire string has been read or skipped, then this method has
     * no effect and always returns `0`.
     *
     * @param n {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws IOException {@inheritDoc}
     */
    override fun skip(n: Long): Long {
        ensureOpen()
        if (next >= length) return 0
        // Bound skip by beginning and end of the source
        var r = min((length - next).toDouble(), n.toDouble()).toLong()
        r = max(-next.toDouble(), r.toDouble()).toLong()
        next += r.toInt()
        return r
    }


    /**
     * Tells whether this stream is ready to be read.
     *
     * @return True if the next read() is guaranteed not to block for input
     *
     * @throws     IOException  If the stream is closed
     */
    override fun ready(): Boolean {
        ensureOpen()
        return true
    }

    /**
     * Tells whether this stream supports the mark() operation, which it does.
     */
    override fun markSupported(): Boolean {
        return true
    }


    /**
     * Marks the present position in the stream.  Subsequent calls to reset()
     * will reposition the stream to this point.
     *
     * @param  readAheadLimit  Limit on the number of characters that may be
     * read while still preserving the mark.  Because
     * the stream's input comes from a string, there
     * is no actual limit, so this argument must not
     * be negative, but is otherwise ignored.
     *
     * @throws     IllegalArgumentException  If `readAheadLimit < 0`
     * @throws     IOException  If an I/O error occurs
     */
    override fun mark(readAheadLimit: Int) {
        require(readAheadLimit >= 0) { "Read-ahead limit < 0" }
        ensureOpen()
        mark = next
    }


    /**
     * Resets the stream to the most recent mark, or to the beginning of the
     * string if it has never been marked.
     *
     * @throws     IOException  If an I/O error occurs
     */
    override fun reset() {
        ensureOpen()
        next = mark
    }

    /**
     * Closes the stream and releases any system resources associated with
     * it. Once the stream has been closed, further read(),
     * ready(), mark(), or reset() invocations will throw an IOException.
     * Closing a previously closed stream has no effect. This method will block
     * while there is another thread blocking on the reader.
     */
    override fun close() {
        str = null
    }
}