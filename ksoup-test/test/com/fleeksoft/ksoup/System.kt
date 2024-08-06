package com.fleeksoft.ksoup

import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

public class System {
    public companion object {
        public fun nanoTime(): Long {
            return Clock.System.now().epochSeconds.seconds.inWholeNanoseconds
        }

        public fun currentTimeMillis(): Long {
            return Clock.System.now().epochSeconds.seconds.inWholeMilliseconds
        }
    }
}
