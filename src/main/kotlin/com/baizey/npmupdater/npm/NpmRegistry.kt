package com.baizey.npmupdater.npm

import com.baizey.npmupdater.utils.OperativeSystem
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class NpmRegistry(project: Project) {
    private val fileRoot = File(project.basePath!!)
    private val virtualRoot = LocalFileSystem.getInstance().findFileByIoFile(fileRoot)
    val url = detectRegistry()

    private fun detectRegistry(): String {
        var registry =
                if (isUsingYarn()) yarn("config get npmRegistryServer")
                else if (isUsingNpm()) npm("config get registry")
                else throw RuntimeException("Both npm and yarn are missing from the system")
        if (!registry.endsWith("/")) registry += "/"
        return registry
    }

    private fun isUsingYarn() = try {
        val hasYarn = !yarn("--version").startsWith("1.")
        if (hasYarn)
            virtualRoot?.children?.map { it.name }?.contains(".yarn") ?: false
        else false
    } catch (e: Exception) {
        false
    }

    private fun isUsingNpm() = try {
        npm("--version").isNotEmpty()
    } catch (e: Exception) {
        false
    }

    private fun npm(cmd: String) = cmd("${if (OperativeSystem.isWindows) "npm.cmd" else "npm"} $cmd")
    private fun yarn(cmd: String) = cmd("${if (OperativeSystem.isWindows) "yarn.cmd" else "yarn"} $cmd")
    private fun cmd(cmd: String): String {
        val command = Runtime.getRuntime().exec(cmd, null, fileRoot)
        val reader = BufferedReader(InputStreamReader(command.inputStream))
        return reader.useLines { it.joinToString(separator = "\n") }.trim()
    }
}