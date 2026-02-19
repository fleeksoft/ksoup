package com.fleeksoft.ksoup.integration

import com.fleeksoft.io.byteInputStream
import com.fleeksoft.ksoup.*
import com.fleeksoft.ksoup.helper.Validate.fail
import com.fleeksoft.ksoup.parser.Parser
import kotlinx.coroutines.test.runTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertNotNull

class FuzzFixesIT {

    @Ignore // disabled, as these soak up build time and the outcome oughtn't change unless we are refactoring the tree builders. manually execute as desired.
    @Test
    fun testHtmlParse() = runTest {
        parameterizedTestSuspend(fuzzTestFiles) { fuzzFile ->
            val startTime: Long = System.currentTimeMillis()
            val completeBy = startTime + timeout * 1000L

            for (i in 0..<numIters) {
                val input = TestHelper.readResourceAsString(fuzzFile).byteInputStream()
                val doc = Ksoup.parseInput(input, charsetName = "UTF-8", baseUri = "https://example.com/")
                if (System.currentTimeMillis() > completeBy) fail("Timeout: only completed $i iters of [$fuzzFile] in $timeout seconds")
            }
        }
    }

    @Ignore // disabled, as these soak up build time and the outcome oughtn't change unless we are refactoring the tree builders. manually execute as desired.
    @Test
    fun testXmlParse() = runTest {
        parameterizedTestSuspend(fuzzTestFiles) { fuzzFile ->
            val startTime: Long = System.currentTimeMillis()
            val completeBy = startTime + timeout * 1000L
            for (i in 0..<numIters) {
                val input = TestHelper.readResourceAsString(fuzzFile).byteInputStream()
                val doc = Ksoup.parseInput(input, charsetName = "UTF-8", baseUri = "https://example.com/", parser = Parser.xmlParser())
                assertNotNull(doc)
                if (System.currentTimeMillis() > completeBy) fail("Timeout: only completed $i iters of [$fuzzFile] in $timeout seconds")
            }
        }

    }

    companion object {
        val numIters: Int = 50
        val timeout: Int = 30 // external fuzzer is set to 60 for 100 runs
        private val fuzzTestFileNames = listOf<String>(
            "1538.html.gz",
            "1539.html.gz",
            "1542.html.gz",
            "1543.html.gz",
            "1544.html.gz",
            "1569.html.gz",
            "1577.html.gz",
            "1580.html.gz",
            "1580-attrname.html.gz",
            "1593.html.gz",
            "1595.html.gz",
            "1596.html.gz",
            "1605.html.gz",
            "1606.html.gz",
            "1607.html.gz",
            "1611.html.gz",
            "1612.html.gz",
            "1613.html.gz",
            "1637.html.gz",
            "1638.html.gz",
            "1639.html.gz",
            "1640.html.gz",
            "1642.html.gz",
            "1644.html.gz",
            "1646.html.gz",
            "1695.html.gz",
            "1696.html.gz",
            "1697.html.gz",
            "2353.html.gz",
            "2374.html.gz",
            "9056.html.gz",
            "36192.html.gz",
            "39164.html.gz",
            "48116.html.gz",
            "63202.html.gz",
            "63236.html.gz",
            "63242.html.gz",
            "63259.html.gz",
            "67469.html.gz",
            "422516687.html.gz",
            "garble.html",
        )

        val fuzzTestFiles: List<String> = fuzzTestFileNames.map { "fuzztests/$it" }.toList()

    }
}