package com.baizey.npmupdater.dto

class NpmSemanticVersion(
    val type: String,
    val major: Int,
    val minor: Int,
    val patch: Int,
    val label: String? = null,
    val deprecatedMessage: String? = null
) : Comparable<NpmSemanticVersion> {
    val isDeprecated = deprecatedMessage != null
    val isWildcardType = type in listOf(LATEST_MAJOR, ANY, URL, UNKNOWN)
    val fullName =
        if (type == URL) label!!
        else if (type == UNKNOWN) label!!
        else if (isWildcardType) type
        else if (label.isNullOrEmpty()) "$type$major.$minor.$patch"
        else "$type$major.$minor.$patch-$label"

    override fun toString() = fullName

    override fun compareTo(other: NpmSemanticVersion): Int {
        if (major != other.major) return major - other.major
        if (minor != other.minor) return minor - other.minor
        if (patch != other.patch) return patch - other.patch

        // versions with labels are considered "pre-release" so those without are higher versioned
        if (label == null) return if (other.label == null) 0 else 1
        if (other.label == null) return -1

        // This is technically wrong, but we just want consistency with labels, see: https://semver.org/
        return label.compareTo(other.label)
    }

    override fun equals(other: Any?) = if (other !is NpmSemanticVersion) false else compareTo(other) == 0
    override fun hashCode(): Int {
        var result = major
        result = 31 * result + minor
        result = 31 * result + patch
        result = 31 * result + (label?.hashCode() ?: 0)
        result = 31 * result + fullName.hashCode()
        return result
    }

    fun withType(type: String): NpmSemanticVersion =
        NpmSemanticVersion(type, major, minor, patch, label, deprecatedMessage)

    companion object {
        @Suppress("unused")
        const val EXACT = ""
        const val URL = "url"
        const val UNKNOWN = "unknown"
        const val LATEST_MAJOR = "latest"
        @Suppress("unused")
        const val LATEST_MINOR = "^"
        @Suppress("unused")
        const val LATEST_PATCH = "~"
        @Suppress("unused")
        const val GREATER_THAN = ">"
        @Suppress("unused")
        const val LESS_THAN = "<"
        @Suppress("unused")
        const val GREATER_OR_EQUAL_THAN = ">="
        @Suppress("unused")
        const val LESS_OR_EQUAL_THAN = "<="
        const val ANY = "*"
        fun of(versionString: String, deprecatedMessage: String?): NpmSemanticVersion {
            // do not touch urls
            if (versionString.startsWith("http")) {
                return NpmSemanticVersion(URL, 0, 0, 0, versionString, deprecatedMessage)
            }

            // Catch cases such as 'latest' or '*'
            val firstDigitIndex = versionString.indexOfAny("012345689".toCharArray())
            if (firstDigitIndex == -1) {
                return when (versionString) {
                    LATEST_MAJOR -> NpmSemanticVersion(LATEST_MAJOR, 0, 0, 0, null, deprecatedMessage)
                    ANY -> NpmSemanticVersion(ANY, 0, 0, 0, null, deprecatedMessage)
                    else -> NpmSemanticVersion(UNKNOWN, 0, 0, 0, versionString, deprecatedMessage)
                }
            }

            val type = versionString.substring(0, firstDigitIndex)
            val parts = versionString.substring(firstDigitIndex).split("-")
            val label = if (parts.size == 1) null else parts[1]
            val versionParts = parts[0].split(".")
            val major = versionParts[0].toInt()
            val minor = if (versionParts.size <= 1) 0 else versionParts[1].toInt()
            val patch = if (versionParts.size <= 2) 0 else versionParts[2].toInt()
            return NpmSemanticVersion(type, major, minor, patch, label, deprecatedMessage)
        }
    }
}