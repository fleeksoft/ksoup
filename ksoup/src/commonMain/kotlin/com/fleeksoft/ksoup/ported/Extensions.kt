package com.fleeksoft.ksoup.ported

import de.cketti.codepoints.appendCodePoint
import io.ktor.http.*
import io.ktor.utils.io.charsets.*
import okio.Buffer

internal fun String.isCharsetSupported(): Boolean {
    val result = runCatching { Charset.forName(this) }.getOrNull()
    return result != null
}

internal fun URLBuilder.appendRelativePath(relativePath: String): URLBuilder {
    val segments = this.encodedPathSegments.toMutableList()

    val isLastSlash = segments.isNotEmpty() && segments.last() == ""

    //    clear / its already joining with /
    segments.removeAll { it.isEmpty() }

    val relativePathParts: MutableList<String> =
        if (relativePath.contains("?")) {
            handleQueryParams(relativePath, "?")
        } else if (relativePath.contains("#")) {
            handleQueryParams(relativePath, "#")
        } else {
            relativePath.split("/").toMutableList()
        }

    if (relativePathParts.size > 1 && relativePathParts.last() == "/") {
        relativePathParts.removeLast()
    }

    if (relativePathParts.isNotEmpty() && segments.isNotEmpty() && !isLastSlash &&
        relativePathParts.first().startsWith("?")
    ) {
        segments.add("${segments.removeLast()}${relativePathParts.removeFirst()}")
    }

//    in files when file://etc/var/message + /var/message = file://var/message
//    etc considered as host

    if (this.protocol == URLProtocol.createOrDefault("file")) {
        if (relativePathParts.size > 1 && relativePathParts.firstOrNull() == "") {
            segments.clear()
            // remove first / space
            relativePathParts.removeFirst()
            this.host = relativePathParts.removeFirst()
        }
    }

    var isNewPathAdded = false
    relativePathParts.forEachIndexed { index, path ->
        when (path) {
            "" -> {
                if (index == 0) {
                    segments.clear()
                } else {
                    segments.add("")
                }
            }

            "." -> {
//                if its last part and . then append / example: .com/b/c/d + ./g/. = .com/b/c/d/g/
                if (index == relativePathParts.size - 1 && segments[index] != "") {
                    segments.add("")
                } else if (!isLastSlash && !isNewPathAdded) {
//                    isNewPathAdded use to avoid /b/c/d + g/./h     here . will not remove last path because its already added new
                    segments.removeLastOrNull()
                }
            }

            ".." -> {
                // Clean up last path if exist
                if (index == 0 && !isLastSlash) {
                    segments.removeLastOrNull()
                }
                if (segments.isNotEmpty()) {
                    segments.removeLast()
                }
            }

            else -> {
//                remove last trailing path if not query or fragment  g.com/a/b to g.com/a
                if (index == 0 && segments.isNotEmpty() &&
                    !isLastSlash && !path.startsWith("?") && !path.startsWith("#")
                ) {
                    segments.removeLast()
                }
                isNewPathAdded = true
                segments.add(path)
            }
        }
    }
    this.encodedPathSegments = segments

    return this
}

private fun handleQueryParams(
    relativePath: String,
    separator: String,
): MutableList<String> {
    val querySplit = relativePath.split(separator).toMutableList()
    val firstQueryPath = querySplit.removeFirst()
    val relativePathParts = firstQueryPath.split("/").toMutableList()
    if (querySplit.isNotEmpty()) {
        relativePathParts.add(
            "${relativePathParts.removeLastOrNull() ?: ""}$separator${querySplit.joinToString(separator)}",
        )
    }
    return relativePathParts
}

// TODO: handle it better
internal fun Charset.canEncode(): Boolean = runCatching { this.newEncoder() }.getOrNull() != null

internal fun CharsetEncoder.canEncode(c: Char): Boolean {
    // TODO: check this
    return kotlin.runCatching { this.encode("$c") }.isSuccess
}

internal fun CharsetEncoder.canEncode(s: String): Boolean {
    // TODO: check this
    return kotlin.runCatching { this.encode(s) }.isSuccess
}

internal fun String.isValidResourceUrl() =
    this.startsWith("http", ignoreCase = true) || this.startsWith("ftp://", ignoreCase = true) ||
        this.startsWith("ftps://", ignoreCase = true) ||
        this.startsWith("file:/", ignoreCase = true) ||
        this.startsWith("//")

internal fun String.isAbsResource(): Boolean = Regex("\\w+:").containsMatchIn(this)

internal fun IntArray.codePointsToString(): String {
    return if (this.isNotEmpty()) {
        buildString {
            this@codePointsToString.forEach {
                appendCodePoint(it)
            }
        }
    } else {
        ""
    }
}

internal fun String.toBuffer(): Buffer {
    return Buffer().apply { writeUtf8(this@toBuffer) }
}
