package com.fleeksoft.ksoup.internal

import kotlin.math.min

object URLUtil {
    fun resolve(base: String, relative: String): String {
        if (relative.isEmpty()) return base

        // If the relative URL is already absolute (has a scheme), return it
        if (isAbsoluteUrl(relative)) {
            return relative
        }

        if (!isAbsoluteUrl(base)) {
            // At least one absolute link required
            return ""
        }

        // Parse the base URL into components (scheme, authority, path, query, fragment)
        val baseUrl = parseUrl(base)

        // Handle protocol-relative URLs (e.g. "//example.com/one")
        if (relative.startsWith("//")) {
            return baseUrl.scheme + ":" + relative
        }

        // Handle fragment or query-relative URLs
        if (relative.startsWith("?")) {
            return "${baseUrl.scheme}:${baseUrl.schemeSeparator}${baseUrl.authority}${baseUrl.path}$relative"
        }
        if (relative.startsWith("#")) {
            return "${baseUrl.scheme}:${baseUrl.schemeSeparator}${baseUrl.authority}${baseUrl.path}${baseUrl.query ?: ""}$relative"
        }

        // If the relative URL starts with "/", it's an absolute path on the current authority
        var resolvedPath = if (relative.startsWith("/")) {
            relative
        } else {
            // If the base URL has a query or fragment, we need to strip it before merging paths
            val cleanedBasePath = stripQueryAndFragment(baseUrl.path)
            mergePaths(cleanedBasePath, relative)
        }

        val relQueryIndex = resolvedPath.indexOf("?")
        val relFragmentIndex = resolvedPath.indexOf("#")

        val queryOrFragmentIndex = if (relQueryIndex != -1 && relFragmentIndex != -1) {
            min(relQueryIndex, relFragmentIndex)
        } else if (relFragmentIndex != -1) {
            relFragmentIndex
        } else {
            relQueryIndex
        }

        val queryOrFragment = if (queryOrFragmentIndex != -1) {
            val result = resolvedPath.substring(queryOrFragmentIndex)
            resolvedPath = resolvedPath.substring(0, queryOrFragmentIndex)
            result
        } else null

        // Normalize the path to resolve ".." and "."
        // add root slash to path only if authority is not empty
        val normalizedPath = normalizePath(resolvedPath, addRoot = baseUrl.authority.isNotEmpty()).let { if (queryOrFragment != null) it + queryOrFragment else it }

//        val relativeFragment = relative.substringAfter('#', "")

        // Form the final URL with scheme, authority, path, query, and fragment
        val finalUrl = StringBuilder()
        finalUrl.append("${baseUrl.scheme}:${baseUrl.schemeSeparator}${baseUrl.authority}$normalizedPath")

        return finalUrl.toString()
    }

    private fun isAbsoluteUrl(url: String): Boolean {
        return url.length > 2 && url.contains(":")
    }

    private fun mergePaths(basePath: String, relativePath: String): String {
        val baseDir = if (basePath.endsWith("/")) basePath else basePath.substring(0, basePath.lastIndexOf('/') + 1)
        return baseDir + relativePath
    }

    private fun normalizePath(path: String, addRoot: Boolean = true): String {
        val segments = path.split("/").toMutableList()
        val result = mutableListOf<String>()

        segments.forEachIndexed { index, segment ->
            when {
                segment.isEmpty() || segment == "." -> {
                    // if its last part and . then append / example: .com/b/c/d + ./g/. = .com/b/c/d/g/
                    if (index == segments.size - 1) {
                        result.add("")
                    }
                }

                segment == ".." -> {
                    // Go up a directory (pop last segment)
                    if (result.isNotEmpty()) {
                        result.removeAt(result.size - 1)
                    }
                }

                else -> {
                    result.add(segment)
                }
            }
        }

        return (if (addRoot) "/" else "") + result.joinToString("/")
    }

    private fun stripQueryAndFragment(path: String): String {
        val queryIndex = path.indexOf('?')
        val fragmentIndex = path.indexOf('#')
        return when {
            queryIndex != -1 -> path.substring(0, queryIndex)
            fragmentIndex != -1 -> path.substring(0, fragmentIndex)
            else -> path
        }
    }

    private data class ParsedUrl(
        val scheme: String,
        val schemeSeparator: String,
        val authority: String,
        val path: String,
        val query: String? = null,
        val fragment: String? = null
    )

    private fun parseUrl(url: String): ParsedUrl {
        var remainingUrl = url
        val scheme: String
        val schemeSeparator: String
        val schemeEndIndex = url.indexOf(":")
        if (schemeEndIndex != -1) {
            schemeSeparator = if (url.indexOf("://") != -1) {
                "//"
            } else if (url.indexOf(":/") != -1) {
                "/"
            } else {
                ""
            }
            scheme = url.substring(0, schemeEndIndex)
            remainingUrl = url.substring(schemeEndIndex + schemeSeparator.length + 1)
        } else {
            // If no scheme, default to "http" or you can adjust it to defaultScheme
            scheme = "https"
            schemeSeparator = "//"
        }

        val authorityEndIndex = if (schemeSeparator != "/") {
            remainingUrl.indexOf('/').takeIf { it != -1 } ?: remainingUrl.indexOf('?').takeIf { it != -1 } ?: remainingUrl.indexOf('#')
                .takeIf { it != -1 } ?: remainingUrl.length
        } else {
            // file paths
            -1
        }

        val authority = if (authorityEndIndex != -1) remainingUrl.substring(0, authorityEndIndex) else null
        val pathAndMore = if (authorityEndIndex == -1) remainingUrl else remainingUrl.substring(authorityEndIndex)
        val pathEndIndex = pathAndMore.indexOfAny(charArrayOf('?', '#')).takeIf { it != -1 } ?: pathAndMore.length
        val path = pathAndMore.substring(0, pathEndIndex)

        val queryStartIndex = pathAndMore.indexOf('?').takeIf { it != -1 } ?: pathAndMore.length
        val fragmentStartIndex = pathAndMore.indexOf('#').takeIf { it != -1 } ?: pathAndMore.length

        val query = if (queryStartIndex != pathAndMore.length) pathAndMore.substring(queryStartIndex, fragmentStartIndex) else null
        val fragment = if (fragmentStartIndex != pathAndMore.length) pathAndMore.substring(fragmentStartIndex) else null

        return ParsedUrl(
            scheme = scheme,
            schemeSeparator = schemeSeparator,
            authority = authority ?: "",
            path = path,
            query = query,
            fragment = fragment
        )
    }

}