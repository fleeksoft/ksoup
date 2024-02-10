import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.dokka)
    id("maven-publish")
    id("signing")
}

group = "com.fleeksoft.ksoup"
version = libs.versions.libraryVersion.get()

val rootPath = "generated/kotlin"

kotlin {
    explicitApi()

    jvm()

    js(IR) {
        nodejs()
        browser()
    }

    linuxX64()
    linuxArm64()

    mingwX64()

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
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
    ).forEach {
        it.binaries.framework {
            baseName = "ksoup"
            isStatic = true
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            implementation(libs.codepoints)
            implementation(libs.korio)
        }
        commonTest {
            this.kotlin.srcDir(layout.buildDirectory.file(rootPath))
            dependencies {
                implementation(libs.kotlin.test)
                compileOnly(projects.ksoupNetwork)
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

publishing {
    repositories {
        maven {
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2")
            credentials {
                username = gradleLocalProperties(rootDir).getProperty("mavenCentralUsername")
                password = gradleLocalProperties(rootDir).getProperty("mavenCentralPassword")
            }
        }
    }

    val javadocJar =
        tasks.register<Jar>("javadocJar") {
            dependsOn(tasks.dokkaHtml)
            archiveClassifier.set("javadoc")
            from("${layout.buildDirectory}/dokka")
        }

    publications {
        withType<MavenPublication> {
            artifact(javadocJar)
            pom {
                name.set("Ksoup")
                description.set(
                    "Ksoup is a Kotlin Multiplatform library for working with HTML and XML, and offers an easy-to-use API for URL fetching, data parsing, extraction, and manipulation using DOM and CSS selectors.",
                )
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
                        name.set("Sabeeh Ul Hussnain")
                        email.set("fleeksoft@gmail.com")
                        organization.set("Fleek Soft")
                    }
                }
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(
//        File(rootDir, "gpg/private.key").readText(),
        gradleLocalProperties(rootDir).getProperty("gpgKeySecret"),
        gradleLocalProperties(rootDir).getProperty("gpgKeyPassword"),
    )
    sign(publishing.publications)
}

// TODO: remove after https://youtrack.jetbrains.com/issue/KT-46466 is fixed
project.tasks.withType(AbstractPublishToMaven::class.java).configureEach {
    dependsOn(project.tasks.withType(Sign::class.java))
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
