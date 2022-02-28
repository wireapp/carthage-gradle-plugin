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

package com.wire.carthage.models

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File

internal class CarthageTest {

    @Test
    fun `test that products found in carthage build directory as classified correctly in BuildProducts`() {

        // GIVEN
        val resourceFolder =  File(".")

        val xcFrameworkPaths = listOf(
            resourceFolder.resolve(Carthage.BUILD_DIR).resolve("AFNetworking.xcframework"),
            resourceFolder.resolve(Carthage.BUILD_DIR).resolve("WireUtilities.xcframework"),
            resourceFolder.resolve(Carthage.BUILD_DIR).resolve("WireCryptobox.xcframework"),
            resourceFolder.resolve(Carthage.BUILD_DIR).resolve("WireSystem.xcframework"),
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
                Pair(Carthage.Platform.IOS, listOf()),
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
        val plist = File(".")
            .resolve(Carthage.BUILD_DIR)
            .resolve("AFNetworking.xcframework")
            .resolve("Info.plist")

        val libraries = listOf(
            Carthage.XCFramework.Library(
                identifier = "ios-arm64_i386_x86_64-simulator",
                path = "AFNetworking.framework",
                supportedPlatform = "ios",
                supportedPlatformVariant = "simulator",
                supportedArchitectures = listOf("arm64", "i386", "x64"),
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
        val resourceFolder = File(".")

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
