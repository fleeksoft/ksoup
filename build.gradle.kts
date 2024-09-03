import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import kotlin.jvm.optionals.getOrNull

plugins {
    alias(libs.plugins.kmp)
    alias(libs.plugins.androidLibrary)
}

val libFinder = versionCatalogs.find("libs").get()
var REAL_VERSION = libs.versions.libraryVersion.get()
val JVM_TARGET = JvmTarget.JVM_17
val JDK_VERSION = JavaVersion.VERSION_17

val GROUP = "com.fleeksoft"

kotlin {
    jvm()
    androidTarget()
}

allprojects {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
    version = REAL_VERSION
    group = GROUP

    project.apply(plugin = "kotlin-multiplatform")
    project.apply(plugin = "android-library")

    java.toolchain.languageVersion = JavaLanguageVersion.of(JDK_VERSION.majorVersion)
    kotlin.jvmToolchain(JDK_VERSION.majorVersion.toInt())
    afterEvaluate {
        tasks.withType(Test::class) {
            //this.javaLauncher.set()
            this.javaLauncher.set(javaToolchains.launcherFor {
                // 17 is latest at the current moment
                languageVersion.set(JavaLanguageVersion.of(JDK_VERSION.majorVersion))
            })
        }
    }

    android {
        compileOptions {
            sourceCompatibility = JDK_VERSION
            targetCompatibility = JDK_VERSION
        }
        compileSdk = 34
        namespace = "com.fleeksoft.${project.name.replace("-", ".")}"
        defaultConfig {
            minSdk = 21
        }
    }
    MicroAmper(this).configure()
}

subprojects {
    apply(plugin = "kotlin-multiplatform")

    kotlin {
        androidTarget {
            this.compilerOptions.jvmTarget.set(JVM_TARGET)
            publishAllLibraryVariants()
        }
    }

    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask::class) {
        compilerOptions.suppressWarnings.set(true)
        // @TODO: We should actually, convert warnings to errors and start removing warnings
        compilerOptions.freeCompilerArgs.add("-nowarn")
    }

    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink::class) {

        val folder = this.outputs.files.toList().firstOrNull()
        val fromFolder = File(project.projectDir, "testresources")

        if (folder != null) {
            val copyAfterLink = tasks.create("${this.name}CopyResources", Copy::class)
            copyAfterLink.from(fromFolder)
            copyAfterLink.into(folder)
            this.dependsOn(copyAfterLink)
        }
    }
}

// Tiny, coupled and limited variant of amper compatible with the current structure, so we can bump to Kotlin 2.0.0 in the meantime, while amper is discarded or evolved.
class MicroAmper(val project: Project) {
    private var kotlinPlatforms = mutableListOf<String>()
    private var kotlinAliases = LinkedHashMap<String, List<String>>()
    private var deps = mutableListOf<Dep>()

    //val kotlinBasePlatforms by lazy { kotlinPlatforms.groupBy { getKotlinBasePlatform(it) }.filter { it.value != listOf(it.key) } }
    val kotlinBasePlatforms by lazy { kotlinPlatforms.groupBy { getKotlinBasePlatform(it) } }

    fun getKotlinBasePlatform(platform: String): String =
        platform.removeSuffix("X64").removeSuffix("X86").removeSuffix("Arm64").removeSuffix("Arm32").removeSuffix("Simulator").removeSuffix("Device")
            .also {
                check(it.all { it.isLowerCase() && !it.isDigit() })
            }

    data class Dep(val path: String, val exported: Boolean, val test: Boolean, val platform: String, val compileOnly: Boolean) {
        val rplatform = platform.takeIf { it.isNotEmpty() } ?: "common"
        val configuration =
            "$rplatform${if (test) "Test" else "Main"}${if (exported) "Api" else if (compileOnly) "CompileOnly" else "Implementation"}"
    }

    fun parseFile(file: File, lines: List<String> = file.readLines()) {
        var mode = ""

        for (line in lines) {
            val tline = line.substringBeforeLast('#').trim().takeIf { it.isNotEmpty() } ?: continue

            if (line.startsWith(" ") || line.startsWith("\t") || line.startsWith("-")) {
                when {
                    mode == "product" -> {
                        //println("product=$tline")
                        when {
                            tline.startsWith("platforms:") -> {
                                val platforms = tline.substringAfter('[').substringBeforeLast(']').split(',').map { it.trim() }
                                kotlinPlatforms.addAll(platforms)
                            }
                        }
                    }

                    mode == "aliases" -> {
                        //println("aliases=$tline")
                        if (tline.startsWith("-")) {
                            val (alias2, platforms2) = tline.split(":", limit = 2)
                            val alias = alias2.trim('-', ' ')
                            val platforms = platforms2.trim('[', ']', ' ').split(',').map { it.trim() }
                            //println(" -> alias=$alias, platforms=$platforms")
                            kotlinAliases[alias] = platforms
                        }
                    }

                    mode.contains("dependencies") -> {
                        val platform = mode.substringAfterLast('@', "")
                        val test = mode.startsWith("test")
                        val compileOnly = line.contains(Regex(":\\s*compile-only"))
                        val exported = line.contains(Regex(":\\s*exported"))
                        val path = tline.removePrefix("-").removeSuffix(": exported").removeSuffix(":exported").removeSuffix(": compile-only")
                            .removeSuffix(":compile-only").trim()
                        if (platform.isBlank() || kotlinBasePlatforms.contains(platform) || kotlinAliases.contains(platform) || platform == "apple") {
                            deps += Dep(path = path, exported = exported, test = test, platform = platform, compileOnly = compileOnly)
                        } else {
                            println("platform not included: $platform in $project")
                        }
                    }
                }
            } else {
                if (tline.endsWith(":")) {
                    mode = tline.trimEnd(':').trim()
                }
                if (tline.startsWith("apply:")) {
                    val paths = tline.substringAfter(':').trim('[', ',', ' ', ']').split(",")
                    for (path in paths) {
                        parseFile(file.parentFile.resolve(path))
                    }
                }
            }
        }
    }

    data class SourceSetPair(val main: KotlinSourceSet, val test: KotlinSourceSet) {
        fun dependsOn(other: SourceSetPair) {
            main.dependsOn(other.main)
            test.dependsOn(other.test)
        }
    }

    val sourceSetPairs = LinkedHashMap<String, SourceSetPair>()

    // specific depends on more generic
    fun NamedDomainObjectContainer<KotlinSourceSet>.ssDependsOn(base: String, other: String) {
        if (base == other) return
        //println("$base dependsOn $other")
        ssPair(base).dependsOn(ssPair(other))
    }

    val projectFiles: Set<String> = (project.projectDir.list() ?: emptyArray()).toSet()

    fun SourceDirectorySet.srcDirIfExists(path: String) {
        //if (path in projectFiles) setSrcDirs(listOf(path)) //else println("file doesn't exist $path")
        //srcDir(path)
        setSrcDirs(listOf(path))
    }

    fun NamedDomainObjectContainer<KotlinSourceSet>.ssPair(name: String): SourceSetPair {
        return sourceSetPairs.getOrPut(name) {
            val atName = if (name == "common") "" else "@$name"
            SourceSetPair(
                main = maybeCreate("${name}Main").also {
                    it.kotlin.srcDirIfExists("src$atName")
                    it.resources.srcDirIfExists("resources$atName")
                    it.kotlin.srcDir("build/generated/ksp/$name/${name}Main/kotlin")
                },
                test = maybeCreate("${name}Test").also {
                    it.kotlin.srcDirIfExists("test$atName")
                    it.resources.srcDirIfExists("testResources$atName")
                    it.kotlin.srcDir("build/generated/ksp/$name/${name}Test/kotlin")
                }
            )
        }
    }

    fun applyTo() = with(project) {
        project.kotlin.sourceSets {
            ssDependsOn("native", "common")
            ssDependsOn("posix", "native")
            ssDependsOn("apple", "posix")
            ssDependsOn("appleNonWatchos", "apple")
            ssDependsOn("appleIosTvos", "apple")

            maybeCreate("commonMain").kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")

            for (platform in kotlinPlatforms) {
                val isMacos = platform.startsWith("macos")
                val isIos = platform.startsWith("ios")
                val isTvos = platform.startsWith("tvos")
                val isWatchos = platform.startsWith("watchos")
                val isNative = platform.contains("X86") || platform.contains("X64") || platform.contains("Arm")
                val isApple = isMacos || isIos || isTvos || isWatchos
                val isLinux = platform.startsWith("linux")
                val isWindows = platform.startsWith("mingw")
                val isPosix = isLinux || isApple
                val basePlatform = getKotlinBasePlatform(platform)
                if (isIos || isTvos) ssDependsOn(basePlatform, "appleIosTvos")
                if (isApple && !isWatchos) ssDependsOn(basePlatform, "appleNonWatchos")
                if (isPosix) ssDependsOn(basePlatform, "posix")
                if (isApple) ssDependsOn(basePlatform, "apple")
                if (isNative) ssDependsOn(basePlatform, "native")
                if (platform != basePlatform) ssDependsOn(platform, basePlatform)
            }
        }

        for (platform in kotlinPlatforms) {
            when (platform) {
                "jvm" -> kotlin.jvm {
                    compilerOptions {
                        this.jvmTarget.set(JVM_TARGET)
                    }
                }

                "js" -> kotlin.js {
                    browser {
                        testTask {
                            useMocha {
                                timeout = "9s"
                            }
                        }
                    }
                    nodejs {
                        testTask {
                            useMocha {
                                timeout = "9s"
                            }
                        }
                    }
                }

                "wasm" -> {
                    kotlin.wasmJs {
                        browser {
                            testTask {
                                useMocha {
                                    timeout = "9s"
                                }
                            }
                        }
                        nodejs {
                            testTask {
                                useMocha {
                                    timeout = "9s"
                                }
                            }
                        }
                    }
                    kotlin.sourceSets {
                        ssDependsOn("wasmJs", "wasm")
                    }
                }

                "android" -> kotlin.androidTarget {}
                "linuxX64" -> kotlin.linuxX64()
                "linuxArm64" -> kotlin.linuxArm64()
                "tvosArm64" -> kotlin.tvosArm64()
                "tvosX64" -> kotlin.tvosX64()
                "tvosSimulatorArm64" -> kotlin.tvosSimulatorArm64()
                "macosX64" -> kotlin.macosX64()
                "macosArm64" -> kotlin.macosArm64()
                "iosArm64" -> kotlin.iosArm64()
                "iosSimulatorArm64" -> kotlin.iosSimulatorArm64()
                "iosX64" -> kotlin.iosX64()
                "watchosArm64" -> kotlin.watchosArm64()
                "watchosArm32" -> kotlin.watchosArm32()
                "watchosDeviceArm64" -> kotlin.watchosDeviceArm64()
                "watchosSimulatorArm64" -> kotlin.watchosSimulatorArm64()
                "mingwX64" -> kotlin.mingwX64()
            }
        }

        //kotlin.applyDefaultHierarchyTemplate()

        kotlin.targets.forEach {
            it.compilations.forEach {
                it.compileTaskProvider.configure {
                    compilerOptions {
                        // apiVersion: Allow to use declarations only from the specified version of bundled libraries
                        // languageVersion: Provide source compatibility with specified language version
                        //this.apiVersion.set(KotlinVersion.KOTLIN_2_0)
                        //this.languageVersion.set(KotlinVersion.KOTLIN_2_0)
                    }
                }
            }
        }

        kotlin.sourceSets {
            // jvm, js, wasm, android, linuxX64, linuxArm64, tvosArm64, tvosX64, tvosSimulatorArm64, macosX64, macosArm64, iosArm64, iosSimulatorArm64, iosX64, watchosArm64, watchosArm32, watchosDeviceArm64, watchosSimulatorArm64, mingwX64

            for ((alias, platforms) in (kotlinAliases + kotlinBasePlatforms)) {
                //for ((alias, platforms) in kotlinAliases) {
                ssDependsOn(alias, "common")
                for (platform in platforms) ssDependsOn(platform, alias)
            }
        }
        //println(" -> $platforms")

        dependencies {
            for (dep in deps) {
                add(
                    dep.configuration, when {
                        dep.path.contains('/') -> project(":${File(dep.path).name}")
                        dep.path.startsWith("\$") -> {
                            when (dep.path) {
                                "\$kotlin-test" -> "org.jetbrains.kotlin:kotlin-test"
                                else -> {
                                    val result = libFinder.findLibrary(dep.path.replace("\$libs.", "").replace(".", "-")).getOrNull()?.get()
                                    result?.toString() ?: TODO("Unknown ${dep.path}, $result")
                                }
                            }
                        }

                        else -> dep.path
                    }
                )
            }
        }

        for (target in kotlin.targets) {
            target.compilations.all {
                compileTaskProvider.configure {
                    compilerOptions {
                        suppressWarnings.set(true)
                    }
                }
            }
        }
    }

    fun configure() {
        val amperFile = File(project.projectDir, "module.yaml").takeIf { it.exists() } ?: return
        parseFile(amperFile)
        applyTo()
    }
}