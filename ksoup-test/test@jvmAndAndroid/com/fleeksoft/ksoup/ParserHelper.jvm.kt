package com.fleeksoft.ksoup

import java.nio.file.Path
import java.nio.file.Paths


object ParserHelper {
    fun getPath(resourceName: String): Path {
        return Paths.get(TestHelper.getResourceAbsolutePath(resourceName))
    }
}