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
if (libBuildType == "korlibs" || libBuildType == "common") {
    include("ksoup-engine-korlibs", "ksoup-network-korlibs")
}

if (libBuildType == "kotlinx" || libBuildType == "common") {
    include("ksoup-engine-kotlinx", "ksoup-network")
}

if (libBuildType == "okio" || libBuildType == "common") {
    include("ksoup-engine-okio", "ksoup-network-ktor2")
}

if (libBuildType == "ktor2" || libBuildType == "common") {
    include("ksoup-engine-ktor2", "ksoup-network-ktor2")
}

include("ksoup")
include("ksoup-test")

//include("sample:shared", "sample:desktop")
//include("sample:android", "sample:ios")