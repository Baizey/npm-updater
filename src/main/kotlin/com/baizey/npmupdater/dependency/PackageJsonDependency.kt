package com.baizey.npmupdater.dependency

data class PackageJsonDependency(val name: String,
                                 val index: Int,
                                 val current: DependencyVersion)