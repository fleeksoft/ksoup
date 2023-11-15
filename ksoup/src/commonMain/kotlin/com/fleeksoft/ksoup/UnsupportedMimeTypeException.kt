package com.fleeksoft.ksoup

import okio.IOException

/**
 * Signals that a HTTP response returned a mime type that is not supported.
 */
class UnsupportedMimeTypeException(message: String?, val mimeType: String, val url: String) :
    IOException(message) {

    override fun toString(): String {
        return super.toString() + ". Mimetype=" + mimeType + ", URL=" + url
    }
}
