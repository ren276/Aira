package com.aira.health.data.remote.strava

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StravaTokenResponse(
    @SerialName("token_type") val tokenType: String,
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_at") val expiresAt: Long,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("scope") val scope: String,
    @SerialName("athlete") val athlete: StravaAthleteDto
)

@Serializable
data class StravaAthleteDto(
    @SerialName("id") val id: Long
)

@Serializable
data class StravaActivityDto(
    @SerialName("id") val id: Long,
    @SerialName("type") val type: String? = null,
    @SerialName("sport_type") val sportType: String? = null,
    @SerialName("distance") val distanceMeters: Float? = null,
    @SerialName("moving_time") val movingTimeSec: Int? = null,
    @SerialName("elapsed_time") val elapsedTimeSec: Int? = null,
    @SerialName("average_heartrate") val averageHeartRate: Float? = null,
    @SerialName("max_heartrate") val maxHeartRate: Float? = null,
    @SerialName("kilojoules") val kiloJoules: Float? = null,
    @SerialName("calories") val calories: Float? = null,
    @SerialName("start_date") val startDate: String,
    @SerialName("start_date_local") val startDateLocal: String? = null
)
