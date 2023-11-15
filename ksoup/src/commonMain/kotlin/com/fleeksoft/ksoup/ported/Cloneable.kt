package com.fleeksoft.ksoup.ported

import com.fleeksoft.ksoup.nodes.Node

internal interface Cloneable<T> {
    fun clone(): T
}
