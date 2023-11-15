package com.fleeksoft.ksoup

fun <E> ArrayList<E>.removeRange(fromIndex: Int, toIndex: Int) {
    for (i in (toIndex - 1) downTo fromIndex) {
        this.removeAt(i)
    }
}
