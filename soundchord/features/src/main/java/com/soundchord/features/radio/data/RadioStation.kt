package com.soundchord.features.radio.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RadioStation(
    @SerialName("stationuuid") val uuid: String,
    @SerialName("name") val name: String,
    @SerialName("url_resolved") val streamUrl: String,
    @SerialName("homepage") val homepage: String? = null,
    @SerialName("favicon") val favicon: String? = null,
    @SerialName("tags") val tags: String? = null,
    @SerialName("country") val country: String? = null,
    @SerialName("countrycode") val countryCode: String? = null,
    @SerialName("language") val language: String? = null,
    @SerialName("codec") val codec: String? = null,
    @SerialName("bitrate") val bitrate: Int = 0,
    @SerialName("votes") val votes: Int = 0
)
