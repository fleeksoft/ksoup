@file:OptIn(ExperimentalTime::class)

package com.fleeksoft.ksoup

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

public class System {
    public companion object {
        public fun currentTimeMillis(): Long {
            return Clock.System.now().toEpochMilliseconds()
        }
    }
}
