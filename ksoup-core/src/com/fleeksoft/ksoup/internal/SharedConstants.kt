package com.fleeksoft.ksoup.internal

import kotlin.random.Random
import kotlin.random.nextUInt

/**
 * ksoup constants used between packages. Do not use as they may change without warning. Users will not be able to see
 * this package when modules are enabled.
 */
public object SharedConstants {
    public const val UserDataKey: String = "/ksoup.userdata"
    public const val AttrRangeKey: String = "ksoup.attrs"
    public const val RangeKey: String = "ksoup.start"
    public const val EndRangeKey: String = "ksoup.end"

    public const val DefaultBufferSize: Int = 1024 * 32
    const val DEFAULT_CHAR_BUFFER_SIZE: Int = 8192
    var DEFAULT_BYTE_BUFFER_SIZE: Int = 8192

    public val FormSubmitTags: Array<String> = arrayOf("input", "keygen", "object", "select", "textarea")
}
