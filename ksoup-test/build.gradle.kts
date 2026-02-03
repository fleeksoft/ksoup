val rootPath = "generated/kotlin"
val isGithubActions: Boolean = System.getenv("GITHUB_ACTIONS")?.toBoolean() == true

val libBuildType = project.findProperty("libBuildType")?.toString()
kotlin {
    sourceSets {
        commonTest {
            dependencies {
                when (libBuildType) {
                    "kotlinx" -> {
//                        implementation("com.fleeksoft.ksoup:ksoup-kotlinx:${libs.versions.libraryVersion.get()}")
                        implementation(project(":ksoup-kotlinx"))
                    }

                    "okio" -> {
//                        implementation("com.fleeksoft.ksoup:ksoup-okio:${libs.versions.libraryVersion.get()}")
                        implementation(project(":ksoup-okio"))
                    }

                    else -> {
                        implementation(project(":ksoup-io-fake"))
                    }
                }
            }
        }
    }
}
kotlin {
    sourceSets {
        commonTest {
            this.kotlin.srcDir(layout.buildDirectory.file(rootPath))
        }
    }
}

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
                const val isKotlinx: Boolean = ${libBuildType == "kotlinx" || libBuildType == "common"}
                const val isOkio: Boolean = ${libBuildType == "okio"}
                const val isCore: Boolean = ${libBuildType == "core"}
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