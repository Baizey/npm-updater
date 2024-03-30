package com.baizey.npmupdater.packagejson

import com.baizey.npmupdater.dto.NpmSemanticVersion

data class PackageJsonDependency(
    val name: String,
    val index: Int,
    val current: NpmSemanticVersion
)