package com.tripbudget.app.cloud

import java.time.Instant

object CloudTime {
    fun toIso(millis: Long): String = Instant.ofEpochMilli(millis).toString()

    fun fromIso(value: String): Long =
        runCatching { Instant.parse(value).toEpochMilli() }
            .getOrElse { System.currentTimeMillis() }
}
