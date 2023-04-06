@file:Suppress("DataClassPrivateConstructor")

package com.baizey.npmupdater.dependency

const val latestString = "latest"

data class DependencyVersion private constructor(
        val deprecatedMessage: String?,
        val type: String,
        val major: String,
        val minor: String,
        val patch: String,
        val preRelease: String) {
    val version =
            if (this.major == latestString)
                major
            else {
                val separator = if (preRelease.isBlank()) "" else "-"
                "$major.$minor.$patch$separator$preRelease"
            }
    val isLatest = this.major == latestString
    val isDeprecated = deprecatedMessage != null

    companion object {
        private val latest = DependencyVersion(deprecatedMessage = null, type = "", major = latestString, minor = "", patch = "", preRelease = "")

        private val versionRegex = """(?<type>\^|~|>|<|>=|<=)?(?<major>\d+)\.?(?<minor>\d+)?\.?(?<patch>\d+)?-?(?<preRelease>\S+)?"""
                .toRegex()

        fun of(version: String, deprecatedMessage: String?): DependencyVersion {
            val match = versionRegex.find(version) ?: return latest
            val versionType = match.groups["type"]?.value ?: ""
            val majorVersion = match.groups["major"]?.value ?: "0"
            val minorVersion = match.groups["minor"]?.value ?: "0"
            val patchVersion = match.groups["patch"]?.value ?: "0"
            val preReleaseVersion = match.groups["preRelease"]?.value ?: ""
            return DependencyVersion(deprecatedMessage, versionType, majorVersion, minorVersion, patchVersion, preReleaseVersion)
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

