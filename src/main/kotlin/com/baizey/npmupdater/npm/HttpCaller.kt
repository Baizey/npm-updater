package com.baizey.npmupdater.npm

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class HttpCaller(private val client: HttpClient = HttpClient.newBuilder().build()) {
    fun get(uri: URI): String {
        val request = HttpRequest.newBuilder().uri(uri).build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }
}