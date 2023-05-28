package com.baizey.npmupdater.packagemanager

import com.baizey.npmupdater.utils.CommandLine
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

interface PackageManager {
    val cli: String
    val root: File
    val url: String
    val token: String
    val mapper: ObjectMapper
    val client: Http

    fun cmd(cmd: String) = CommandLine.run("$cli $cmd", root)

    fun fetch(packageName: String): Response {
        val uri = URI("$url${if (url.endsWith("/")) "" else "/"}$packageName")
        val response = client.get(uri, token)
        return mapper.readValue<Response>(response)
    }

    companion object {
        fun get(project: Project): PackageManager {
            val root = File(project.basePath!!)
            val virtualRoot = LocalFileSystem.getInstance().findFileByIoFile(root)

            val isUsingYarn = try {
                val yarn = YarnPackageManager(root)
                val hasYarn = !yarn.cmd("--version").startsWith("1.")
                if (hasYarn)
                    virtualRoot?.children?.map { it.name }?.contains(".yarn") ?: false
                else false
            } catch (e: Exception) {
                false
            }

            return if (isUsingYarn) YarnPackageManager(root) else NpmPackageManager(root)
        }
    }
}

class Http(private val client: HttpClient = HttpClient.newBuilder().build()) {
    fun get(uri: URI, token: String?): String {
        val request = HttpRequest.newBuilder().uri(uri)
        if (token != null) request.header("Authorization", "Bearer $token")
        val response = client.send(request.build(), HttpResponse.BodyHandlers.ofString())
        return response.body()
    }
}

data class DistTags(@get:JsonProperty("latest") var latest: String)
data class Version(
        @get:JsonProperty("version") val version: String,
        @get:JsonProperty("deprecated") val deprecated: String?
)

data class Response(
        @get:JsonProperty("dist-tags") val distTags: DistTags,
        @get:JsonProperty("versions") val versions: Map<String, Version>
)