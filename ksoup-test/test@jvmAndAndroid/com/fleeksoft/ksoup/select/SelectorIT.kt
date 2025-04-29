package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Parser
import com.fleeksoft.ksoup.parser.StreamParser
import kotlin.test.Test
import kotlin.test.assertEquals

class SelectorIT {

    @Test
    fun multiThreadHas() {
        val html = "<div id=1></div><div id=2><p>One</p><p>Two</p>"
        val eval = QueryParser.parse("div:has(p)")

        val numThreads = 20
        val numThreadLoops = 5

        val catcher = ThreadCatcher()

        val threads: Array<Thread?> = arrayOfNulls(numThreads)
        for (threadNum in 0 until numThreads) {
            val thread = Thread {
                val doc: Document = Ksoup.parse(html)
                for (loop in 0 until numThreadLoops) {
                    val els: Elements = doc.select(eval)
                    assertEquals(1, els.size)
                    assertEquals("2", els[0].id())
                }
            }
            thread.setName("Runner-$threadNum")
            thread.start()
            thread.setUncaughtExceptionHandler(catcher)
            threads[threadNum] = thread
        }

        // now join them all
        for (thread in threads) {
            thread?.join()
        }

        assertEquals(0, catcher.exceptionCount.get())
    }

    internal class ThreadCatcher : Thread.UncaughtExceptionHandler {
        var exceptionCount: java.util.concurrent.atomic.AtomicInteger = java.util.concurrent.atomic.AtomicInteger()

        override fun uncaughtException(t: Thread, e: Throwable) {
            e.printStackTrace()
            exceptionCount.incrementAndGet()
        }
    }

    @Test
    fun streamParserSelect() {
        // https://github.com/jhy/jsoup/issues/2277
        // The memo in the StructuralEvaluator was not getting reset correctly, and so would run out of memory
        // Test tracks memory consumption. Will be interesting to see how it behaves on the CI workers.

        val xml = "<A><B><C>1"
        val query = QueryParser.parse("A B C")
        val runtime = Runtime.getRuntime()

        System.gc()
        Thread.sleep(100)
        val initialUsed = runtime.totalMemory() - runtime.freeMemory()

        for (i in 0..50000 - 1) { // Before fix, would exceed 10MB in ~ 9000 iters
            StreamParser(Parser.xmlParser()).use { parser ->
                parser.parse(xml, "")
                parser.selectFirst(query)
                parser.stop()
            }
            if (i % 1000 == 0) {
                System.gc()
                Thread.sleep(100)
                val currentUsed = runtime.totalMemory() - runtime.freeMemory()
                val delta = currentUsed - initialUsed

                // Fail if we grow + 10MB
                if (delta > 10000000) {
                    kotlin.test.fail(
                        String.format(
                            "Memo leak detected. Memory increased by %,d bytes after %,d iterations",
                            delta, i
                        )
                    )
                }
            }
        }
    }
}
