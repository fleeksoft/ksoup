package com.fleeksoft.ksoup.io

@Deprecated(
    message = "SourceReader is deprecated, use com.fleeksoft.io.InputStream instead.",
    replaceWith = ReplaceWith("com.fleeksoft.io.InputStream"),
    level = DeprecationLevel.WARNING
)
interface SourceReader {

    fun mark(readLimit: Long)

    fun reset()

    fun readInt(): Int

    fun readBytes(count: Int): ByteArray

    fun read(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size): Int

    fun readAllBytes(): ByteArray

    fun exhausted(): Boolean

    fun close()

    companion object
}