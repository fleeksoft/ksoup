package com.fleeksoft.ksoup.io

import com.fleeksoft.charset.Charset
import com.fleeksoft.charset.Charsets
import com.fleeksoft.charset.toByteArray
import com.fleeksoft.io.Reader
import com.fleeksoft.io.buffered
import com.fleeksoft.io.reader
import com.fleeksoft.ksoup.internal.SharedConstants

@Deprecated(
    message = "String.openSourceReader is deprecated, use com.fleeksoft.io.String.byteInputStream instead.",
    replaceWith = ReplaceWith("this.byteInputStream()", "com.fleeksoft.io.byteInputStream"),
    level = DeprecationLevel.WARNING // Or DeprecationLevel.ERROR if you want it to cause a compilation error
)
fun String.openSourceReader(charset: Charset? = null): SourceReader =
    SourceReader.from(charset?.let { this.toByteArray(it) } ?: this.encodeToByteArray())

@Deprecated(
    message = "ByteArray.openSourceReader is deprecated, use ByteArray.inputStream() instead.",
    replaceWith = ReplaceWith("this.inputStream()", "com.fleeksoft.io.inputStream"),
    level = DeprecationLevel.WARNING
)
fun ByteArray.openSourceReader(): SourceReader = SourceReader.from(this)

@Deprecated(
    message = "SourceReader.toReader is deprecated, use com.fleeksoft.io.InputStream instead.",
    level = DeprecationLevel.WARNING
)
fun SourceReader.toReader(charset: Charset = Charsets.UTF8, chunkSize: Int = SharedConstants.DefaultBufferSize): Reader =
    SourceInputStream(this).reader(charset).buffered(chunkSize)


internal fun String.isCharsetSupported(): Boolean {
    return Charsets.isSupported(this)
}