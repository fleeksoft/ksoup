package com.fleeksoft.ksoup.internal

import com.fleeksoft.charset.Charsets
import com.fleeksoft.ksoup.Platform
import com.fleeksoft.ksoup.TestHelper
import com.fleeksoft.ksoup.io.internal.ControllableInputStream
import com.fleeksoft.ksoup.io.internal.SimpleBufferedInput
import com.fleeksoft.ksoup.isJsOrWasm
import com.fleeksoft.ksoup.parser.CharacterReader
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ReaderTest {

    @Test
    fun readerOfStringAndFile() = runTest {
        // TODO: remove after release
        if (Platform.isJsOrWasm()) {
            return@runTest
        }
        // make sure that reading from a String and from a File produce the same bytes
        val resourceName = "fuzztests/garble.html"
        val fromBytes = TestHelper.readResourceAsString(resourceName)

        val streamReader: SimpleStreamReader = getReader(resourceName)
        val fromStream: String = getString(streamReader)
        assertEquals(fromBytes, fromStream)

        val reader2: SimpleStreamReader = getReader(resourceName)
        val cr = CharacterReader(reader2)
        val fullRead: String? = cr.consumeTo('X') // does not exist in input
        assertEquals(fromBytes, fullRead)
    }

    companion object {
        private fun getString(streamReader: SimpleStreamReader): String {
            // read StreamReader to a string:
            val builder = StringBuilder()
            val cbuffer = CharArray(1024)
            var read: Int
            while ((streamReader.read(cbuffer).also { read = it }) != -1) {
                builder.appendRange(cbuffer, 0, 0 + read)
            }
            return builder.toString()
        }

        private suspend fun getReader(resourceName: String): SimpleStreamReader {
            // set up a chain as in when we parse: simplebufferedinput -> controllableinputstream -> simplestreamreader -> characterreader
            val input = SimpleBufferedInput(TestHelper.resourceFilePathToStream(resourceName))
            val stream = ControllableInputStream.wrap(input, 0)
            return SimpleStreamReader(stream, Charsets.UTF8)
        }
    }
}
