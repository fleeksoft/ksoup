val isKorlibs = project.findProperty("isKorlibs")?.toString()?.toBoolean() ?: false
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
                const val isKotlinx: Boolean = ${!isKorlibs}
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
    sourceSets {
        commonTest {
            this.kotlin.srcDir(layout.buildDirectory.file(rootPath))
        }
    }
    js(IR) {
        browser {
            testTask {
                useMocha {
                    timeout = "9s"
                }
            }
        }
    }
}