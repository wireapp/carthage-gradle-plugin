package com.wire.plugin

import com.wire.carthage.models.Carthage
import org.gradle.api.Plugin
import org.gradle.api.Project
import com.wire.tasks.*
import org.gradle.api.tasks.Copy
import java.io.File
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.lang.IllegalStateException


const val EXTENSION_NAME = "carthage"

abstract class CarthagePlugin : Plugin<Project> {

    override fun apply(project: Project) = with(project) {

        val extension = project
            .extensions
            .create(EXTENSION_NAME, CarthagePluginExtension::class.java, project)

        project.afterEvaluate {
            pluginManager.withPlugin(MULTIPLATFORM_PLUGIN_NAME) {

                if (pluginManager.hasPlugin(MULTIPLATFORM_PLUGIN_NAME).not()) {
                    logger.error("Multiplatform plugin not available")
                    return@withPlugin
                }

                val multiplatformExtension = this@with
                    .extensions
                    .getByName(KOTLIN_PROJECT_EXTENSION_NAME) as KotlinMultiplatformExtension

                project.tasks.register("carthage-clean") {
                    project.buildDir.resolve(Carthage.ROOT_DIR).deleteRecursively()
                }

                createInterops(project, multiplatformExtension, extension)
                createCopyFrameworksTasks(project, multiplatformExtension)
            }
        }
    }

    fun createCopyFrameworksTasks(
        project: Project,
        kotlinExtension: KotlinMultiplatformExtension
    ) {
        val buildProducts = deriveBuildProducts(project)

        kotlinExtension.supportedTargets().all { target ->
            val frameworks = Carthage.xcFrameworks(buildProducts)
            val targetFrameworks = frameworks.map { it.first.resolve(targetLibraryIdentifier(project, it.second, target)) }

            target.binaries.all { binary ->
                val copyFrameworksTask = project.tasks.register("copy-carthage-frameworks-${target.name}-${binary.name}", Copy::class.java) {
                    from(targetFrameworks)
                    into(binary.outputDirectory.resolve("Frameworks"))
                }

                binary.linkTask.dependsOn(copyFrameworksTask)
                return@all true
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun createInterops(
        project: Project,
        kotlinExtension: KotlinMultiplatformExtension,
        carthagePluginExtension: CarthagePluginExtension
    ) {
        val carthageRunTask = project
            .tasks
            .register("carthage-run", CarthageTask::class.java) {
                this.platforms.set(project.provider { carthagePluginExtension.platforms })
                this.command.set(project.provider { carthagePluginExtension.command })
                this.useXCFramework.set(project.provider { true })
            }

        carthagePluginExtension.dependencies.all { dependency -> Unit
            val defGenTask = project
                .tasks
                .register(
                    "carthage-defgen-${dependency.moduleName}",
                    GenerateDefTask::class.java
                ) {
                    this.artifactName.set(project.provider { dependency.moduleName })
                    this.dependsOn(carthageRunTask)
                }

            kotlinExtension.supportedTargets().all { target -> Unit
                val (xcFrameworkDir, xcFramework) = framework(project, dependency.moduleName)
                val libraryIdentifier = targetLibraryIdentifier(project, xcFramework, target)

                val headersDir = xcFrameworkDir.resolve("$libraryIdentifier/${dependency.moduleName}.framework/Headers")
                val frameworkDir = xcFrameworkDir.resolve("$libraryIdentifier")

                target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).cinterops.create(dependency.moduleName) {

                    val interopTask = project.tasks.getByPath(interopProcessingTaskName)
                    interopTask.dependsOn(defGenTask)

                    packageName =  dependency.packageName
                    defFileProperty.set(defGenTask.map { it.outputFile})
                    includeDirs(headersDir)
                    compilerOpts("-F$frameworkDir")
                }

                target.binaries {
                    getTest("DEBUG").linkerOpts.add("-F$frameworkDir")
                    framework {
                        baseName = carthagePluginExtension.baseName
                        transitiveExport = true
                        linkerOpts(
                            "-F$frameworkDir"
                        )
                    }
                }
                return@all true
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun targetLibraryIdentifier(project: Project, xcFramework: Carthage.XCFramework, target: KotlinNativeTarget): String {
        val tripletResolutionPair = Carthage.resolve(target) ?: throw IllegalStateException()
        val carthagePlatform = tripletResolutionPair.first
        val platformVariant = tripletResolutionPair.second

        val libraryIdentifier = xcFramework.availableLibraries.firstOrNull {
            it.supportedPlatform.lowercase() == carthagePlatform.platformString.lowercase() &&
                    it.supportedArchitectures.map(String::lowercase).contains(target.konanTarget.architecture.name.lowercase()) &&
                    (it.supportedPlatformVariant ?: "").lowercase() == platformVariant.variantName.lowercase()
        }?.identifier ?: run {

            project.logger
                .error(
                    "[XCFRAMEWORK] no library supporting triplet " +
                            carthagePlatform.platformString.lowercase() +
                            "-" +
                            target.konanTarget.architecture.name.lowercase() +
                            formatVariantForLogging(platformVariant)
                )
            throw IllegalStateException()
        }

        return libraryIdentifier
    }

    fun framework(project: Project, artifactName: String): Pair<File, Carthage.XCFramework> {
        val buildProducts = deriveBuildProducts(project)
        val xcFrameworkPair = Carthage
            .xcFramework(artifactName, inBuildProducts = buildProducts) ?: run {
            project.logger.error("[XCFRAMERWORK] no .xcframework named ${artifactName} found")
            throw IllegalStateException()
        }
        return xcFrameworkPair
    }

    fun formatVariantForLogging(variant: PlatformVariant): String {
        return if (variant.variantName.isNotEmpty()) {
            "-${variant.variantName}"
        } else { "-device" }
    }

    private fun deriveBuildProducts(project: Project): Carthage.BuildProducts {
        project.logger.info("retrieving carthage build products")
        val buildProducts = Carthage.buildProducts(project.projectDir)
        project.logger.info("found the following build products")

        buildProducts.xcFrameworksPaths.forEach {
            project.logger.info("[XCFRAMEWORK] $it")
        }

        buildProducts.frameworksByPlatformPaths.forEach {
            it.value.forEach { frameworkPath ->
                project.logger.info("[FRAMEWORK][${it.key.platformString}] $frameworkPath")
            }
        }
        return buildProducts
    }
}

internal fun KotlinMultiplatformExtension.supportedTargets() = targets
    .withType(KotlinNativeTarget::class.java)
    .matching { it.konanTarget.family.isAppleFamily }

private const val MULTIPLATFORM_PLUGIN_NAME = "kotlin-multiplatform"
private const val KOTLIN_PROJECT_EXTENSION_NAME = "kotlin"

fun Carthage.Companion.resolve(target: KotlinNativeTarget): Pair<Carthage.Platform, PlatformVariant>? {

    if (!target.konanTarget.family.isAppleFamily) {
        return null
    }

    return when (target.konanTarget) {

        // iOS
        KonanTarget.IOS_ARM32,
        KonanTarget.IOS_ARM64 -> Pair(Carthage.Platform.IOS, PlatformVariant.DEVICE)

        KonanTarget.IOS_SIMULATOR_ARM64,
        KonanTarget.IOS_X64 -> Pair(Carthage.Platform.IOS, PlatformVariant.SIMULATOR)

        // macOS
        KonanTarget.MACOS_X64,
        KonanTarget.MACOS_ARM64 -> Pair(Carthage.Platform.MACOS, PlatformVariant.DEVICE)

        // tvOS
        KonanTarget.TVOS_ARM64 -> Pair(Carthage.Platform.TVOS, PlatformVariant.DEVICE)

        KonanTarget.TVOS_SIMULATOR_ARM64,
        KonanTarget.TVOS_X64 -> Pair(Carthage.Platform.TVOS, PlatformVariant.SIMULATOR)

        // watchOS
        KonanTarget.WATCHOS_ARM32,
        KonanTarget.WATCHOS_ARM64 -> Pair(Carthage.Platform.WATCHOS, PlatformVariant.DEVICE)

        KonanTarget.WATCHOS_X64,
        KonanTarget.WATCHOS_X86,
        KonanTarget.WATCHOS_SIMULATOR_ARM64 -> Pair(Carthage.Platform.WATCHOS, PlatformVariant.SIMULATOR)

        else -> null
    }
}

enum class PlatformVariant(val variantName: String) {
    SIMULATOR(variantName = "simulator"),
    DEVICE(variantName = ""),
}
