package com.fleeksoft.ksoup.safety

import com.fleeksoft.ksoup.helper.ValidationException
import com.fleeksoft.ksoup.nodes.Attribute
import com.fleeksoft.ksoup.nodes.Attributes
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.parser.Tag
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SafelistTest {
    @Test
    fun testCopyConstructor_noSideEffectOnTags() {
        val safelist1 = Safelist.none().addTags(TEST_TAG)
        val safelist2 = Safelist(safelist1)
        safelist1.addTags("invalidTag")
        assertFalse(safelist2.isSafeTag("invalidTag"))
    }

    @Test
    fun testCopyConstructor_noSideEffectOnEnforcedAttributes() {
        val safelist1 = Safelist.none().addEnforcedAttribute(TEST_TAG, TEST_ATTRIBUTE, TEST_VALUE)
        val safelist2 = Safelist(safelist1)
        safelist1.addEnforcedAttribute(TEST_TAG, TEST_ATTRIBUTE, "invalidValue")
        for ((_, value) in safelist2.getEnforcedAttributes(TEST_TAG)) {
            assertNotEquals("invalidValue", value)
        }
    }

    @Test
    fun testCopyConstructor_noSideEffectOnProtocols() {
        val invalidScheme = "invalid-scheme"
        val safelist1 = Safelist.none()
            .addAttributes(TEST_TAG, TEST_ATTRIBUTE)
            .addProtocols(TEST_TAG, TEST_ATTRIBUTE, TEST_SCHEME)
        val safelist2 = Safelist(safelist1)
        safelist1.addProtocols(TEST_TAG, TEST_ATTRIBUTE, invalidScheme)
        val attributes = Attributes()
        val invalidAttribute = Attribute(TEST_ATTRIBUTE, "$invalidScheme://someValue")
        attributes.put(invalidAttribute)
        val invalidElement = Element(Tag.valueOf(TEST_TAG), "", attributes)
        assertFalse(safelist2.isSafeAttribute(TEST_TAG, invalidElement, invalidAttribute))
    }

    @Test
    fun noscriptIsBlocked() {
        var threw = false
        var safelist: Safelist? = null
        try {
            safelist = Safelist.none().addTags("NOSCRIPT")
        } catch (validationException: ValidationException) {
            threw = true
            assertTrue(validationException.message!!.contains("unsupported"))
        }
        assertTrue(threw)
        assertNull(safelist)
    }

    companion object {
        private const val TEST_TAG = "testTag"
        private const val TEST_ATTRIBUTE = "testAttribute"
        private const val TEST_SCHEME = "valid-scheme"
        private const val TEST_VALUE = "$TEST_SCHEME://testValue"
    }
}
