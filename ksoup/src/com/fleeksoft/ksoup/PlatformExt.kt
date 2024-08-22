package com.fleeksoft.ksoup

public fun Platform.isApple(): Boolean = this.current == PlatformType.IOS || this.current == PlatformType.MAC

public fun Platform.isWindows(): Boolean = this.current == PlatformType.WINDOWS

public fun Platform.isLinux(): Boolean = this.current == PlatformType.LINUX

public fun Platform.isJvmOrAndroid(): Boolean = this.current == PlatformType.JVM || this.current == PlatformType.ANDROID

public fun Platform.isJvm(): Boolean = this.current == PlatformType.JVM

public fun Platform.isJS(): Boolean = this.current == PlatformType.JS || this.current == PlatformType.WASM_JS

public fun Platform.isWasmJs(): Boolean = this.current == PlatformType.WASM_JS

