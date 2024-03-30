package com.baizey.npmupdater.packagemanager

import com.baizey.npmupdater.dto.DependencyCollection
import com.baizey.npmupdater.dto.NpmSemanticVersion
import com.baizey.npmupdater.packagejson.PackageJsonDependency
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.concurrentMapOf
import java.io.IOException
import java.util.concurrent.Executors
import kotlin.math.roundToLong

class DependencyService(
    project: Project,
    private val packageManager: PackageManager = PackageManager.get(project)
) {
    companion object {
        private val thread = Executors.newSingleThreadExecutor()
        private val instances = concurrentMapOf<String?, DependencyService>()
        fun getInstance(project: Project) = instances.computeIfAbsent(project.basePath) { DependencyService(project) }
    }

    class Cache<T> {
        private val internalCache = concurrentMapOf<String, CacheItem<T>>()
        fun getOrAdd(key: String, supplier: () -> T): T {
            val item = internalCache.computeIfAbsent(key) { CacheItem(supplier(), System.nanoTime()) }

            if (item.isExpired()) {
                item.renewLifetime()
                thread.submit { internalCache.replace(key, CacheItem(supplier(), System.nanoTime())) }
            }

            return item.value
        }

        private data class CacheItem<T>(val value: T, private val timestamp: Long) {
            companion object {
                private val second: Long = 1e9.roundToLong()
                private val lifetime: Long = 60L * second
            }

            private var expireBy: Long = timestamp + lifetime

            fun renewLifetime() {
                expireBy = System.nanoTime() + lifetime
            }

            fun isExpired() = System.nanoTime() > expireBy
        }
    }

    private val cache = Cache<NpmVersions>()

    fun getVersionInfo(packageJson: PackageJsonDependency): DependencyCollection {
        val name = packageJson.name
        val info = cache.getOrAdd(name) { fetchPackageInfo(packageJson) }

        val packageVersion = packageJson.current
        val matchedVersion =
            if (packageJson.current.isWildcardType) info.latest
            else info.versions.firstOrNull { it == packageVersion }
        val current = matchedVersion ?: NpmSemanticVersion("", packageVersion.major, packageVersion.minor, packageVersion.patch, "fake")

        val bumpMajor = info.latest
        var bumpMinor = NpmSemanticVersion("", 0, 0, 0, "fake")
        var bumpPatch = NpmSemanticVersion("", 0, 0, 0, "fake")
        info.versions.filter { it.label.isNullOrBlank() }.forEach {
            if (it.major != current.major) return@forEach
            if (it.minor == current.minor) {
                if (it > bumpPatch) bumpPatch = it
            } else if (it.minor > current.minor) {
                if (it > bumpMinor) bumpMinor = it
            }
        }

        return DependencyCollection(
            name = name,
            index = packageJson.index,
            current = current,
            matched = matchedVersion,
            latest = info.latest.withType(packageJson.current.type),
            major = if (bumpMajor.major <= current.major) null else bumpMajor.withType(packageJson.current.type),
            minor = if (bumpMinor.minor <= current.minor) null else bumpMinor.withType(packageJson.current.type),
            patch = if (bumpPatch.patch <= current.patch) null else bumpPatch.withType(packageJson.current.type),
            versions = info.versions
        )
    }

    data class NpmVersions(val latest: NpmSemanticVersion, val versions: List<NpmSemanticVersion>)

    private fun fetchPackageInfo(dependency: PackageJsonDependency): NpmVersions {
        return try {
            val response = packageManager.fetch(dependency.name)

            val versions = response.versions.values.map { NpmSemanticVersion.of(it.version, it.deprecated) }

            val latestVersion = NpmSemanticVersion.of(response.distTags.latest.trim(), null)
            val latest = versions.first { it == latestVersion }

            NpmVersions(latest, versions)
        } catch (e: IOException) {
            NpmVersions(dependency.current, listOf())
        }
    }
}