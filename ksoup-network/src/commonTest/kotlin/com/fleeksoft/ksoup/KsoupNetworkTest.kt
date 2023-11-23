package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.network.parseGetRequest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class KsoupNetworkTest {

    private val rootUri = "https://aip.dfs.de/BasicVFR/"

    // issue #3 fix
    @Ignore
    @Test
    fun issue3RedirectLocationTest() {
//        val doc = Jsoup.connect(rootUri).get()
        val doc = Ksoup.parseGetRequest(rootUri)

        println("doc: ${doc.title()}")
        val location = doc.location()!!
        println("location: '$rootUri' --> '$location'")
        // The output should be "location: 'https://aip.dfs.de/BasicVFR/' --> 'https://aip.dfs.de/BasicVFR/2023NOV16/chapter/daf49d0cf52a85f8ae58cc4f2aed1580.html'"

        assertNotEquals(rootUri, location, "resolveCurrentLink-1")
        assertTrue(location.startsWith("https://aip.dfs.de/BasicVFR/20"), "resolveCurrentLink-2")
        assertTrue(location.contains("/chapter/"), "resolveCurrentLink-3")
    }

}