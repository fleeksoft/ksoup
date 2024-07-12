import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

group = "com.fleeksoft.ksoup.test"
version = libs.versions.libraryVersion.get()

val rootPath = "generated/kotlin"

kotlin {
    explicitApi()

    jvm()

    js(IR) {
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
//        browser()
        nodejs()
    }
//    yet not supported by korlibs and amper
//    @OptIn(ExperimentalWasmDsl::class)
//    wasmWasi()

    linuxX64()
    linuxArm64()

    macosX64()
    macosArm64()

    mingwX64()

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
        macosX64(),
        macosArm64(),
        tvosX64(),
        tvosArm64(),
        tvosSimulatorArm64(),
//        watchosX64(),
        watchosArm64(),
        watchosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "ksoup-test"
            isStatic = true
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonTest {
            this.kotlin.srcDir(layout.buildDirectory.file(rootPath))
            dependencies {
                implementation(libs.codepoints)
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(projects.ksoup)
                implementation(projects.ksoupNetwork)
                implementation(libs.korlibs.io)
            }
        }

        // Define a new source set for shared JVM and Android tests
        val jvmAndroidCommonTest by creating {
            dependsOn(commonTest.get())
            kotlin.srcDir("src/jvmAndroidCommonTest/kotlin")
        }
        // Make JVM and Android test source sets depend on the new shared test source set
        jvmTest.get().dependsOn(jvmAndroidCommonTest)
        androidNativeTest.get().dependsOn(jvmAndroidCommonTest)
    }
}

android {
    namespace = "com.fleeksoft.ksoup.test"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}


val isGithubActions: Boolean = System.getenv("GITHUB_ACTIONS")?.toBoolean() == true
val generateBuildConfigFile: Task by tasks.creating {
    group = "build setup"
    val file = layout.buildDirectory.file("$rootPath/BuildConfig.kt")
    outputs.file(file)

    doLast {
        val content =
            """
            package com.fleeksoft.ksoup

            object BuildConfig {
                const val PROJECT_ROOT: String = "${rootProject.rootDir.absolutePath.replace("\\", "\\\\")}"
                const val isGithubActions: Boolean = $isGithubActions
            }
            """.trimIndent()
        file.get().asFile.writeText(content)
    }
}

tasks.all {
    if (name != generateBuildConfigFile.name && !name.contains("publish", ignoreCase = true)) {
        dependsOn(generateBuildConfigFile.name)
    }
}
