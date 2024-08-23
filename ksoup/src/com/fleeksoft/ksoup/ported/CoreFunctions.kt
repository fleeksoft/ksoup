package com.fleeksoft.ksoup.ported

import com.fleeksoft.ksoup.Platform
import com.fleeksoft.ksoup.isJsOrWasm


// js don't support ?i
public fun jsSupportedRegex(regex: String): Regex {
    return if (Platform.isJsOrWasm() && regex.contains("(?i)")) {
        Regex(regex.replace("(?i)", ""), RegexOption.IGNORE_CASE)
    } else {
        Regex(regex)
    }
}

