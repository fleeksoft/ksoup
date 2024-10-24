package com.fleeksoft.ksoup.io

import com.fleeksoft.io.InputStream
import korlibs.io.file.VfsFile
import korlibs.io.stream.*


@Deprecated(
    message = "SourceReader.Companion.from(syncStream) is deprecated, use syncStream.asInputStream() instead.",
    level = DeprecationLevel.WARNING
)
fun SourceReader.Companion.from(syncStream: SyncStream): SourceReader = SourceReaderImpl(syncStream)
@Deprecated(
    message = "SourceReader.Companion.from(asyncInputStream) is deprecated, use syncStream.asInputStream() instead.",
    level = DeprecationLevel.WARNING
)
suspend fun SourceReader.Companion.from(asyncInputStream: AsyncInputStream): SourceReader =
    SourceReaderImpl(asyncInputStream.toAsyncStream().toSyncOrNull() ?: asyncInputStream.readAll().openSync())


fun FileSource.Companion.from(file: VfsFile): FileSource  = FileSourceImpl(file)
fun FileSource.Companion.from(filePath: String): FileSource  = FileSourceImpl(filePath)

fun SyncStream.asInputStream(): InputStream {
    val ss = this
    return object : InputStream() {
        override fun read(): Int = if (ss.eof) -1 else ss.readU8()
        override fun read(bytes: ByteArray, off: Int, len: Int): Int = ss.read(bytes, off, len)
        override fun available(): Int = ss.available.toInt()
        override fun close() = ss.close()
        override fun mark(readLimit: Int) = ss.mark(readLimit)
        override fun reset() = ss.reset()
        override fun markSupported(): Boolean = true
        override fun skip(n: Long): Long {
            ss.skip(n.toInt())
            return n
        }
    }
}