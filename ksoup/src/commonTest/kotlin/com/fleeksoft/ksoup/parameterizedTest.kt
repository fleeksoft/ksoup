package com.fleeksoft.ksoup

fun <T> parameterizedTest(parameters: List<T>, testFunc: (T) -> Unit) {
    parameters.forEach {
        testFunc(it)
    }
}