package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.kotlinx.KotlinxKsoupEngine
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString

object TestHelper {
    fun getResourceAbsolutePath(resourceName: String): String {
        if (Platform.isWindows()) {
            return "../../../../testResources/$resourceName"
        } else if (Platform.isJS()) {
            return "https://raw.githubusercontent.com/fleeksoft/ksoup/release/ksoup-test/testResources/$resourceName"
        }
        return "/Users/sabeeh/IdeaProjects/ksoup-new/ksoup-test/testResources/$resourceName"
    }

    fun readFile(filePath: String): String {
        return SystemFileSystem.source(Path(filePath)).buffered().readString()
    }

    fun initKsoup() {
        KsoupEngineInstance.init(KotlinxKsoupEngine())
    }
}