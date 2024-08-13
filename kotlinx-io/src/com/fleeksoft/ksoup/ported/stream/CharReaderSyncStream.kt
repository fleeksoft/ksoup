package com.fleeksoft.ksoup.ported.stream

import com.fleeksoft.ksoup.internal.SharedConstants
import com.fleeksoft.ksoup.ported.io.BufferReader
import com.fleeksoft.ksoup.ported.io.Charset
import kotlinx.io.Buffer
import kotlin.math.min

internal class CharReaderSyncStream(
    private val bufferReader: BufferReader,
    private val charset: Charset,
    private val chunkSize: Int,
) {
    private var temp = ByteArray(chunkSize)
    private val buffer = Buffer()
    private var tempStringBuilder = StringBuilder()

    private var markedTempStringBuilder: StringBuilder? = null
    private var currentState: ReaderState = ReaderState(consumedBytes = 0, consumedBuffer = 0, lastBytesRead = 0)
    private var markedState: ReaderState? = null

    fun clone(): CharReaderSyncStream = CharReaderSyncStream(bufferReader.clone(), charset, chunkSize)

    init {
        bufferReader.mark(SharedConstants.DefaultBufferSize)
    }

    private fun bufferUp() {
        while (buffer.size < temp.size) {
            val readCount = bufferReader.read(temp)
            if (readCount <= 0) break

            currentState =
                currentState.copy(
                    consumedBytes = currentState.consumedBytes + readCount,
                    lastBytesRead = readCount,
                )

            buffer.write(temp, 0, readCount)
            if (currentState.applyMarkedState) {
                currentState = currentState.copy(applyMarkedState = false)
                if (currentState.consumedBuffer > 0) {
                    buffer.skip(currentState.consumedBuffer.toLong())
                }
            } else {
                currentState = currentState.copy(consumedBuffer = 0)
            }

//            println("stream.read: $readCount, state: $currentState")
        }
    }

    fun read(
        out: StringBuilder,
        count: Int,
    ): Int {
        bufferUp()

        while (tempStringBuilder.length < count) {
            val readCount = buffer.readAtMostTo(temp)
            val consumed = charset.decode(tempStringBuilder, temp, 0, readCount)
            if (consumed <= 0) break
            currentState = currentState.copy(consumedBuffer = currentState.consumedBuffer + consumed)
            buffer.skip(consumed.toLong())
        }

        // println("tempStringBuilder=$tempStringBuilder")

        val slice = tempStringBuilder.substring(0, min(count, tempStringBuilder.length))
        tempStringBuilder = StringBuilder(slice.length).append(tempStringBuilder.substring(slice.length))

        out.append(slice)
        return slice.length
    }

    fun mark(readLimit: Int) {
        this.markedTempStringBuilder = StringBuilder(tempStringBuilder)
        this.markedState = this.currentState

//        println()
//        println("mark: markedTempStringBuilder: ${this.markedTempStringBuilder}")
//        println("mark: streamAvailable: ${stream.availableRead}, state: ${this.markedState}")
    }

    fun reset() {
        if (this.markedState != null) {
            if (this.markedTempStringBuilder != null) {
                this.tempStringBuilder = StringBuilder(this.markedTempStringBuilder!!)
                this.markedTempStringBuilder = null
            }

            buffer.clear()
            temp = ByteArray(chunkSize)
            val skipedBytes = this.markedState!!.consumedBytes - this.markedState!!.lastBytesRead
            this.currentState = this.markedState!!.copy(consumedBytes = skipedBytes, applyMarkedState = true)

            bufferReader.reset()
            bufferReader.mark(SharedConstants.DefaultBufferSize)
            if (skipedBytes > 0) {
                bufferReader.skip(skipedBytes)
            }

            this.markedState = null
        }
    }

    fun skip(count: Int) {
        this.read(StringBuilder(), count)
    }
}

private data class ReaderState(
    val consumedBytes: Int,
    val consumedBuffer: Int,
    val lastBytesRead: Int,
    val applyMarkedState: Boolean = false,
)
