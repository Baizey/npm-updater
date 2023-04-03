package com.baizey.npmupdater.npm

import com.baizey.npmupdater.DependencyVersion
import com.baizey.npmupdater.NpmPackage
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

class NpmService(
        project: Project,
        private val httpClient: HttpCaller = HttpCaller(),
        private val registry: NpmRegistry = NpmRegistry(project)
) {
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
        fun isExpired() = System.nanoTime() > (timestamp + 30 * 1e9)
    }

    private val cache = concurrentMapOf<String, CacheItem<NpmPackage>>()
    private val mapper = ObjectMapper().registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)

    fun getLatestVersion(packageName: String): NpmPackage {
        val result = cache.getOrDefault(packageName, null)
        if (result != null && result.isExpired()) cache.remove(packageName)
        return cache.computeIfAbsent(packageName) { key ->
            try {
                val jsonResponse = httpClient.get(URI(registry.url + key))
                val response = mapper.readValue<Response>(jsonResponse)
                val latestRaw = response.distTags.latest.trim()
                val versions = response.versions.values.map { DependencyVersion.of(it.version, it.deprecated) }
                val latest = versions.first { it.version == latestRaw }
                CacheItem(NpmPackage(packageName, latest, versions))
            } catch (e: IOException) {
                CacheItem(NpmPackage(packageName, DependencyVersion.of("", null), listOf()))
            }
        }.value
    }
}