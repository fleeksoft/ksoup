package com.fleeksoft.ksoup.ported.io

import com.fleeksoft.ksoup.internal.SharedConstants
import com.fleeksoft.ksoup.io.Charset
import com.fleeksoft.ksoup.io.KByteBuffer
import com.fleeksoft.ksoup.io.SourceReader
import com.fleeksoft.ksoup.ported.exception.IOException
import kotlin.math.min

//https://github.com/openjdk/jdk/blob/jdk23/src/java.base/share/classes/sun/nio/cs/StreamDecoder.java
class StreamDecoder(source: SourceReader, charset: Charset) : Reader() {

    // -- Charset-based stream decoder impl --
    private var cs: Charset = charset

    //    private var decoder: CharsetDecoder = charset.newDecoder()
    private var bb: KByteBuffer = KByteBuffer(SharedConstants.DEFAULT_BYTE_BUFFER_SIZE)

    // Exactly one of these is non-null
    private var source: SourceReader? = source

    private var closed = false

    private fun ensureOpen() {
        if (closed) throw IOException("Stream closed")
    }

    // In order to handle surrogates properly we must never try to produce
    // fewer than two characters at a time.  If we're only asked to return one
    // character then the other is saved here to be returned later.
    //
    private var haveLeftoverChar: Boolean = false
    private var leftoverChar: Char? = null


    // -- Public methods corresponding to those in InputStreamReader --
    // All synchronization and state/argument checking is done in these public
    // methods; the concrete stream-decoder subclasses defined below need not
    // do any such checking.
    fun getEncoding(): String? {
        if (isOpen()) return encodingName()
        return null
    }

    override fun read(): Int {
        return read0()
    }

    override fun read(cbuf: CharArray, offset: Int, length: Int): Int {
        return lockedRead(cbuf, offset, length)
    }

    private fun lockedRead(cbuf: CharArray, offset: Int, length: Int): Int {
        var off = offset
        var len = length
        ensureOpen()
        if ((off < 0) || (off > cbuf.size) || (len < 0) ||
            ((off + len) > cbuf.size) || ((off + len) < 0)
        ) {
            throw IndexOutOfBoundsException()
        }
        if (len == 0) return 0

        var n = 0

        if (haveLeftoverChar) {
            // Copy the leftover char into the buffer
            cbuf[off] = leftoverChar!!
            off++
            len--
            haveLeftoverChar = false
            n = 1
            if ((len == 0) || !implReady())  // Return now if this is all we can produce w/o blocking
                return n
        }

        if (len == 1) {
            // Treat single-character array reads just like read()
            val c = read0()
            if (c == -1) return if ((n == 0)) -1 else n
            cbuf[off] = c.toChar()
            return n + 1
        }


        // Read remaining characters
        val nr: Int = implRead(cbuf, off, off + len)


        // At this point, n is either 1 if a leftover character was read,
        // or 0 if no leftover character was read. If n is 1 and nr is -1,
        // indicating EOF, then we don't return their sum as this loses data.
        return if ((nr < 0)) (if (n == 1) 1 else nr) else (n + nr)
    }

    private fun read0(): Int {
        return lockedRead0()
    }

    private fun lockedRead0(): Int {
        // Return the leftover char, if there is one
        if (haveLeftoverChar) {
            haveLeftoverChar = false
            return leftoverChar!!.code
        }

        // Convert more bytes
        val cb = CharArray(2)
        when (val n = read(cb, 0, 2)) {
            -1 -> return -1
            2 -> {
                leftoverChar = cb[1]
                haveLeftoverChar = true
                return cb[0].code
            }

            1 -> return cb[0].code
            else -> {
                require(false) { "Unable to read from source" }
                return -1
            }
        }
    }


    fun implRead(cbuf: CharArray, off: Int, end: Int): Int {
        // In order to handle surrogate pairs, this method requires that
        // the invoker attempt to read at least two characters.  Saving the
        // extra character, if any, at a higher level is easier than trying
        // to deal with it here.

        val length = end - off
        require(length > 1)

        /*var cb: CharBuffer = CharBuffer.wrap(cbuf, off, length)
        if (cb.position() != 0) {
            // Ensure that cb[0] == cbuf[off]
            cb = cb.slice()
        }*/

        if (bb.available() < length && inReady()) {
            readBytes()
        }


        if (bb.available() <= 0) return -1
        val text = bb.readText(cs, min(length, bb.size))
        for (i in text.indices) {
            cbuf[off + i] = text[i]
        }

        return text.length
    }

    fun encodingName(): String {
        return cs.name
    }

    private fun readBytes(): Int {
        bb.compact()
        // Read from the input stream, and then update the buffer
        val lim: Int = bb.size - bb.available()
        val pos = bb.position()
        val rem = (if (pos <= lim) lim - pos else 0)
        val n = this.source!!.readAtMostTo(bb, rem)
        if (n < 0) return n
        if (n == 0) throw IOException("Underlying input stream returned zero bytes")
        require(n <= rem) { "require n <= rem : n = $n, rem = $rem" }
        val remaining = bb.available()
        require(remaining != 0) { "buffer empty to read" }
        return remaining
    }

    override fun ready(): Boolean {
        ensureOpen()
        return haveLeftoverChar || implReady()
    }

    private fun inReady(): Boolean {
        return try {
            ((this.source != null) && (this.source!!.exhausted().not())) // ## RBC.available()?
        } catch (x: IOException) {
            false
        }
    }

    private fun implReady(): Boolean {
        return !bb.exhausted() || inReady()
    }

    override fun close() {
        if (closed)
            return
        try {
            implClose()
        } finally {
            closed = true
        }
    }


    private fun implClose() {
        this.source?.close()
    }

    private fun isOpen(): Boolean {
        return !closed
    }

}