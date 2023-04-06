package com.baizey.npmupdater.dependency

data class Dependency(val name: String,
                      var index: Int,
                      val current: DependencyVersion,
                      val latest: DependencyVersion,
                      val major: DependencyVersion?,
                      val minor: DependencyVersion?,
                      val patch: DependencyVersion?,
                      val versions: List<DependencyVersion>) {

    fun semanticVersionBehind(): SemVer {
        if (current.major != latest.major) return SemVer.MAJOR
        if (current.minor != latest.minor) return SemVer.MINOR
        if (current.patch != latest.patch) return SemVer.PATCH
        if (current.preRelease != latest.preRelease) return SemVer.PRE_RELEASE
        return SemVer.NONE
    }
}