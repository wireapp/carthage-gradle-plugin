package com.wire.tasks
import com.wire.carthage.models.Carthage
import com.wire.plugin.CarthageCommand
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.api.provider.ListProperty

abstract class CarthageTask : DefaultTask() {

    init {
        description = "Run Carthage"
        group = "wire"
    }

    @get:Input
    @get:Option(option = "platforms", description = "The set of platforms which Carthage should build for")
    abstract val platforms: ListProperty<Carthage.Platform>

    @get:Input
    @get:Option(option = "command", description = "The carthage command to execute")
    abstract val command: Property<CarthageCommand>

    @get:Input
    @get:Option(option = "useXCFramework", description = "If carthage should build xcframeworks")
    abstract val useXCFramework: Property<Boolean>

    @TaskAction
    fun carthageAction() {
        val platformList = platforms.get().joinToString(" ")
        logger.lifecycle("carthage invocation: carthage ${command.get().commandString} --cache-builds --platform $platformList ${if(useXCFramework.get()) "--use-xcframeworks" else ""}")

        project.exec {
            this.executable = "carthage"
            this.workingDir = project.projectDir

            this.args(mutableListOf<Any?>().apply {
                add("${command.get().commandString}")
                add("--cache-builds")
                add("--platform")
                platforms.get().forEach {
                    add(it.platformString)
                }
                if (useXCFramework.get()) {
                    add("--use-xcframeworks")
                }
            })
        }
    }
}
