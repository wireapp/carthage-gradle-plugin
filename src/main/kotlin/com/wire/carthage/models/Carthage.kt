package com.wire.carthage.models

import java.io.File
import com.dd.plist.PropertyListParser
import com.dd.plist.NSArray
import com.dd.plist.NSDictionary
import com.dd.plist.NSObject

sealed class Carthage {

    data class DependencyProduct(val name: String)

    class XCFramework(
        val availableLibraries: List<Library>,
        val packageType: String,
        val formatVersion: String)
    {
        data class Library(
            val identifier: String,
            val path: String,
            val supportedPlatform: String,
            val supportedPlatformVariant: String? = null,
            val supportedArchitectures: List<String>,
            val debugSymbolsPath: String? = null,
            val bitcodeSymbolMapsPath: String? = null
        )
    }

    data class BuildProducts(
        val xcFrameworksPaths: List<String>,
        val frameworksByPlatformPaths: HashMap<Platform, List<String>>
    )

    enum class Platform(val platformString: String) {
        IOS(platformString = "iOS"),
        MACOS(platformString = "macos"),
        TVOS(platformString = "tvOS"),
        WATCHOS(platformString = "watchOS")
    }

    companion object {

        const val ROOT_DIR = "Carthage"
        const val BUILD_DIR = "$ROOT_DIR/Build"
        const val FRAMEWORK =  "framework"
        const val XCFRAMEWORK = "xc$FRAMEWORK"

        private const val KEY_XC_AVAILABLE_LIBRARIES = "AvailableLibraries"

        private const val KEY_XC_DEBUG_SYMBOLS_PATH = "DebugSymbolsPath"
        private const val KEY_XC_LIBRARY_IDENTIFIER = "LibraryIdentifier"
        private const val KEY_XC_LIBRARY_PATH = "LibraryPath"
        private const val KEY_XC_SUPPORTED_ARCHITECTURES = "SupportedArchitectures"
        private const val KEY_XC_SUPPORTED_PLATFORM = "SupportedPlatform"
        private const val KEY_XC_SUPPORTED_PLATFORM_VARIANT = "SupportedPlatformVariant"
        private const val KEY_XC_BITCODE_SYMBOLMAPS_PATH = "BitcodeSymbolMapsPath"

        private const val KEY_XC_CF_BUNDLE_PACKAGE_TYPE = "CFBundlePackageType"
        private const val KEY_XC_XC_FRAMEWORK_FORMAT_VERSION = "XCFrameworkFormatVersion"

        @OptIn(ExperimentalStdlibApi::class)
        fun buildProducts(baseDir: File): BuildProducts {
            val allDirs = baseDir
                .resolve(BUILD_DIR)
                .walkTopDown()
                .toList()
                .filter { it.isDirectory }

            val xcFrameworkDirs = allDirs.filter { it.extension == XCFRAMEWORK }.map { it.path }

            val frameworkPerPlatform = Platform.values().map { platform ->
                val frameworks = allDirs
                    .firstOrNull { it.name.lowercase() == platform.platformString.lowercase() }
                    ?.walkTopDown()
                    ?.toList()
                    ?.filter { it.isDirectory && it.extension == FRAMEWORK }

                val paths: List<String>? = frameworks?.map { it.path }
                Pair(platform,  paths ?: listOf())
            }

            val hashMap: HashMap<Platform, List<String>> = HashMap(4)
            frameworkPerPlatform.forEach {
                hashMap[it.first] = it.second
            }
            return BuildProducts(xcFrameworkDirs, hashMap)
        }

        fun xcFrameworkFromPlist(plist: File): XCFramework? {
            val rootDictionary = PropertyListParser.parse(plist) as? NSDictionary

            val packageType = rootDictionary?.objectForKey(KEY_XC_CF_BUNDLE_PACKAGE_TYPE)?.toString()
            val formatVersion = rootDictionary?.objectForKey(KEY_XC_XC_FRAMEWORK_FORMAT_VERSION)?.toString()

            if (packageType.isNullOrEmpty() || formatVersion.isNullOrEmpty()) {
                return null
            }

            val librariesNSObjects: List<NSObject> = (
                    rootDictionary?.objectForKey(KEY_XC_AVAILABLE_LIBRARIES) as? NSArray
            )?.array?.toList()
                ?: listOf()

            val availableLibraries: List<XCFramework.Library> = librariesNSObjects.mapNotNull { libraryObject ->
                val nsDictionary = libraryObject as? NSDictionary

                val debugSymbolsPath = nsDictionary?.objectForKey(KEY_XC_DEBUG_SYMBOLS_PATH)?.toString()
                val bitcodeSymbolMapsPath = nsDictionary?.objectForKey(KEY_XC_BITCODE_SYMBOLMAPS_PATH)?.toString()
                val libraryIdentifier = nsDictionary?.objectForKey(KEY_XC_LIBRARY_IDENTIFIER)?.toString()
                val libraryPath = nsDictionary?.objectForKey(KEY_XC_LIBRARY_PATH)?.toString()
                val supportedArchitectures = (
                        nsDictionary?.objectForKey(KEY_XC_SUPPORTED_ARCHITECTURES) as? NSArray
                )
                    ?.array
                    ?.toList()
                    ?.map { it.toString() }
                    ?.map { if (it == "x86_64") "x64" else it }

                val supportedPlatform = nsDictionary?.objectForKey(KEY_XC_SUPPORTED_PLATFORM)?.toString()
                val supportedPlatformVariant = nsDictionary?.objectForKey(KEY_XC_SUPPORTED_PLATFORM_VARIANT)?.toString()

                if (
                    libraryIdentifier.isNullOrBlank()
                    || libraryPath.isNullOrBlank()
                    || supportedArchitectures.isNullOrEmpty()
                    || supportedPlatform.isNullOrEmpty()
                ) {
                    return null
                }
                else {
                     XCFramework.Library(
                        identifier = libraryIdentifier!!,
                        path = libraryPath!!,
                        supportedPlatform = supportedPlatform!!,
                        supportedPlatformVariant = supportedPlatformVariant,
                        supportedArchitectures = supportedArchitectures!!,
                        debugSymbolsPath = debugSymbolsPath,
                        bitcodeSymbolMapsPath = bitcodeSymbolMapsPath
                    )
                }
            }

            return XCFramework(
                availableLibraries= availableLibraries,
                packageType = packageType!!,
                formatVersion = formatVersion!!
            )
        }

        /**
         * @param named The name of the xcframework **without** the extension
         * @param inBuildProducts the build products in `Carthage/Build`
        */
        fun xcFramework(named: String, inBuildProducts: BuildProducts): Pair<File, XCFramework>? {
            val xcFramework = inBuildProducts
                .xcFrameworksPaths
                .map{ File(it) }
                .firstOrNull { it.nameWithoutExtension == named }
            val plist = xcFramework
                ?.resolve("Info.plist")

            return plist?.let{ plistFile ->
                xcFrameworkFromPlist(plistFile)?.let {
                    Pair(xcFramework, it)
                }
            }
        }

        /**
         * @param inBuildProducts the build products in `Carthage/Build`
         */
        fun xcFrameworks(inBuildProducts: BuildProducts): List<Pair<File, XCFramework>> {
            return inBuildProducts
                .xcFrameworksPaths
                .map { File(it) }
                .map { Pair(it, xcFrameworkFromPlist(it.resolve("Info.plist"))!!) }
        }

        /**
         * @param named The name of the framework **without** the extension
         * @param forPlatform The `Carthage.Platform` to restrict the search to
         * @param inBuildProducts the build products in `Carthage/Build`
         */
        fun framework(named: String, forPlatform: Carthage.Platform, inBuildProducts: BuildProducts): File? {
            return inBuildProducts
                .frameworksByPlatformPaths[forPlatform]
                ?.map{ File(it) }
                ?.firstOrNull { it.nameWithoutExtension == named }
        }
    }
}
