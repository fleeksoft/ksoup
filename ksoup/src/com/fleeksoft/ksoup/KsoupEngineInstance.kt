package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.engine.KsoupEngine
import com.fleeksoft.ksoup.engine.KsoupEngineImpl

object KsoupEngineInstance {
    lateinit var ksoupEngine: KsoupEngine
        private set

    init {
        if (::ksoupEngine.isInitialized.not()) {
            init(KsoupEngineImpl)
        }
    }

    fun init(ksoupEngine: KsoupEngine) {
        this.ksoupEngine = ksoupEngine
    }
}