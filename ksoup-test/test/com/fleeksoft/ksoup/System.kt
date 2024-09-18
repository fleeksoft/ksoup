package com.fleeksoft.ksoup

import kotlinx.datetime.Clock

public class System {
    public companion object {
        public fun currentTimeMillis(): Long {
            return Clock.System.now().toEpochMilliseconds()
        }
    }
}
