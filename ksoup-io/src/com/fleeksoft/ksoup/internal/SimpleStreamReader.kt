package com.fleeksoft.ksoup.internal

import com.fleeksoft.charset.Charset
import com.fleeksoft.charset.CharsetDecoder
import com.fleeksoft.charset.CoderResult
import com.fleeksoft.charset.CodingErrorActionValue
import com.fleeksoft.io.*
import com.fleeksoft.io.exception.IOException
import com.fleeksoft.ksoup.io.internal.SimpleBufferedInput.Companion.BufferPool


/**
 * A simple decoding InputStreamReader that recycles internal buffers.
 */
class SimpleStreamReader(private val input: InputStream, charset: Charset) : Reader() {
    private val decoder: CharsetDecoder = charset.newDecoder()
        .onMalformedInput(CodingErrorActionValue.REPLACE)
        .onUnmappableCharacter(CodingErrorActionValue.REPLACE)

    private var byteBuf: ByteBuffer? // null after close

    init {
        val buf: ByteArray = BufferPool.borrow() // shared w/ SimpleBufferedInput, ControllableInput
        byteBuf = ByteBufferFactory.wrap(buf)
        byteBuf?.flipExt() // limit(0)
    }


    public override fun read(cbuf: CharArray, off: Int, len: Int): Int {
        requireNotNull(byteBuf) // can't read after close
        var charBuf: CharBuffer = CharBufferFactory.wrap(cbuf, off, len)
        if (charBuf.position() != 0) charBuf = charBuf.sliceExt() as CharBuffer

        var readFully = false
        while (true) {
            val result: CoderResult = decoder.decode(byteBuf!!, charBuf, readFully)
            if (result.isUnderflow()) {
                if (readFully || !charBuf.hasRemaining() || (charBuf.position() > 0) && input.available() <= 0) break
                val read = bufferUp()
                if (read < 0) {
                    readFully = true
                    if ((charBuf.position() == 0) && (!byteBuf!!.hasRemaining())) break
                }
                continue
            }
            if (result.isOverflow()) break
            result.throwException()
        }

        if (readFully) decoder.reset()
        if (charBuf.position() == 0) {
            return if (readFully) -1 else 0 // 0 if there was a surrogate and the reader tried to read only 1.
        }
        return charBuf.position()
    }

    private fun bufferUp(): Int {
        checkNotNull(byteBuf) // already validated ^
        byteBuf!!.compact()
        try {
            val pos: Int = byteBuf!!.position()
            val remaining: Int = (byteBuf!!.limit() - pos)
            val read: Int = input.read(byteBuf!!.array(), byteBuf!!.arrayOffset() + pos, remaining)
            if (read < 0) return read
            if (read == 0) throw IOException("Underlying input stream returned zero bytes")
            byteBuf!!.setPositionExt(pos + read)
        } finally {
            byteBuf!!.flipExt()
        }
        return byteBuf!!.remaining()
    }

    public override fun close() {
        if (byteBuf == null) return
        BufferPool.release(byteBuf!!.array())
        byteBuf = null
        input.close()
    }
}
