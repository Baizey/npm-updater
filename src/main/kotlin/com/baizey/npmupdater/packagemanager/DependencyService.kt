package com.baizey.npmupdater.packagemanager

import com.baizey.npmupdater.dependency.Dependency
import com.baizey.npmupdater.dependency.DependencyVersion
import com.baizey.npmupdater.dependency.PackageJsonDependency
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

    private data class CacheItem<T>(val value: T, private val timestamp: Long = System.nanoTime()) {
        companion object {
            private val second: Long = 1e9.roundToLong()
            private val tenSeconds: Long = 10L * second
            private val minute: Long = 60 * second
        }

        private var expireBy: Long = timestamp + minute

        fun extendExpiration() {
            expireBy += tenSeconds
        }

        fun isExpired() = System.nanoTime() > expireBy
    }

    private val cache = concurrentMapOf<String, CacheItem<NpmVersions>>()

    fun getLatestVersion(packageJsonDependency: PackageJsonDependency): Dependency {
        val name = packageJsonDependency.name
        val temp = cache.getOrDefault(name, null)
        if (temp != null && temp.isExpired()) {
            temp.extendExpiration()
            thread.submit { cache.replace(name, fetchPackageInfo(packageJsonDependency)) }
        }
        val cacheItem = cache.computeIfAbsent(name) { fetchPackageInfo(packageJsonDependency) }.value

        val currentMatch = cacheItem.versions
                .firstOrNull { it == packageJsonDependency.current }
                ?: cacheItem.latest

        val current = DependencyVersion.of(
                if (packageJsonDependency.current.isLatest) cacheItem.latest.versionWithType(packageJsonDependency.current.type)
                else packageJsonDependency.current.versionWithType(),
                currentMatch.deprecatedMessage)

        val bumpMinor = cacheItem.versions
                .filter { current.major == it.major }
                .groupBy { it.minor }
                .maxByOrNull { it.key.toIntOrNull() ?: -1 }?.value
                ?.filter { it.preRelease.isBlank() }
                ?.maxByOrNull { it.patch.toIntOrNull() ?: -1 }
                ?.let { if (it.minor == current.minor) null else it }

        val bumpPatch = cacheItem.versions
                .filter { it.major == current.major && it.minor == current.minor }
                .filter { it.preRelease.isBlank() }
                .maxByOrNull { it.patch.toIntOrNull() ?: -1 }
                ?.let { if (it.patch == current.patch) null else it }

        val bumpMajor = if(cacheItem.latest.major != current.major) cacheItem.latest else null

        return Dependency(
                name = name,
                index = packageJsonDependency.index,
                current = current,
                latest = cacheItem.latest,
                major = bumpMajor,
                minor = bumpMinor,
                patch = bumpPatch,
                versions = cacheItem.versions)
    }

    data class NpmVersions(val latest: DependencyVersion, val versions: List<DependencyVersion>)

    private fun fetchPackageInfo(dependency: PackageJsonDependency): CacheItem<NpmVersions> {
        return try {
            val response = packageManager.fetch(dependency.name)

            val versions = response.versions.values.map { DependencyVersion.of(it.version, it.deprecated) }

            val latestVersion = DependencyVersion.of(response.distTags.latest.trim(), null)
            val latest = versions.first { it == latestVersion }

            CacheItem(NpmVersions(latest, versions))
        } catch (e: IOException) {
            CacheItem(NpmVersions(dependency.current, listOf()))
        }
    }
}