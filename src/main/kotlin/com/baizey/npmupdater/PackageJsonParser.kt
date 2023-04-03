package com.baizey.npmupdater

class PackageJsonParser {
    private val dependencyRegex = """^\s*"(?<package>\S+)"\s*:\s*"(?<version>\S+)".*$""".toRegex()
    fun findDependencies(content: String): List<PackageJsonDependency> {
        val dependencies = mutableListOf<PackageJsonDependency>()
        var charCount = 0
        var isInDependencyScope = false
        content.lines().forEach {
            charCount += it.length + 1
            val line = it.trim()

            if (line.contains("}"))
                isInDependencyScope = false
            else if (line.contains("\"dependencies\":") || line.contains("\"devDependencies\""))
                isInDependencyScope = true

            if (isInDependencyScope) {
                val match = dependencyRegex.find(line)
                if (match != null) {
                    val version = match.groups["version"]?.value ?: ""
                    val packageName = match.groups["package"]?.value ?: ""
                    dependencies.add(PackageJsonDependency(packageName, charCount - 4, DependencyVersion.of(version, null)))
                }
            }
        }
        return dependencies
    }
}
