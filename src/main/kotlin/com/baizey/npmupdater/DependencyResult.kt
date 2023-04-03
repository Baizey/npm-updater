@file:Suppress("DataClassPrivateConstructor")

package com.baizey.npmupdater

data class DependencyVersion private constructor(
        val deprecatedMessage: String?,
        val type: String,
        val major: String,
        val minor: String,
        val patch: String,
        val preRelease: String) {
    val version: String
    val isDeprecated get() = deprecatedMessage != null
    val isLatest get() = this.type == latest.type

    init {
        val separator = if (preRelease.isBlank()) "" else "-"
        version = "$major.$minor.$patch$separator$preRelease"
    }

    companion object {
        private val latest = DependencyVersion(null, "latest", "", "", "", "")

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

data class PackageJsonDependency(val name: String,
                                 val index: Int,
                                 val current: DependencyVersion)

data class Dependency(val name: String,
                      val index: Int,
                      val current: DependencyVersion,
                      val latest: DependencyVersion,
                      val versions: List<DependencyVersion>)