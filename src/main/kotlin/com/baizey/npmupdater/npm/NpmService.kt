package com.baizey.npmupdater.npm

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
    private data class Response(@get:JsonProperty("dist-tags") val distTags: DistTags)
    private data class CacheItem(val value: String, private val timestamp: Long = System.nanoTime()) {
        fun isExpired() = System.nanoTime() > (timestamp + 30 * 1e9)
    }

    private val cache = concurrentMapOf<String, CacheItem>()
    private val mapper = ObjectMapper()
            .registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    fun getLatestVersion(packageName: String): String {
        val result = cache.getOrDefault(packageName, null)
        if (result != null && result.isExpired()) cache.remove(packageName)
        return cache.computeIfAbsent(packageName) {
            try {
                val jsonResponse = httpClient.get(URI(registry.url + it))
                val response = mapper.readValue<Response>(jsonResponse)
                CacheItem(response.distTags.latest.trim())
            } catch (e: IOException) {
                CacheItem("")
            }
        }.value
    }
}

