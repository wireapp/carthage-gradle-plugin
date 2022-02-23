package com.wire.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File


abstract class GenerateDefTask : DefaultTask() {

    init {
        description = "Generate .def files for cinterop"
        group = "wire"
    }

    @get:Input
    abstract val artifactName: Property<String>

    @get:Internal
    val defOutputDir: File = project.buildDir.resolve("carthage/defs")

    @get:OutputFile
    val outputFile: File
        get() {
            return defOutputDir.resolve("${artifactName.get()}.def")
        }

    @OptIn(ExperimentalStdlibApi::class)
    @TaskAction
    fun generateDef() {
        logger .lifecycle("generating .def for ${artifactName.get()}")

        if (!defOutputDir.exists()) {
            defOutputDir.mkdirs()
        }

        val defFile = defOutputDir.resolve("${artifactName.get()}.def")
        writeDef(artifactName.get(), defFile)
    }

    private fun writeDef(
        artifactName: String,
        toFile: File) {

        toFile.createNewFile()
        toFile.writeText(
            """
            language = Objective-C
            excludeDependentModules = true
            modules = $artifactName
            compilerOpts = -framework $artifactName
            linkerOpts = -framework $artifactName
            """.trimIndent()
        )
    }
}
