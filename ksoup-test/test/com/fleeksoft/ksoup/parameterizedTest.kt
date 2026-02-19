package com.fleeksoft.ksoup

fun <T> parameterizedTest(
    parameters: List<T>,
    testFunc: (T) -> Unit,
) {
    parameters.forEach {
        testFunc(it)
    }
}

suspend fun <T> parameterizedTestSuspend(
    parameters: List<T>,
    testFunc: suspend (T) -> Unit,
) {
    parameters.forEach {
        testFunc(it)
    }
}