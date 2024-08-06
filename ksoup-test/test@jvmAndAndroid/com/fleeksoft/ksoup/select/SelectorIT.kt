package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
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
}
