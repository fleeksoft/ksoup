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
    id("org.jetbrains.amper.settings.plugin").version("0.5.0-dev-992")
}

val libBuildType = settings.providers.gradleProperty("libBuildType").get()

include("ksoup-engine-common")
if (libBuildType == "korlibs" || libBuildType == "dev") {
    include("ksoup-engine-korlibs", "ksoup-network-korlibs")
}

if (libBuildType == "kotlinx" || libBuildType == "dev") {
    include("ksoup-engine-kotlinx", "ksoup-network")
}

if (libBuildType == "okio" || libBuildType == "dev") {
    include("ksoup-engine-okio")
}

if (libBuildType == "ktor2" || libBuildType == "dev") {
    include("ksoup-engine-ktor2")
}

include("ksoup")
include("ksoup-test")
//include("sample:shared", "sample:desktop")
//include("sample:android", "sample:ios")