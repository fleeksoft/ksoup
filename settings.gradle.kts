pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
        maven("https://packages.jetbrains.team/maven/p/amper/amper")
        maven("https://www.jetbrains.com/intellij-repository/releases")
        maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    }
}

plugins {
    id("org.jetbrains.amper.settings.plugin").version("0.4.0")
}


include("ksoup-core")
include("ksoup-korio", "ksoup-network-korio", "ksoup-test")
//include("ksoup")
//include("sample:shared", "sample:android", "sample:desktop", "sample:ios")