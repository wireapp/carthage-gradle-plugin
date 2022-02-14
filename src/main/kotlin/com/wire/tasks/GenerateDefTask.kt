package com.wire.tasks

import com.wire.carthage.models.Carthage
import com.wire.plugin.DefGeneratorArtifactDefinition
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import com.wire.plugin.DefGeneratorParameters
import com.wire.plugin.PlatformVariant
import com.wire.plugin.resolve
import java.io.File


abstract class GenerateDefTask : DefaultTask() {

    init {
        description = "Generate .def files for cinterop"
        group = "wire"
    }

    @get:Input
    @get:Option(option = "defGeneratorParameters", description = "The set of parameters to run use when generating .def files")
    @get:Optional
    abstract val parameters: Property<DefGeneratorParameters>

    @get:Input
    @get:Option(option = "tag", description = "A Tag to be used for debug")
    @get:Optional
    abstract val tag: Property<String>

    @OptIn(ExperimentalStdlibApi::class)
    @TaskAction
    fun generateDef() {

        fun formatVariantForLogging(variant: PlatformVariant): String {
            return if (variant.variantName.isNotEmpty()) {
                "-${variant.variantName}"
            } else { "-device" }
        }

        val nativeTarget = parameters.get().target ?: return

        val prettyTag = tag.orNull?.let { "[$it]" } ?: ""

        if (!nativeTarget.konanTarget.family.isAppleFamily) {
            logger.lifecycle("$prettyTag ${nativeTarget.konanTarget.name} is not an Apple family target. Nothing to do. Skipping.")
            return
        }

        val artifactDefinitions = parameters.get().artifactDefinitions
        val buildProducts = this.deriveBuildProducts()

        val defOutputDir = parameters.get().defOutputDir

        artifactDefinitions.forEach { artifactDefinition ->

            val tripletResolutionPair = Carthage.resolve(nativeTarget) ?: run {
                logger
                    .error(
                        "$prettyTag no platform-arch-variant triplet" +
                                "resolution was possible for target ${nativeTarget.konanTarget}"
                    )
                return
            }

            val carthagePlatform = tripletResolutionPair.first
            val platformVariant = tripletResolutionPair.second

            logger
                .lifecycle(
                    "$prettyTag generating .def for ${artifactDefinition.artifactName}, target triplet " +
                    carthagePlatform.platformString.lowercase() +
                    "-" +
                    nativeTarget.konanTarget.architecture.toString().lowercase() +
                    formatVariantForLogging(platformVariant)
                )

            if (artifactDefinition.isXCFramework) {

                val xcFrameworkPair = Carthage
                    .xcFramework(artifactDefinition.artifactName, inBuildProducts = buildProducts) ?: run {
                    logger.error("$prettyTag [XCFRAMERWORK] no .xcframework named ${artifactDefinition.artifactName} found")
                    return
                }
                val xcFrameworkDir = xcFrameworkPair.first
                val xcFramework = xcFrameworkPair.second

                logger.info("$prettyTag [XCFRAMEWORK] found at ${xcFrameworkDir.path}")

                val libraryIdentifier = xcFramework.availableLibraries.firstOrNull {
                    it.supportedPlatform.lowercase() == carthagePlatform.platformString.lowercase() &&
                    it.supportedArchitectures.map(String::lowercase).contains(nativeTarget.konanTarget.architecture.name.lowercase()) &&
                    (it.supportedPlatformVariant ?: "").lowercase() == platformVariant.variantName.lowercase()
                }?.identifier ?: run {
                    logger
                        .error(
                            "$prettyTag [XCFRAMEWORK] no library supporting triplet " +
                            carthagePlatform.platformString.lowercase() +
                            "-" +
                            nativeTarget.konanTarget.architecture.name.lowercase() +
                            formatVariantForLogging(platformVariant)
                        )
                    return
                }

                val headers = xcFrameworkDir
                    .resolve("$libraryIdentifier/${artifactDefinition.artifactName}.framework/Headers")
                    .walkTopDown()
                    .filter { it.extension == "h" && it.name != "${artifactDefinition.artifactName}.h" }

                headers.forEach { logger.info("$prettyTag [XCFRAMEWORK] header at: ${it.absolutePath}") }

                if (!defOutputDir.exists()) {
                    defOutputDir.mkdirs()
                }

                val targetDefDir = defOutputDir.resolve("${artifactDefinition.artifactName}/${nativeTarget.konanTarget.architecture}")
                if (!targetDefDir.exists()) {
                    targetDefDir.mkdirs()
                }
                var defFile = targetDefDir.resolve("${artifactDefinition.artifactName}.def")

                writeDef(artifactDefinition, headers, defFile)
            }
            else {
                TODO("handle regular frameworks as well")
            }
        }
    }

    private fun writeDef(
        forArtifact: DefGeneratorArtifactDefinition,
        withHeaders: Sequence<File>,
        toFile: File) {

        toFile.createNewFile()
        toFile.writeText(
            """
            language = Objective-C
            headers = ${withHeaders.map{ it.name }.joinToString(" ")}
            excludeDependentModules = true
            compilerOpts = -framework ${forArtifact.artifactName}
            linkerOpts = -framework ${forArtifact.artifactName}
            """.trimIndent()
        )
    }

    private fun deriveBuildProducts(): Carthage.BuildProducts {
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
