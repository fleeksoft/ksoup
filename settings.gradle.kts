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


include("ksoup")
include("ksoup-engine-common")
include("ksoup-engine-kotlinx", "ksoup-network")
include("ksoup-engine-korlibs", "ksoup-network-korlibs")
include("ksoup-test")
include("sample:shared", "sample:desktop")
//include("sample:android", "sample:ios")