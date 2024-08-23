package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.nodes.Attributes
import kotlin.test.Test
import kotlin.test.assertEquals

class ParserSettingsTest {

    @Test
    fun caseSupport() {
        // TODO: mutlilocale test may move to jvm
//        Locale.setDefault(locale)
        val bothOn = ParseSettings(preserveTagCase = true, preserveAttributeCase = true)
        val bothOff = ParseSettings(preserveTagCase = false, preserveAttributeCase = false)
        val tagOn = ParseSettings(preserveTagCase = true, preserveAttributeCase = false)
        val attrOn = ParseSettings(preserveTagCase = false, preserveAttributeCase = true)
        assertEquals("IMG", bothOn.normalizeTag("IMG"))
        assertEquals("ID", bothOn.normalizeAttribute("ID"))
        assertEquals("img", bothOff.normalizeTag("IMG"))
        assertEquals("id", bothOff.normalizeAttribute("ID"))
        assertEquals("IMG", tagOn.normalizeTag("IMG"))
        assertEquals("id", tagOn.normalizeAttribute("ID"))
        assertEquals("img", attrOn.normalizeTag("IMG"))
        assertEquals("ID", attrOn.normalizeAttribute("ID"))
    }

    @Test
    fun attributeCaseNormalization() {
        // TODO: mutlilocale test may move to jvm
//        Locale.setDefault(locale)
        val parseSettings = ParseSettings(preserveTagCase = false, preserveAttributeCase = false)
        val normalizedAttribute = parseSettings.normalizeAttribute("HIDDEN")
        assertEquals("hidden", normalizedAttribute)
    }

    @Test
    fun attributesCaseNormalization() {
        // TODO: mutlilocale test may move to jvm
//        Locale.setDefault(locale)
        val parseSettings = ParseSettings(preserveTagCase = false, preserveAttributeCase = false)
        val attributes = Attributes()
        attributes.put("ITEM", "1")
        val normalizedAttributes = parseSettings.normalizeAttributes(attributes)
        assertEquals("item", normalizedAttributes!!.asList()[0].key)
    }
}
