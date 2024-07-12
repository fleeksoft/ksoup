package com.fleeksoft.ksoup

import korlibs.io.file.std.resourcesVfs
import korlibs.io.file.std.uniVfs
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class JsTest {

//    ./gradlew :ksoup-test:jsNodeTest --tests "com.fleeksoft.ksoup.JsTest.testFileExists"
    @Test
    fun testFileExists() = runTest {
        val t = "htmltests/medium.html"
        val file = resourcesVfs["htmltests/medium.html"]
//        assertEquals(true, file.exists())
    }
}
