package com.fleeksoft.ksoup

fun assertContains(expected: String, actual: String?, ignoreCase: Boolean = true) {
    check(actual?.contains(expected, ignoreCase = ignoreCase) == true) {
        "expected to contains '$expected' but was '$actual'."
    }
}