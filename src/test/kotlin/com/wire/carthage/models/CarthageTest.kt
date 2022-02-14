package com.wire.carthage.models

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File

internal class CarthageTest {

    @Test
    fun `test that products found in carthage build directory as classified correctly in BuildProducts`() {

        // GIVEN
        val resourceFolder = File("src/test/resources")

        val xcFrameworkPaths = listOf(
            resourceFolder.resolve(Carthage.BUILD_DIR).resolve("AFNetworking.xcframework"),
            resourceFolder.resolve(Carthage.BUILD_DIR).resolve("WireUtilities.xcframework"),
            resourceFolder.resolve(Carthage.BUILD_DIR).resolve("WireCryptobox.xcframework"),
        ).map { it.path }

        val frameworkPaths = listOf(
            resourceFolder
                .resolve(Carthage.BUILD_DIR)
                .resolve(Carthage.Platform.IOS.platformString)
                .resolve("WireSystem.framework").path
        )

        val knownBuildProducts = Carthage.BuildProducts(
            xcFrameworkPaths,
            hashMapOf(
                Pair(Carthage.Platform.MACOS, listOf()),
                Pair(Carthage.Platform.WATCHOS, listOf()),
                Pair(Carthage.Platform.IOS, frameworkPaths),
                Pair(Carthage.Platform.TVOS, listOf())
            )
        )

        // WHEN
        val buildProducts = Carthage.buildProducts(resourceFolder)

        // THEN
        assertEquals(
            knownBuildProducts,
            buildProducts
        )
    }

    @Test
    fun `test that parsing an xcframework from a valid plist file succeeds`() {
        // GIVEN
        val plist = File("src/test/resources")
            .resolve(Carthage.BUILD_DIR)
            .resolve("AFNetworking.xcframework")
            .resolve("Info.plist")

        val libraries = listOf(
            Carthage.XCFramework.Library(
                identifier = "ios-arm64_i386_x86_64-simulator",
                path = "AFNetworking.framework",
                supportedPlatform = "ios",
                supportedPlatformVariant = "simulator",
                supportedArchitectures = listOf("arm64", "i386", "x86_64"),
                debugSymbolsPath = "dSYMs"
            ),
            Carthage.XCFramework.Library(
                identifier = "ios-arm64_armv7",
                path = "AFNetworking.framework",
                supportedPlatform = "ios",
                supportedArchitectures = listOf("arm64", "armv7"),
                debugSymbolsPath = "dSYMs",
                bitcodeSymbolMapsPath = "BCSymbolMaps"
            )
        )

        val knownXCFramework = Carthage.XCFramework(
            availableLibraries = libraries,
            packageType = "XFWK",
            formatVersion = "1.0"
        )

        // WHEN
        val xcframeworkFromPlist = Carthage.xcFrameworkFromPlist(plist)

        // THEN
        assertNotNull(xcframeworkFromPlist)

        xcframeworkFromPlist?.let { xcFramework ->
            assertEquals(knownXCFramework.formatVersion, xcFramework.formatVersion)
            assertEquals(knownXCFramework.packageType, xcFramework.packageType)

            knownXCFramework.availableLibraries.zip(xcFramework.availableLibraries).forEach{ pair ->
                val (lhs, rhs) = pair
                assertEquals(lhs.identifier, rhs.identifier)
                assertEquals(lhs.path, rhs.path)
                assertEquals(lhs.supportedPlatform, rhs.supportedPlatform)
                lhs.supportedArchitectures.zip(rhs.supportedArchitectures).forEach {
                    assertEquals(it.first, it.second)
                }
                assertEquals(lhs.supportedPlatformVariant, rhs.supportedPlatformVariant)
                assertEquals(lhs.debugSymbolsPath, rhs.debugSymbolsPath)
                assertEquals(lhs.bitcodeSymbolMapsPath, rhs.bitcodeSymbolMapsPath)
            }
        }
    }

    @Test
    fun `test that retrieving an existing framework succeeds`() {
        // GIVEN
        val resourceFolder = File("src/test/resources")

        val knownValidFramework = resourceFolder
            .resolve(Carthage.BUILD_DIR)
            .resolve("iOS")
            .resolve("WireSystem.framework")

        val buildProducts = Carthage.buildProducts(resourceFolder)

        // WHEN
        val framework = Carthage.framework(
            "WireSystem",
            forPlatform = Carthage.Platform.IOS,
            inBuildProducts = buildProducts
        )

        // THEN
        assertEquals(knownValidFramework, framework)
    }
}
