package com.fleeksoft.ksoup

object KsoupEngineInstance {
    internal lateinit var ksoupEngine: KsoupEngine
        private set

    fun init(ksoupEngine: KsoupEngine) {
        this.ksoupEngine = ksoupEngine
    }
}