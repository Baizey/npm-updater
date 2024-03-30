package com.baizey.npmupdater.dto

data class DependencyCollection(
    val name: String,
    var index: Int,
    val current: NpmSemanticVersion,
    val matched: NpmSemanticVersion?,
    val latest: NpmSemanticVersion,
    val major: NpmSemanticVersion?,
    val minor: NpmSemanticVersion?,
    val patch: NpmSemanticVersion?,
    val versions: List<NpmSemanticVersion>,
)