package com.fleeksoft.ksoup.network

import com.fleeksoft.io.InputStream
import com.fleeksoft.ksoup.io.SourceReader
import com.fleeksoft.ksoup.io.from
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.util.*
import io.ktor.utils.io.core.*

@Deprecated(
    message = "HttpResponse.asSourceReader() is deprecated, use HttpResponse.asInputStream() instead.",
    replaceWith = ReplaceWith("this.asInputStream()"),
    level = DeprecationLevel.WARNING
)
suspend fun HttpResponse.asSourceReader() = SourceReader.from(this.bodyAsText().encodeToByteArray())

@Deprecated("Ktor2 support is deprecated; use the ktor3 variant `ksoup-network` instead.")
suspend fun HttpResponse.asInputStream() = this.body<ByteReadPacket>().asStream()


@Deprecated("Ktor2 support is deprecated; use the ktor3 variant `ksoup-network` instead.")
public fun Input.asStream(): InputStream = object : InputStream() {

    override fun read(): Int {
        if (endOfInput) return -1
        return readByte().toInt()
    }

    override fun read(bytes: ByteArray, off: Int, len: Int): Int {
        if (this@asStream.endOfInput) return -1
        return readAvailable(bytes, off, len)
    }

    override fun skip(n: Long): Long = discard(n)

    override fun close() {
        this@asStream.close()
    }
}
