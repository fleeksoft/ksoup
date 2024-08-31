package com.fleeksoft.ksoup.io

import korlibs.io.file.VfsFile
import korlibs.io.stream.*


fun SourceReader.Companion.from(byteArray: ByteArray): SourceReader = SourceReaderImpl(byteArray)
fun SourceReader.Companion.from(syncStream: SyncStream): SourceReader = SourceReaderImpl(syncStream)
suspend fun SourceReader.Companion.from(asyncInputStream: AsyncInputStream): SourceReader =
    SourceReaderImpl(asyncInputStream.toAsyncStream().toSyncOrNull() ?: asyncInputStream.readAll().openSync())


fun FileSource.Companion.from(file: VfsFile): FileSource  = FileSourceImpl(file)
fun FileSource.Companion.from(filePath: String): FileSource  = FileSourceImpl(filePath)