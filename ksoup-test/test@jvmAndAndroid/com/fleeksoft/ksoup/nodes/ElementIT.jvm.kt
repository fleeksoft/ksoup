package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.Ksoup
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals

class ElementITJvm {

    // TODO: KMP test
    @Test
    fun concurrentChildren() {
        val childCount = 200

        val doc: Document = Ksoup.parse("<div></div>")
        val div = doc.expectFirst("div")
        for (i in 0..<childCount) {
            div.appendElement("p").after("Some text")
        }

        val threadCount = 100
        val iterCount = 10000
        val startLatch = CountDownLatch(1)
        val endLatch = CountDownLatch(threadCount)
        val failure: AtomicReference<Throwable?> = AtomicReference()

        for (i in 0..<threadCount) {
            val t = Thread(Runnable {
                try {
                    startLatch.await()
                    for (j in 0..<iterCount) {
                        val children = div.children()
                        assertEquals(childCount, children.size)
                    }
                } catch (e: Throwable) {
                    System.err.println(e)
                    failure.set(e)
                } finally {
                    endLatch.countDown()
                }
            })
            t.start()
        }

        startLatch.countDown()
        endLatch.await()
        if (failure.get() != null) {
            throw java.lang.AssertionError(failure.get())
        }
    }
}