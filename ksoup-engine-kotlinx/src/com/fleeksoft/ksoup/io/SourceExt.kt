@file:OptIn(InternalAPI::class)

package com.fleeksoft.ksoup.io

import io.ktor.utils.io.*
import kotlinx.io.Source
import kotlinx.io.files.Path


@Deprecated(
    message = "SourceReader.Companion.from(source) is deprecated, use Source.asInputStream() instead.",
    level = DeprecationLevel.WARNING
)
fun SourceReader.Companion.from(source: Source): SourceReader = SourceReaderImpl(source)
@Deprecated(
    message = "SourceReader.Companion.from(ByteReadChannel) is deprecated, use ByteReadChannel.readBuffer.asInputStream() instead.",
    level = DeprecationLevel.WARNING
)
fun SourceReader.Companion.from(bodyChannel: ByteReadChannel): SourceReader = SourceReaderImpl(bodyChannel.readBuffer)


fun FileSource.Companion.from(file: Path): FileSource  = FileSourceImpl(file)
fun FileSource.Companion.from(filePath: String): FileSource  = FileSourceImpl(filePath)