pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
//        maven("https://packages.jetbrains.team/maven/p/amper/amper")
//        maven("https://www.jetbrains.com/intellij-repository/releases")
//        maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    }
}

/*plugins {
    id("org.jetbrains.amper.settings.plugin").version("0.5.0-dev-992")
}*/

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

val libBuildType = settings.providers.gradleProperty("libBuildType").get()

include("ksoup-engine-common")

if (libBuildType == "lite" || libBuildType == "dev") {
    include("ksoup-engine-lite")
}

if (libBuildType == "korlibs" || libBuildType == "dev") {
    include("ksoup-engine-korlibs", "ksoup-network-korlibs")
}

if (libBuildType == "kotlinx" || libBuildType == "dev") {
    include("ksoup-engine-kotlinx", "ksoup-network")
}

if (libBuildType == "okio" || libBuildType == "dev") {
    include("ksoup-engine-okio", "ksoup-network-ktor2")
}

if (libBuildType == "ktor2" || libBuildType == "dev") {
    include("ksoup-engine-ktor2", "ksoup-network-ktor2")
}

if (libBuildType != "common") {
    include("ksoup")
    include("ksoup-test")
    include("ksoup-benchmark")
}

//include("sample:shared", "sample:desktop")
//include("sample:android", "sample:ios")