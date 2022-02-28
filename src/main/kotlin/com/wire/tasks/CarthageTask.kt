// Wire
// Copyright (C) 2022 Wire Swiss GmbH
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see http://www.gnu.org/licenses/.

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
