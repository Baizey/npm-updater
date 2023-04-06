package com.baizey.npmupdater.packagemanager

import com.baizey.npmupdater.utils.OperativeSystem
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

class NpmPackageManager(
        override val root: File,
        override val cli: String = if (OperativeSystem.isWindows) "npm.cmd" else "npm",
        override val mapper: ObjectMapper = ObjectMapper().registerKotlinModule()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false),
        override val client: Http = Http()
) : PackageManager {
    override val token = cmd("config get authToken")
    override val url = cmd("config get registry")
}