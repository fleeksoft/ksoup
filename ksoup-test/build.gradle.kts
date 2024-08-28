@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

val libBuildType = project.findProperty("libBuildType")?.toString()
plugins {
    alias(libs.plugins.power.assert)
}
val rootPath = "generated/kotlin"
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
                const val libBuildType: String = "$libBuildType"
                const val isKotlinx: Boolean = ${libBuildType == "kotlinx" || libBuildType == "dev"}
                const val isKorlibs: Boolean = ${libBuildType == "korlibs"}
                const val isOkio: Boolean = ${libBuildType == "okio"}
            }
            """.trimIndent()
        file.get().asFile.writeText(content)
    }
}

tasks.configureEach {
    if (name != generateBuildConfigFile.name && !name.contains("publish", ignoreCase = true)) {
        dependsOn(generateBuildConfigFile.name)
    }
}

kotlin {
    js(IR) {
        browser {
            testTask {
                useMocha {
                    timeout = "9s"
                }
            }
        }
    }
    if (libBuildType != "okio") {
        wasmJs()
    }
    sourceSets {
        commonTest {
            this.kotlin.srcDir(layout.buildDirectory.file(rootPath))
        }
    }
}