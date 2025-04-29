package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.Ksoup
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SelectorITCommon {
    @Test
    fun multiCoroutineHas() = runTest {
        val html = "<div id=1></div><div id=2><p>One</p><p>Two</p>"
        val eval = QueryParser.parse("div:has(p)")

        val numCoroutines = 20
        val numLoops = 5
        val exceptionCount = atomic(0)

        // a handler that catches any uncaught exception in a coroutine
        val handler = CoroutineExceptionHandler { _, throwable ->
            throwable.printStackTrace()
            exceptionCount.incrementAndGet()
        }

        // launch numCoroutines coroutines and wait for all to complete
        coroutineScope {
            repeat(numCoroutines) { idx ->
                launch(Dispatchers.Default + handler) {
                    // parse once per coroutine, then loop
                    val doc = Ksoup.parse(html)
                    repeat(numLoops) {
                        val els = doc.select(eval)
                        assertEquals(1, els.size, "loop#$it in coroutine#$idx got wrong element count")
                        assertEquals("2", els[0].id(), "loop#$it in coroutine#$idx got wrong id")
                    }
                }
            }
        }

        // after all coroutines finish, ensure no exceptions were caught
        assertEquals(0, exceptionCount.value, "There were coroutine exceptions")
    }
}