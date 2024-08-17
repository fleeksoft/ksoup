package com.fleeksoft.ksoup.kotlinx.ported.io

//https://github.com/openjdk/jdk/blob/jdk23/src/java.base/share/classes/java/lang/Readable.java
interface Readable {
    /**
     * Attempts to read characters into the specified character buffer.
     * The buffer is used as a repository of characters as-is: the only
     * changes made are the results of a put operation. No flipping or
     * rewinding of the buffer is performed. If the [ ][java.nio.CharBuffer.length] of the specified character
     * buffer is zero, then no characters will be read and zero will be
     * returned.
     *
     * @param cb the buffer to read characters into
     * @return The number of `char` values added to the buffer,
     * possibly zero, or -1 if this source of characters is at its end
     * @throws IOException if an I/O error occurs
     * @throws NullPointerException if cb is null
     * @throws ReadOnlyBufferException if cb is a read only buffer,
     * even if its length is zero
     */
    fun read(cb: CharBuffer): Int
}