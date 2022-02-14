package com.wire.plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import java.io.File
import javax.inject.Inject
import com.wire.carthage.models.Carthage
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

abstract class WirePluginExtension @Inject constructor(val project: Project) {

    private val objects = project.objects

    var carthageParameters: Property<CarthageParameters> = objects.property(CarthageParameters::class.java)

    var defGeneratorParameters: Property<DefGeneratorParameters> = objects.property(DefGeneratorParameters::class.java)

    val tag: Property<String> = objects.property(String::class.java)

//    fun defFileForArtifact(artifactDefinition: DefGeneratorArtifactDefinition): File  {
//
//        val outputDir = defGeneratorParameters.get().defOutputDir
//        return File("${outputDir}/${artifactDefinition.artifactName}/${artifactDefinition.artifactTarget}/${artifactDefinition.artifactName}.def")
//    }

//    fun headersDirForArtifact(artifactDefinition: DefGeneratorArtifactDefinition): File  {
//        val headersDir = if (artifactDefinition.isXCFramework) {
//            "${artifactDefinition.artifactName}.xcframework/${artifactDefinition.artifactTarget}/${artifactDefinition.artifactName}.framework/Headers"
//        } else {
//            "${artifactDefinition.artifactName}.framework/Headers"
//        }
//        return File(headersDir)
//    }

//    @OptIn(ExperimentalStdlibApi::class)
//    fun createCinteropForTarget(target: KotlinNativeTarget) {
//        val mainCompilation = target.compilations.getByName(MAIN_COMPILATION_NAME)
//        val topLevelCarthageBuildOutputs = this.project.projectDir.resolve("Carthage/Build").walkTopDown().toList().filter { it.isDirectory }
//        val platformDirs = topLevelCarthageBuildOutputs.filter { Carthage.Platform.values().map { it.platformString.lowercase() }.contains( it.name.lowercase()) }
//        carthageParameters.get().dependencyArtifacts

//            .cinterops
//            .create(iosCryptoboxArtifact.artifactName) {
//                defFileProperty.set(project.rootDir.resolve("cryptography").resolve(wire.defFileForArtifact(iosCryptoboxArtifact)))
//                includeDirs("${project.rootDir}/cryptography/Carthage/Build/${wire.headersDirForArtifact(iosCryptoboxArtifact).path}")
//                packageName = "com.wire.${iosCryptoboxArtifact.artifactName}"
//            }
//    }
}

data class CarthageDependencyArtifact(val artifactName: String)

data class CarthageParameters(
    val command: CarthageCommand,
    val platforms: List<Carthage.Platform>,
    val useXCFrameworks: Boolean,
    val dependencyArtifacts: List<CarthageDependencyArtifact>
)

enum class CarthageCommand(val commandString: String) {
    BOOTSTRAP("bootstrap"),
    UPDATE("update")
}

data class DefGeneratorParameters(
    var defOutputDir: File,
    var artifactDefinitions: List<DefGeneratorArtifactDefinition>,
    var target: KotlinNativeTarget? = null
)

data class DefGeneratorArtifactDefinition(
    val artifactName: String,
    val isXCFramework: Boolean
)
