package com.fleeksoft.ksoup.select

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ElementsEqualityTest {

    @Test
    fun testElementsEquality() {
        // Create two Elements collections with the same content
        val html = "<div><p>First</p><p>Second</p></div>"
        val doc = Ksoup.parse(html)
        
        val els1 = doc.select("p")
        val els2 = doc.select("p")
        
        // These should be equal since they contain the same elements
        assertEquals(els1, els2, "Elements with same content should be equal")
        
        // Test with different content
        val els3 = doc.select("div")
        assertNotEquals(els1, els3, "Elements with different content should not be equal")
    }
    
    @Test
    fun testNodesEquality() {
        // Create two Nodes collections with the same content
        val html = "<div><p>First</p><p>Second</p></div>"
        val doc = Ksoup.parse(html)
        
        val nodes1 = Nodes<Element>(doc.select("p"))
        val nodes2 = Nodes<Element>(doc.select("p"))
        
        // These should be equal since they contain the same elements
        assertEquals(nodes1, nodes2, "Nodes with same content should be equal")
        
        // Test with different content
        val nodes3 = Nodes<Element>(doc.select("div"))
        assertNotEquals(nodes1, nodes3, "Nodes with different content should not be equal")
    }
}