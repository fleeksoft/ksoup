package com.fleeksoft.ksoup.ported

// alt to java.lang.String(char value[], int offset, int count)
fun String.Companion.buildString(charArray: CharArray, offset: Int, length: Int): String {
    return charArray.concatToString(startIndex = offset, endIndex = offset + length)
}
