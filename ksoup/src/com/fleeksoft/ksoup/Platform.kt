package com.fleeksoft.ksoup

public enum class PlatformType {
    ANDROID,
    JVM,
    IOS,
    LINUX,
    JS,
    MAC,
    WINDOWS,
    WASM_JS,
}

public expect object Platform {
    public val current: PlatformType
}