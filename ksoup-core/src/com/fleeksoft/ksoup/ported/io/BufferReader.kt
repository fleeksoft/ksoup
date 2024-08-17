package com.fleeksoft.ksoup.ported.io

interface BufferReader {
    public fun skip(count: Int)

    public fun mark(readLimit: Int)

    public fun reset()

    public fun readBytes(count: Int): ByteArray

    public fun read(): Byte

    public fun read(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size): Int

    public fun readAllBytes(): ByteArray

    public fun exhausted(): Boolean

    public fun clone(): BufferReader

    public fun close()
}