package com.fleeksoft.ksoup.ported

import kotlin.jvm.JvmInline

/**
 * Represents a Unicode code point.
 *
 * You can create/retrieve instances of this class by using the following functions:
 * - [Int.toCodePoint]
 * - [Char.toCodePoint]
 */
class CodePoint internal constructor(val value: Int) {
    val charCount: Int = if (value >= Character.MIN_SUPPLEMENTARY_CODE_POINT) 2 else 1

    init {
        require(Character.isValidCodePoint(value)) { "Not a valid code point" }
    }

    /**
     * Converts this Unicode code point to its UTF-16 representation stored in a char array.
     *
     * If this code point is a BMP (Basic Multilingual Plane or Plane 0) value, the resulting char array has the same
     * value as [value]. If the specified code point is a supplementary code point, the resulting char array has the
     * corresponding surrogate pair.
     */
    fun toChars(): CharArray {
        return Character.toChars(value)
    }

    /**
     * Converts this Unicode code point to its UTF-16 representation.
     *
     * If this code point is a BMP (Basic Multilingual Plane or Plane 0) value, the same value is stored in
     * `destination[offset]`, and 1 is returned. If this code point is a supplementary character, its surrogate values
     * are stored in `destination[offset]` (high-surrogate) and `destination[offset+1]` (low-surrogate), and 2 is
     * returned.
     */
    fun toChars(destination: CharArray, offset: Int): Int {
        return Character.toChars(value, destination, offset)
    }

    /**
     * Returns the standard Unicode notation of this code point.
     *
     * "U+" followed by the code point value in hexadecimal (using upper case letters), which is prepended with leading
     * zeros to a minimum of four digits.
     */
    fun toUnicodeNotation(): String {
        return "U+${value.toString(16).uppercase().padStart(4, '0')}"
    }

    /**
     * Returns the string representation of this code point.
     *
     * The returned string consists of the sequence of characters returned by [toChars].
     */
    override fun toString(): String {
        return toChars().concatToString()
    }
}

/**
 * Returns a [CodePoint] with this value.
 *
 * Throws [IllegalArgumentException] if this value falls outside the range of valid code points.
 */
fun Int.toCodePoint(): CodePoint {
    return CodePoint(this)
}

/**
 * Returns a [CodePoint] with the same value as this `Char`.
 */
fun Char.toCodePoint(): CodePoint {
    return CodePoint(this.code)
}