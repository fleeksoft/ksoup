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
    id("org.jetbrains.amper.settings.plugin").version("0.5.0-dev-966")
}

val isKorlibs = settings.providers.gradleProperty("isKorlibs").get().toBoolean()

include("ksoup")
include("ksoup-engine-common")
if (isKorlibs) {
    include("ksoup-engine-korlibs", "ksoup-network-korlibs")
} else {
    include("ksoup-engine-kotlinx", "ksoup-network")
}
include("ksoup-test")
//include("sample:shared", "sample:desktop")
//include("sample:android", "sample:ios")