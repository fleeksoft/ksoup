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


include("ksoup-io", "ksoup-core-placeholder", "ksoup-core", "ksoup-test")
include("korio-io", "ksoup-korio", "ksoup-network-korio")
//include("kotlinx-io", "ksoup")
//include("sample:shared", "sample:android", "sample:desktop", "sample:ios")