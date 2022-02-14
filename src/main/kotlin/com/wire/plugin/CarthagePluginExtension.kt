package com.wire.plugin
import org.gradle.api.Project
import javax.inject.Inject
import com.wire.carthage.models.Carthage
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

abstract class CarthagePluginExtension @Inject constructor(val project: Project) {

    private var _dependencies = project.container(CarthageDependency::class.java)

    val command: CarthageCommand = CarthageCommand.UPDATE
    val platforms: List<Carthage.Platform> = listOf(Carthage.Platform.IOS)
    var baseName: String = "Carthage"

    val dependencies: NamedDomainObjectSet<CarthageDependency>
        get() = _dependencies

    fun dependency(name: String) {
        _dependencies.add(CarthageDependency(name))
    }
}

enum class CarthageCommand(val commandString: String) {
    BOOTSTRAP("bootstrap"),
    UPDATE("update")
}

data class CarthageDependency(
    @get:Input val moduleName: String,
    @get:Internal var packageName: String = "carthage.$moduleName"
) : Named {

    @Input
    override fun getName(): String = moduleName
}
