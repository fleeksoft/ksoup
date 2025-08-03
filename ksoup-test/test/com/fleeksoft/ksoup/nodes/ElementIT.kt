package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.*


class ElementIT {

    @Test
    fun testFastReparent() {
        if (Platform.isWasmJs() && BuildConfig.isGithubActions) {
//            failing on github action
            return
        }

        val htmlBuf = StringBuilder()
        val rows = 300000
        for (i in 1..rows) {
            htmlBuf
                .append("<p>El-")
                .append(i)
                .append("</p>")
        }
        val html = htmlBuf.toString()
        val doc = Ksoup.parse(html)
        val start = System.currentTimeMillis()
        val wrapper = Element("div")
        val childNodes = doc.body().childNodes()
        wrapper.insertChildren(0, childNodes)
        val runtime = System.currentTimeMillis() - start
        assertEquals(rows, wrapper.childNodes.size)
        assertEquals(rows, childNodes.size) // child nodes is a wrapper, so still there
        assertEquals(0, doc.body().childNodes().size) // but on a fresh look, all gone
        doc.body().empty().appendChild(wrapper)
        val wrapperAcutal = doc.body().children()[0]
        assertEquals(wrapper, wrapperAcutal)
        assertEquals("El-1", wrapperAcutal.children()[0].text())
        assertEquals("El-$rows", wrapperAcutal.children()[rows - 1].text())
        assertTrue(runtime <= 20000)
    }

    @Test
    fun testFastReparentExistingContent() {
        if (Platform.isJsOrWasm() && BuildConfig.isGithubActions) {
//            failing on github action
            return
        }

        val htmlBuf = StringBuilder()
        val rows = 300000
        for (i in 1..rows) {
            htmlBuf
                .append("<p>El-")
                .append(i)
                .append("</p>")
        }
        val html = htmlBuf.toString()
        val doc = Ksoup.parse(html)
        val start = System.currentTimeMillis()
        val wrapper = Element("div")
        wrapper.append("<p>Prior Content</p>")
        wrapper.append("<p>End Content</p>")
        assertEquals(2, wrapper.childNodes.size)
        val childNodes = doc.body().childNodes()
        wrapper.insertChildren(1, childNodes)
        val runtime = System.currentTimeMillis() - start
        assertEquals(rows + 2, wrapper.childNodes.size)
        assertEquals(rows, childNodes.size) // child nodes is a wrapper, so still there
        assertEquals(0, doc.body().childNodes().size) // but on a fresh look, all gone
        doc.body().empty().appendChild(wrapper)
        val wrapperAcutal = doc.body().children()[0]
        assertEquals(wrapper, wrapperAcutal)
        assertEquals("Prior Content", wrapperAcutal.children()[0].text())
        assertEquals("El-1", wrapperAcutal.children()[1].text())
        assertEquals("El-$rows", wrapperAcutal.children()[rows].text())
        assertEquals("End Content", wrapperAcutal.children()[rows + 1].text())
        assertTrue(runtime <= 20000)
    }

    // These overflow tests take a couple seconds to run, so are in the slow tests
    @Test
    fun hasTextNoOverflow() {
        if (Platform.isJsOrWasm()) {
            // FIXME: timeout error for js
            return
        }

        // hasText() was recursive, so could overflow
        val doc = Document("https://example.com/")
        var el = doc.body()
        for (i in 0..50000) {
            el = el.appendChild(doc.createElement("p")).firstElementChild()!!
        }
        assertFalse(doc.hasText())
        el.text("Hello")
        assertTrue(doc.hasText())
        assertEquals(el.text(), doc.text())
    }

    @Test
    fun dataNoOverflow() {
        if (Platform.isJsOrWasm()) {
            // FIXME: timeout error for js
            return
        }

        // data() was recursive, so could overflow
        val doc = Document("https://example.com/")
        var el: Element = doc.body()
        for (i in 0..50000) {
            el = el.appendChild(doc.createElement("p")).firstElementChild()!!
        }
        val script = el.appendElement("script")
        script.text("script") // holds data nodes, so inserts as data, not text
        assertFalse(script.hasText())
        assertEquals("script", script.data())
        assertEquals(el.data(), doc.data())
    }

    @Test
    fun parentsNoOverflow() {
        if (Platform.isJsOrWasm()) {
            // FIXME: timeout error for js
            return
        }

        // parents() was recursive, so could overflow
        val doc = Document("https://example.com/")
        var el = doc.body()
        val num = 50000
        for (i in 0..num) {
            el = el.appendChild(doc.createElement("p")).firstElementChild()!!
        }
        val parents = el.parents()
        assertEquals(num + 2, parents.size) // +2 for html and body
        assertEquals(doc, el.ownerDocument())
    }

    @Test
    fun wrapNoOverflow() {
        if (Platform.isWasmJs()) {
            // FIXME: timeout error for js
            return
        }
        // deepChild was recursive, so could overflow if presented with a fairly insane wrap
        val doc = Document("https://example.com/")
        val el = doc.body().appendElement("p")
        val num = 50000
        val sb = StringBuilder()
        for (i in 0..num) {
            sb.append("<div>")
        }
        el.wrap(sb.toString())
        val html = doc.body().html()
        assertTrue(html.startsWith("<div>"))
        assertEquals(num + 3, el.parents().size)
    }

    @Test
    fun testConcurrentMergeWithCoroutines() = runTest {
        // https://github.com/jhy/Ksoup/discussions/2280 / https://github.com/jhy/Ksoup/issues/2281
        // This was failing because the template.clone().append(html) was reusing the same underlying parser object.
        // Document#clone now correctly clones its parser.

        class TemplateMerger(private val templateDoc: Document) {
            fun mergeWith(inputDoc: Document): Document {
                val merged = templateDoc.clone()
                val content = merged.getElementById("content")
                content!!.append(inputDoc.html())
                return merged
            }
        }
        // Prepare template
        val templateHtml = "<html><body><div id='content'></div></body></html>"
        val templateDoc = Ksoup.parse(templateHtml)
        val merger = TemplateMerger(templateDoc)

        // Number of concurrent “threads” (coroutines) and iterations per coroutine
        val coroutineCount = 10
        val iterations = 1_000

        // SupervisorScope so one coroutine failure doesn't cancel others immediately,
        // allowing you to see all failures (if any)
        supervisorScope {
            val jobs = List(coroutineCount) {
                async {
                    try {
                        repeat(iterations) {
                            val inputHtml = "<html><body><p>Some content</p></body></html>"
                            // Parsing can be offloaded to IO or Default dispatcher
                            val inputDoc = withContext(Dispatchers.Default) {
                                Ksoup.parse(inputHtml)
                            }
                            val merged = merger.mergeWith(inputDoc)
                            assertNotNull(merged)
                        }
                    } catch (e: Exception) {
                        // Propagate as test failure
                        fail("Exception in coroutine #$it: ${e.message}")
                    }
                }
            }
            // Wait for all coroutines to complete (or fail)
            jobs.forEach { it.await() }
        }
    }
}
