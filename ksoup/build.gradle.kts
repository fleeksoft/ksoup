plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    id("maven-publish")
}

group = "com.fleeksoft"
version = "0.0.1"

kotlin {
    jvm()

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
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "ksoup"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            //put your multiplatform dependencies here
            implementation(libs.ktor.core)

            implementation(libs.kotlinx.datetime)
//            implementation(libs.kotlinx.io)
            implementation(libs.codepoints)
            implementation(libs.okio)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.gson)
        }

        jvmTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.fleeksoft.ksoup"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

publishing {

    publications {

    }

    repositories {
        maven {

        }
    }
}

tasks.named("publish").configure {
    dependsOn("jvmTest")
//    testDebugUnitTest
//    iosSimulatorArm64Test
}

tasks.named("publishToMavenLocal").configure {
    dependsOn("iosSimulatorArm64Test")
}