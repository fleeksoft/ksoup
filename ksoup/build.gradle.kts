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

kotlin {
    explicitApi()

    jvm()

    linuxX64()
    linuxArm64()

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    listOf(
        iosX64(), iosArm64(), iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "ksoup"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)

            implementation(libs.kotlinx.datetime)
            implementation(libs.codepoints)
            api(libs.okio)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.gson)
            implementation(projects.ksoupNetwork)
        }

        jvmMain.dependencies {
        }

        jvmTest.dependencies {
        }

        androidMain.dependencies {
        }

        iosMain.dependencies {
        }

        linuxTest {
        }

        /*val jvmAndroidCommonMain by creating {
            dependsOn(commonMain.get())
            kotlin.srcDir("src/jvmAndroidCommonMain/kotlin")
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
        androidNativeTest.get().dependsOn(jvmAndroidCommonTest)*/
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

    val javadocJar = tasks.register<Jar>("javadocJar") {
        dependsOn(tasks.dokkaHtml)
        archiveClassifier.set("javadoc")
        from("${layout.buildDirectory}/dokka")
    }

    publications {
        withType<MavenPublication> {
            artifact(javadocJar)
            pom {
                name.set("Ksoup")
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
                        name.set("Sabeeh Ul Hussnain")
                        email.set("fleeksoft@gmail.com")
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