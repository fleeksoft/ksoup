plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
}

group = "com.fleeksoft.ksoup"
version = libs.versions.libraryVersion.get()

val rootPath = "generated/kotlin"

kotlin {
    explicitApi()

    jvm()

    js(IR) {
        nodejs()
    }

    wasmJs {
        nodejs()
    }

    linuxX64()
    linuxArm64()

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
        watchosX64(),
        watchosArm64(),
        watchosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "ksoup"
            isStatic = true
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.codepoints)
            api(libs.kotlinx.io)
        }
        commonTest {
            this.kotlin.srcDir(layout.buildDirectory.file(rootPath))
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        jvmMain.dependencies {
        }

        jvmTest.dependencies {
        }

        androidMain.dependencies {
        }

        appleMain.dependencies {
        }

        jsMain.dependencies {
        }

        val jvmAndroidCommonMain by creating {
            dependsOn(commonMain.get())
            kotlin.srcDir("src/jvmAndroidCommonMain/kotlin")
        }

        jsTest {
        }

        // Make JVM and Android source sets depend on the new shared source set
        jvmMain.get().dependsOn(jvmAndroidCommonMain)
        androidMain.get().dependsOn(jvmAndroidCommonMain)

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
    namespace = "com.fleeksoft.ksoup"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

mavenPublishing {
    coordinates("com.fleeksoft.ksoup", "ksoup", libs.versions.libraryVersion.get())
    pom {
        name.set("ksoup")
        description.set("Ksoup is a Kotlin Multiplatform library for working with HTML and XML, and offers an easy-to-use API for URL fetching, data parsing, extraction, and manipulation using DOM and CSS selectors.")
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://opensource.org/licenses/Apache-2.0")
            }
        }
        url.set("https://github.com/fleeksoft/ksoup")
        issueManagement {
            system.set("Github")
            url.set("https://github.com/fleeksoft/ksoup/issues")
        }
        scm {
            connection.set("https://github.com/fleeksoft/ksoup.git")
            url.set("https://github.com/fleeksoft/ksoup")
        }
        developers {
            developer {
                name.set("Sabeeh Ul Hussnain Anjum")
                email.set("fleeksoft@gmail.com")
                organization.set("Fleek Soft")
            }
        }
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
