package com.fleeksoft.ksoup.integration

import com.fleeksoft.ksoup.Ksoup.parse
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parameterizedTest
import com.fleeksoft.ksoup.parser.Parser
import com.fleeksoft.ksoup.ported.BufferReader
import java.io.File
import java.net.URISyntaxException
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Tests fixes for issues raised by the [OSS Fuzz project](https://oss-fuzz.com/testcases?project=jsoup). As
 * some of these are timeout tests - run each file 100 times and ensure under time.
 */
class FuzzFixesIT {

    @Test
    fun testHtmlParse() {
        parameterizedTest(testFiles()) { file ->
            val startTime = System.currentTimeMillis()
            val completeBy = startTime + timeout * 1000L
            for (i in 0 until numIters) {
                val doc: Document =
                    parse(BufferReader(file.readBytes()), "UTF-8", "https://example.com/")
                assertNotNull(doc)
                if (System.currentTimeMillis() > completeBy) fail(
                    "Timeout: only completed $i iters of [${file.name}] in $timeout seconds"
                )
            }
        }
    }

    @Test
    fun testXmlParse() {
        parameterizedTest(testFiles()) { file ->
            val startTime = System.currentTimeMillis()
            val completeBy = startTime + timeout * 1000L
            for (i in 0 until numIters) {
                val doc = parse(
                    BufferReader(file.readBytes()),
                    "UTF-8",
                    "https://example.com/",
                    Parser.xmlParser()
                )
                assertNotNull(doc)
                if (System.currentTimeMillis() > completeBy) fail(
                    "Timeout: only completed $i iters of [${file.name}] in $timeout seconds"
                )
            }
        }
    }

    companion object {
        var numIters = 50
        var timeout = 30 // external fuzzer is set to 60 for 100 runs
        var testDir = getJvmFile("/fuzztests/")

        @JvmStatic
        private fun testFiles(): List<File> {
            val files = testDir.listFiles()!!.toList()
            assertTrue(files.size > 10)
            return files
        }

        @JvmStatic
        fun getJvmFile(resourceName: String): File {
            return try {
                val resource = ParseTest::class.java.getResource(resourceName)
                File(resource!!.toURI())
            } catch (e: URISyntaxException) {
                throw IllegalStateException(e)
            }
        }
    }
}
