package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.nodes.Document
import korlibs.io.file.std.uniVfs
import kotlinx.coroutines.test.runTest
import org.jsoup.Jsoup
import kotlin.system.measureTimeMillis
import kotlin.test.Ignore
import kotlin.test.Test

class PerformanceComparisonTest {

    @Test
    @Ignore
    fun compareWithJsoup() = runTest {

        if (BuildConfig.isGithubActions) {
            return@runTest
        }

        val testData = TestHelper.getResourceAbsolutePath("test.txt").uniVfs.readString()
        val testData2 = TestHelper.readGzipResource("htmltests/news-big-page.html.gz").readAllBytes().decodeToString()

        comparisonTest(ksoupTest = { parseTimes, selectTimes ->
            ksoupTest1(testData, parseTimes, selectTimes)
        }, { parseTimes, selectTimes ->
            jsoupTest1(testData, parseTimes, selectTimes)
        })

        comparisonTest(ksoupTest = { parseTimes, selectTimes ->
            ksoupTest2(testData2, parseTimes, selectTimes)
        }, { parseTimes, selectTimes ->
            jsoupTest2(testData2, parseTimes, selectTimes)
        })
    }

    private fun comparisonTest(
        ksoupTest: (parseTimes: MutableList<Long>, selectTimes: MutableList<Long>) -> Unit,
        jsoupTest: (parseTimes: MutableList<Long>, selectTimes: MutableList<Long>) -> Unit
    ) {
        val ksoupParseTimes = mutableListOf<Long>()
        val ksoupSelectTimes = mutableListOf<Long>()
        val jsoupParseTimes = mutableListOf<Long>()
        val jsoupSelectTimes = mutableListOf<Long>()

        // Perform multiple tests
        repeat(30) {
            ksoupTest(ksoupParseTimes, ksoupSelectTimes)
            jsoupTest(jsoupParseTimes, jsoupSelectTimes)
        }

        // Print comparison table
        printComparisonTable(ksoupParseTimes, ksoupSelectTimes, jsoupParseTimes, jsoupSelectTimes)
    }

    private fun ksoupTest1(data: String, parseTimes: MutableList<Long>, selectTimes: MutableList<Long>) {
        val doc: Document
        val parseTime = measureTimeMillis {
            doc = Ksoup.parse(data)
        }
        val selectTime = measureTimeMillis {
            doc.getElementsByClass("an-info").mapNotNull { anInfo ->
                anInfo.parent()?.let { a ->
                    val attr = a.attr("href")
                    if (attr.isEmpty()) return@let null

                    attr.substringAfter("/Home/Bangumi/", "")
                        .takeIf { it.isNotBlank() }
                }
            }
        }
        parseTimes.add(parseTime)
        selectTimes.add(selectTime)
    }

    private fun jsoupTest1(data: String, parseTimes: MutableList<Long>, selectTimes: MutableList<Long>) {
        val doc: org.jsoup.nodes.Document
        val parseTime = measureTimeMillis {
            doc = Jsoup.parse(data)
        }
        val selectTime = measureTimeMillis {
            doc.getElementsByClass("an-info").mapNotNull { anInfo ->
                anInfo.parent()?.let { a ->
                    val attr = a.attr("href")
                    if (attr.isEmpty()) return@let null

                    attr.substringAfter("/Home/Bangumi/", "")
                        .takeIf { it.isNotBlank() }
                }
            }
        }
        parseTimes.add(parseTime)
        selectTimes.add(selectTime)
    }


    private fun ksoupTest2(data: String, parseTimes: MutableList<Long>, selectTimes: MutableList<Long>) {
        val doc: Document
        val parseTime = measureTimeMillis {
            doc = Ksoup.parse(data)
        }
        val selectTime = measureTimeMillis {
            doc.select("p").forEach {
                val text = it.text()
            }
        }
        parseTimes.add(parseTime)
        selectTimes.add(selectTime)
    }

    private fun jsoupTest2(data: String, parseTimes: MutableList<Long>, selectTimes: MutableList<Long>) {
        val doc: org.jsoup.nodes.Document
        val parseTime = measureTimeMillis {
            doc = Jsoup.parse(data)
        }
        val selectTime = measureTimeMillis {
            doc.select("p").forEach {
                val text = it.text()
            }
        }
        parseTimes.add(parseTime)
        selectTimes.add(selectTime)
    }

    private fun printComparisonTable(
        ksoupParseTimes: List<Long>,
        ksoupSelectTimes: List<Long>,
        jsoupParseTimes: List<Long>,
        jsoupSelectTimes: List<Long>
    ) {
        // Define column widths (max 15)
        val col1Width = 5  // Test number
        val col2Width = 15 // KParse (ms)
        val col3Width = 15 // JParse (ms)
        val col4Width = 15 // KSelect (ms)
        val col5Width = 15 // JSelect (ms)
        val col6Width = 15 // Parse Diff (%)
        val col7Width = 15 // Select Diff (%)

        // Print table header
        println("\nComparison Table: Ksoup vs Jsoup")
        println(
            "%-${col1Width}s| %-${col2Width}s| %-${col3Width}s| %-${col4Width}s| %-${col5Width}s| %-${col6Width}s| %-${col7Width}s".format(
                "Test", "KParse (ms)", "JParse (ms)", "KSelect (ms)", "JSelect (ms)", "Parse Diff (%)", "Select Diff (%)"
            )
        )
        println("".padEnd(col1Width + col2Width + col3Width + col4Width + col5Width + col6Width + col7Width + 9, '-'))

        // Print test results with percentage differences
        for (i in ksoupParseTimes.indices) {
            val parseDiffPercent =
                if (jsoupParseTimes[i] == 0L) (ksoupParseTimes[i] * 100).toDouble() else (((ksoupParseTimes[i] - jsoupParseTimes[i]).toDouble() / jsoupParseTimes[i]) * 100)
            val selectDiffPercent =
                if (jsoupSelectTimes[i] == 0L) (ksoupSelectTimes[i] * 100).toDouble() else (((ksoupSelectTimes[i] - jsoupSelectTimes[i]).toDouble() / jsoupSelectTimes[i]) * 100)

            println(
                "%-${col1Width}d| %-${col2Width}d| %-${col3Width}d| %-${col4Width}d| %-${col5Width}d| %-${col6Width}.2f| %-${col7Width}.2f".format(
                    i + 1, ksoupParseTimes[i], jsoupParseTimes[i], ksoupSelectTimes[i], jsoupSelectTimes[i], parseDiffPercent, selectDiffPercent
                )
            )
        }

        // Calculate averages
        val avgKsoupParseTime = ksoupParseTimes.average()
        val avgJsoupParseTime = jsoupParseTimes.average()
        val avgKsoupSelectTime = ksoupSelectTimes.average()
        val avgJsoupSelectTime = jsoupSelectTimes.average()

        // Calculate average percentage differences
        val avgParseDiffPercent = ((avgKsoupParseTime - avgJsoupParseTime) / avgJsoupParseTime) * 100
        val avgSelectDiffPercent = ((avgKsoupSelectTime - avgJsoupSelectTime) / avgJsoupSelectTime) * 100

        // Print average results
        println("".padEnd(col1Width + col2Width + col3Width + col4Width + col5Width + col6Width + col7Width + 9, '-'))
        println(
            "%-${col1Width}s| %-${col2Width}.2f| %-${col3Width}.2f| %-${col4Width}.2f| %-${col5Width}.2f| %-${col6Width}.2f| %-${col7Width}.2f".format(
                "Avg", avgKsoupParseTime, avgJsoupParseTime, avgKsoupSelectTime, avgJsoupSelectTime, avgParseDiffPercent, avgSelectDiffPercent
            )
        )
    }


}
