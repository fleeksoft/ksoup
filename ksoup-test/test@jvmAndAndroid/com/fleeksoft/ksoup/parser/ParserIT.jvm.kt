package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import kotlin.test.Test
import kotlin.test.assertTrue


class ParserITJvm {

    @Test
    fun parserIsThreadSafe() {
        // tests that a single parser can be called by multiple threads and won't blow up
        // without the lock, will see many exceptions in parse, and non-equal docs
        val html = "<div id=1><div id=2><div id=3>Text.</div></div></div>"
        val parser = Parser.htmlParser()
        val expectDoc: Document = parser.parseInput(html, "")

        val numThreads = 10
        val numLoops = 20
        val threads: MutableList<Thread> = ArrayList(numThreads)
        val toCheck: MutableList<Document> = ArrayList(numThreads * numLoops)
        for (i in 0..<numThreads) {
            val thread = Thread(Runnable {
                for (j in 0..<numLoops) {
                    val doc: Document = parser.parseInput(html, "")
                    toCheck.add(doc)
                }
            })
            threads.add(thread)
            thread.start()
        }

        for (thread in threads) {
            thread.join()
        }

        for (doc in toCheck) {
            assertTrue(doc.hasSameValue(expectDoc))
        }
    }

    @Test
    @Throws(InterruptedException::class)
    fun parserIsThreadSafeWithCloneAndAppend() {
        // tests that a single parser can be called by multiple threads via Element.clone().append()
        val html = "<div id=1><div id=2><div id=3></div></div></div>"
        val append = "<div id=4>Text.</div>"
        val parser = Parser.htmlParser()
        val baseDoc: Document = parser.parseInput(html, "")
        val baseElement: Element = baseDoc.expectFirst("#3")

        val numThreads = 10
        val numLoops = 20
        val threads: MutableList<Thread> = ArrayList(numThreads)
        val toCheck: MutableList<Element> = ArrayList(numThreads * numLoops)
        for (i in 0..<numThreads) {
            val thread = Thread(Runnable {
                for (j in 0..<numLoops) {
                    val cloned: Element = baseElement.clone()
                    cloned.append(append) // invokes the parser internally - parseFragment
                    toCheck.add(cloned)
                }
            })
            threads.add(thread)
            thread.start()
        }

        for (thread in threads) {
            thread.join()
        }

        baseElement.append(append)
        for (element in toCheck) {
            assertTrue(element.hasSameValue(baseElement))
        }
    }
}