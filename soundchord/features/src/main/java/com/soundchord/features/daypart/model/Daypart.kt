package com.soundchord.features.daypart.model

import java.time.LocalTime

enum class Daypart(val key: String) {
    MORNING("morning"),       // 06:00 – 11:59
    AFTERNOON("afternoon"),   // 12:00 – 17:59
    EVENING("evening"),       // 18:00 – 21:59
    NIGHT("night");           // 22:00 – 05:59

    companion object {
        fun current(time: LocalTime = LocalTime.now()): Daypart {
            val hour = time.hour
            return when (hour) {
                in 6..11 -> MORNING
                in 12..17 -> AFTERNOON
                in 18..21 -> EVENING
                else -> NIGHT
            }
        }
    }
}
