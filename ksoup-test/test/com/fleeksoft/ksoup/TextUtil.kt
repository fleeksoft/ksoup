package com.fleeksoft.ksoup


object TextUtil {
    var stripper = Regex("\\r?\\n\\s*")
    var stripLines = Regex("\\r?\\n?")
    var spaceCollapse = Regex("\\s{2,}")
    var tagSpaceCollapse = Regex(">\\s+<")
    var stripCRs = Regex("\\r*")

    fun stripNewlines(text: String): String {
        return stripper.replace(text, "")
    }

    fun normalizeSpaces(text: String): String {
        var text = text
        text = stripLines.replace(text, "")
        text = stripper.replace(text, "")
        text = spaceCollapse.replace(text, " ")
        text = tagSpaceCollapse.replace(text, "><")
        return text
    }

    fun stripCRs(text: String): String {
        return stripCRs.replace(text, "")
    }
}
