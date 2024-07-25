import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
}

group = "com.fleeksoft.ksoup"
version = libs.versions.libraryVersion.get()

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
        watchosX64(),
        watchosArm64(),
        watchosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "ksoup-network"
            isStatic = true
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            compileOnly(projects.ksoup)
            api(libs.ktor.client.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(projects.ksoup)
        }

        val nonJsMain by creating {
            dependsOn(commonMain.get())
        }

        jvmMain {
            dependsOn(nonJsMain)
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }

        androidMain {
            dependsOn(nonJsMain)
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }

        appleMain {
            dependsOn(nonJsMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        linuxMain {
            dependsOn(nonJsMain)
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }

        jsMain.dependencies {
            implementation(libs.ktor.client.js)
        }

        mingwMain {
            dependencies {
                implementation(libs.ktor.client.win)
            }
        }
    }
}

android {
    namespace = "com.fleeksoft.ksoup.network"
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
    coordinates("com.fleeksoft.ksoup", "ksoup-network", libs.versions.libraryVersion.get())
    pom {
        name.set("ksoup-network")
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
