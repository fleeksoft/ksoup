pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
        maven("https://packages.jetbrains.team/maven/p/amper/amper")
        maven("https://www.jetbrains.com/intellij-repository/releases")
        maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
    }
}

/*plugins {
    id("org.jetbrains.amper.settings.plugin").version("0.5.0")
}*/

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

include("ksoup")
include("ksoup-io")
include("ksoup-io-fake") //just for test module
include("ksoup-kotlinx")
include("ksoup-network")
include("ksoup-network-ktor2")
include("ksoup-okio")
include("ksoup-korlibs")
include("ksoup-network-korlibs")
include("ksoup-test")
include("ksoup-network-test")
include("ksoup-benchmark")

//include("sample:shared", "sample:desktop")
//include("sample:android", "sample:ios")