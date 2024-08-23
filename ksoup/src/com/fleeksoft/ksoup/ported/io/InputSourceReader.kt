package com.fleeksoft.ksoup.ported.io

import com.fleeksoft.ksoup.io.Charset
import com.fleeksoft.ksoup.io.SourceReader


class InputSourceReader(source: SourceReader, charset: Charset = Charsets.UTF8) : Reader() {

    private val sd = StreamDecoder(source, charset)

    /**
     * Returns the name of the character encoding being used by this stream.
     *
     *
     *  If the encoding has an historical name then that name is returned;
     * otherwise the encoding's canonical name is returned.
     *
     *
     *  If this instance was created with the [ ][.InputStreamReader] constructor then the returned
     * name, being unique for the encoding, may differ from the name passed to
     * the constructor. This method will return `null` if the
     * stream has been closed.
     *
     * @return The historical name of this encoding, or
     * `null` if the stream has been closed
     *
     * @see Charset
     */
    fun getEncoding(): String? {
        return sd.getEncoding()
    }

    /**
     * Reads a single character.
     *
     * @return The character read, or -1 if the end of the stream has been
     *         reached
     *
     * @throws IOException If an I/O error occurs
     */
    override fun read(): Int {
        return sd.read()
    }

    override fun read(cbuf: CharArray, offset: Int, length: Int): Int {
        return sd.read(cbuf, offset, length)
    }

    override fun ready(): Boolean {
        return sd.ready()
    }

    override fun close() {
        sd.close()
    }
}