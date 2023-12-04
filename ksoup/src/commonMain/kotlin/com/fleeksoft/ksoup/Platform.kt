package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.select.Evaluator
import okio.BufferedSource
import okio.Path

internal expect fun readGzipFile(file: Path): BufferedSource


internal expect fun readFile(file: Path): BufferedSource



// js don't support ?i
// TODO: may be check this for js only and replace it
internal fun jsSupportedRegex(regex: String): Regex  {
    return if (regex.contains("(?i)")) {
        Regex(regex.replace("(?i)", ""), RegexOption.IGNORE_CASE)
    } else {
        Regex(regex)
    }
}