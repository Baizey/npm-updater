package com.baizey.npmupdater.utils

import java.util.*

object OperativeSystem {
    val isWindows = System
            .getProperty("os.name")
            .lowercase(Locale.getDefault())
            .contains("win")
}