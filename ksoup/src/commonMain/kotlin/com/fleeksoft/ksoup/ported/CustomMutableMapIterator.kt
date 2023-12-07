package com.fleeksoft.ksoup.ported

import com.fleeksoft.ksoup.nodes.Element

public class ElementIterator(private val iterator: MutableIterator<Element>) : MutableIterator<Element> by iterator {
    private var currentEntry: Element? = null

    override fun next(): Element {
        currentEntry = iterator.next()
        return currentEntry!!
    }

    override fun remove() {
        iterator.remove()
        currentEntry?.remove()
    }
}
