package com.baizey.npmupdater.npm

import com.baizey.npmupdater.Dependency
import com.baizey.npmupdater.DependencyVersion
import com.baizey.npmupdater.PackageJsonDependency
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.concurrentMapOf
import java.io.IOException
import java.net.URI
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.roundToLong

class NpmService(
        project: Project,
        private val httpClient: HttpCaller = HttpCaller(),
        private val registry: NpmRegistry = NpmRegistry(project)
) {
    companion object {
        private val thread = Executors.newSingleThreadExecutor()
        private val instances = concurrentMapOf<String?, NpmService>()
        fun getInstance(project: Project) = instances.computeIfAbsent(project.basePath) { NpmService(project) }
    }

    private data class DistTags(@get:JsonProperty("latest") var latest: String)
    private data class Version(
            @get:JsonProperty("version") val version: String,
            @get:JsonProperty("deprecated") val deprecated: String?
    )

    private data class Response(
            @get:JsonProperty("dist-tags") val distTags: DistTags,
            @get:JsonProperty("versions") val versions: Map<String, Version>
    )

    private data class CacheItem<T>(val value: T, private val timestamp: Long = System.nanoTime()) {
        companion object {
            private val second: Long = 1e9.roundToLong()
            private val tenSeconds: Long = 10L * second
            private val thirtySeconds: Long = 30L * second
        }

        private var expireBy: Long = timestamp + thirtySeconds

        fun extendExpiration() {
            expireBy += tenSeconds
        }

        fun isExpired() = System.nanoTime() > expireBy
    }

    private val cache = concurrentMapOf<String, CacheItem<Dependency>>()
    private val mapper = ObjectMapper().registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)

    fun getLatestVersion(dependency: PackageJsonDependency): Dependency {
        val temp = cache.getOrDefault(dependency.name, null)
        if (temp != null && temp.isExpired()) {
            temp.extendExpiration()
            thread.submit { cache.replace(dependency.name, fetchPackageInfo(dependency)) }
        }
        val result = cache.computeIfAbsent(dependency.name) { fetchPackageInfo(dependency) }.value
        result.index = dependency.index
        return result
    }

    private fun fetchPackageInfo(dependency: PackageJsonDependency): CacheItem<Dependency> {
        val key = dependency.name
        return try {
            val jsonResponse = httpClient.get(URI(registry.url + key))
            val response = mapper.readValue<Response>(jsonResponse)

            val versions = response.versions.values.map { DependencyVersion.of(it.version, it.deprecated) }

            val latestMatch = DependencyVersion.of(response.distTags.latest.trim(), null)
            val latest = versions.first { it == latestMatch }

            val currentMatch = versions.firstOrNull { it == dependency.current }
            val current =
                    if (dependency.current.isLatest) latest
                    else DependencyVersion.of(dependency.current.versionWithType(), currentMatch?.deprecatedMessage)

            CacheItem(Dependency(key, dependency.index, current, latest, versions))
        } catch (e: IOException) {
            CacheItem(Dependency(key, dependency.index, dependency.current, dependency.current, listOf()))
        }
    }
}