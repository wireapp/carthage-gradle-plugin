package com.wire.plugin

import com.wire.carthage.models.Carthage
import org.gradle.api.Plugin
import org.gradle.api.Project
import com.wire.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin
import org.jetbrains.kotlin.konan.target.KonanTarget


const val EXTENSION_NAME = "wire"

abstract class WirePlugin : Plugin<Project> {

//    internal fun KotlinMultiplatformExtension.supportedTargets() = targets
//        .withType(KotlinNativeTarget::class.java)
//        .matching { it.konanTarget.family.isAppleFamily }
//
//internal fun KotlinMultiplatformExtension.supportedTargets() = targets
//    .withType(KotlinNativeTarget::class.java)
//    .matching { it.konanTarget.family.isAppleFamily }

    override fun apply(project: Project) = with(project) {

        val extension = project
            .extensions
            .create(EXTENSION_NAME, WirePluginExtension::class.java, project)

        project.afterEvaluate {
            pluginManager.withPlugin(MULTIPLATFORM_PLUGIN_NAME) {

                if (pluginManager.hasPlugin(MULTIPLATFORM_PLUGIN_NAME).not()) {
                    logger.error("NONCSAONFASKGASKASKGHJASKGASKJ")

                    return@withPlugin
                }

                val multiplatformExtension = this@with
                    .extensions
                    .getByName(KOTLIN_PROJECT_EXTENSION_NAME) as KotlinMultiplatformExtension

                project.tasks.register("carthage-clean") {
                    project.buildDir.resolve(Carthage.ROOT_DIR).deleteRecursively()
                }

                val carthageRunTask = project
                    .tasks
                    .register("carthage-run", CarthageTask::class.java) {
                        this.tag.set(extension.tag)
                        this.parameters.set(extension.carthageParameters)
                    }

                project.logger.lifecycle("------- Checking Targets ------")
                project.logger.lifecycle("------- ${multiplatformExtension.supportedTargets().toList()} ------")

                multiplatformExtension.supportedTargets().forEach { target ->
                    project.logger.lifecycle("------- Found Target ${target.konanTarget} ------")
                    project
                        .tasks
                        .register(
                            "carthage-defgen-${target.konanTarget.architecture}",
                            GenerateDefTask::class.java
                        ) {
                            this.tag.set(extension.tag)
                            extension.defGeneratorParameters.get().target = target
                            this.parameters.set(extension.defGeneratorParameters)
//                    this.dependsOn(carthageRunTask)
                        }
                }

            }
        }


//
//        project.tasks.register("carthage-compilation") {
//
//        }
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
