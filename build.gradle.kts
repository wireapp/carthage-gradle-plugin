plugins {
    kotlin("jvm") version "1.4.30"
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

group = PluginCoordinates.GROUP
version = PluginCoordinates.VERSION

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlin:kotlin-native-utils:1.6.10")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10")
    api("com.googlecode.plist:dd-plist:1.23")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()

    val runCarthage = project.tasks.register("run carthage", Exec::class.java) {
        commandLine("carthage", "bootstrap", "--platform", "ios", "--use-xcframeworks")
        inputs.file(project.projectDir.resolve("Cartfile"))
        outputs.dir(project.projectDir.resolve("Carthage"))
    }

    this.dependsOn(runCarthage)
}

gradlePlugin {
    plugins {
        create(PluginCoordinates.ID) {
            id = PluginCoordinates.ID
            implementationClass = PluginCoordinates.IMPLEMENTATION_CLASS
            version = PluginCoordinates.VERSION
        }
    }
}

object PluginCoordinates {

    const val GROUP = "com.wire"
    const val ARTIFACT = "carthage-gradle-plugin"
    const val VERSION = "0.0.1"

    const val ID = "$GROUP.$ARTIFACT"
    const val IMPLEMENTATION_CLASS = "com.wire.plugin.CarthagePlugin"
}

publishing {
    repositories {
        maven {
            url = uri("../wire-maven/releases")
        }
    }
}
