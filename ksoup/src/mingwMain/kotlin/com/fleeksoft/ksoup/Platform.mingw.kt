package com.fleeksoft.ksoup

public actual object Platform {
    public actual val current: PlatformType
        get() = PlatformType.WINDOWS
}
