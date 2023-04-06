package com.baizey.npmupdater.utils

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object CommandLine {
    fun run(cmd: String, root: File): String {
        val command = Runtime.getRuntime().exec(cmd, null, root)
        val reader = BufferedReader(InputStreamReader(command.inputStream))
        return reader.useLines { it.joinToString(separator = "\n") }.trim()
    }
}