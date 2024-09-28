plugins {
    alias(libs.plugins.kotlinx.benchmark)
    alias(libs.plugins.allopen)
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}


benchmark {
    targets {
        register("jvm")
    }

    configurations {
        named("main") {
//            exclude("org.jsoup.parser.JsoupBenchmark")
//            exclude("com.fleeksoft.ksoup.benchmark.KsoupBenchmark")
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
