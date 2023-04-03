package com.baizey.npmupdater

import com.baizey.npmupdater.npm.NpmService
import com.intellij.openapi.project.Project

class PackageJsonParser(project: Project,
                        private val npm: NpmService = NpmService(project)) {

    private val dependencyRegex = """^\s*"(?<package>\S+)"\s*:\s*"(?<version>\S+)".*$""".toRegex()

    fun findDependenciesWithUpdatesPossible(content: String): List<DependencyDescription> {
        val dependencies = getDependencies(content)
        val outOfDateDependencies = dependencies
                .parallelStream()
                .map {
                    try {
                        val registry = npm.getLatestVersion(it.name)
                        DependencyDescription(it, registry)
                    } catch (e: Exception) {
                        null
                    }
                }
                .toList()
                .filterNotNull()
                .filter { it.registry.latest != it.json.current }
        return outOfDateDependencies
    }

    private fun getDependencies(content: String): List<PackageJsonDependency> {
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
                    dependencies.add(PackageJsonDependency(packageName, DependencyVersion.of(version, null), charCount - 4))
                }
            }
        }
        return dependencies
    }
}
