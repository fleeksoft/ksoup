package com.fleeksoft.ksoup.integration

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Attribute
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.safety.Safelist
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Check that we can extend Safelist methods
 */
class SafelistExtensionTest {
    @Test
    fun canCustomizeSafeTests() {
        val openSafelist = OpenSafelist(Safelist.relaxed())
        val safelist = Safelist.relaxed()
        val html = "<p><opentag openattr>Hello</opentag></p>"
        val openClean = Ksoup.clean(html, openSafelist)
        val clean = Ksoup.clean(html, safelist)
        assertEquals("<p><opentag openattr=\"\">Hello</opentag></p>", com.fleeksoft.ksoup.TextUtil.stripNewlines(openClean))
        assertEquals("<p>Hello</p>", clean)
    }

    // passes tags and attributes starting with "open"
    private class OpenSafelist(safelist: Safelist?) : Safelist(safelist!!) {
        override fun isSafeAttribute(
            tagName: String,
            el: Element,
            attr: Attribute,
        ): Boolean {
            return if (attr.key.startsWith("open")) true else super.isSafeAttribute(tagName, el, attr)
        }

        override fun isSafeTag(tag: String): Boolean {
            return if (tag.startsWith("open")) true else super.isSafeTag(tag)
        }
    }
}
