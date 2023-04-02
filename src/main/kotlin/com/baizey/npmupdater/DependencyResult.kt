@file:Suppress("DataClassPrivateConstructor")

package com.baizey.npmupdater

import java.lang.RuntimeException

data class DependencyVersion private constructor(
        val type: String,
        val major: String,
        val minor: String,
        val patch: String,
        val preRelease: String) {
    val version: String

    init {
        val separator = if (preRelease.isBlank()) "" else "-"
        version = "$major.$minor.$patch$separator$preRelease"
    }

    companion object {
        private val versionRegex = """(?<type>\^|~|>|<|>=|<=)?(?<major>\d+)\.?(?<minor>\d+)?\.?(?<patch>\d+)?-?(?<preRelease>\S+)?"""
                .toRegex()

        fun of(version: String): DependencyVersion {
            val match = versionRegex.find(version)
                    ?: throw RuntimeException("Could not resolve version from given string `$version`")
            val versionType = match.groups["type"]?.value ?: ""
            val majorVersion = match.groups["major"]?.value ?: "0"
            val minorVersion = match.groups["minor"]?.value ?: "0"
            val patchVersion = match.groups["patch"]?.value ?: "0"
            val preReleaseVersion = match.groups["preRelease"]?.value ?: ""
            return DependencyVersion(versionType, majorVersion, minorVersion, patchVersion, preReleaseVersion)
        }
    }

    fun versionWithType(type: String = this.type): String = "$type$version"

    override fun toString() = version

    override fun equals(other: Any?): Boolean =
            if (other is DependencyVersion) other.version == version else false

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + major.hashCode()
        result = 31 * result + minor.hashCode()
        result = 31 * result + patch.hashCode()
        result = 31 * result + preRelease.hashCode()
        return result
    }
}

data class DependencyName(val name: String, val current: DependencyVersion, val index: Int)

data class DependencyDescription(val json: DependencyName, val latest: DependencyVersion)

data class DependencyResult(val annotations: List<DependencyDescription>)