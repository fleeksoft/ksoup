package com.fleeksoft.ksoup.ported

import com.fleeksoft.ksoup.nodes.Node

public class NodesIterator<T>(private val iterator: MutableIterator<T>) : MutableIterator<T> by iterator {
    private var currentEntry: T? = null

    override fun next(): T {
        currentEntry = iterator.next()
        return currentEntry!!
    }

    override fun remove() {
        iterator.remove()
        if (currentEntry is Node) {
            (currentEntry as? Node)?.remove()
        }
    }
}
