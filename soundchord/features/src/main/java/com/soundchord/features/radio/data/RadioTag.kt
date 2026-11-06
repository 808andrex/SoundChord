package com.soundchord.features.radio.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RadioTag(
    @SerialName("name") val name: String,
    @SerialName("stationcount") val stationCount: Int = 0,
)
