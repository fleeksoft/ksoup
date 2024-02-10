@file:OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)

package com.fleeksoft.ksoup

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.darwin.ByteVar
import platform.posix.memcpy
import platform.zlib.MAX_WBITS
import platform.zlib.Z_NO_FLUSH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.inflate
import platform.zlib.inflateEnd
import platform.zlib.inflateInit2
import platform.zlib.z_stream

internal fun decompressGzip(input: ByteArray): ByteArray {
    val inputSize = input.size.toULong()

    memScoped {
        val inputPtr = allocArray<ByteVar>(inputSize.toInt())
        input.usePinned { pinned ->
            memcpy(inputPtr, pinned.addressOf(0), inputSize)
        }

        val strm = alloc<z_stream>()
        strm.next_in = inputPtr
        strm.avail_in = inputSize.toUInt()

        if (inflateInit2(strm.ptr, 16 + MAX_WBITS) != Z_OK) {
            throw RuntimeException("Failed to initialize zlib inflater")
        }

        var outputBuffer = ByteArray(1024 * 10) // Initial size
        val outputChunk = ByteArray(1024) // Adjust size as needed
        var totalOutputSize = 0

        try {
            do {
                outputChunk.usePinned { pinnedOutput ->
                    strm.next_out = pinnedOutput.addressOf(0).reinterpret()
                    strm.avail_out = outputChunk.size.toUInt()

                    val ret = inflate(strm.ptr, Z_NO_FLUSH)
                    if (ret != Z_OK && ret != Z_STREAM_END) {
                        throw RuntimeException("Failed to inflate: $ret")
                    }

                    val have = outputChunk.size - strm.avail_out.toInt()
                    if (totalOutputSize + have > outputBuffer.size) {
                        // Resize the buffer if necessary
                        outputBuffer = outputBuffer.copyOf(totalOutputSize + have)
                    }
                    for (i in 0 until have) {
                        outputBuffer[totalOutputSize + i] = outputChunk[i]
                    }
                    totalOutputSize += have
                }
            } while (strm.avail_out == 0u)
        } finally {
            inflateEnd(strm.ptr)
        }

        return outputBuffer.copyOfRange(0, totalOutputSize)
    }
}
