package com.example.vektorgate.requests

import kotlinx.serialization.Serializable

@Serializable
class EnrollmentRequest (
    val device_name: String,
    val public_key: String,
    val firebase_token: String
)
@Serializable
data class EnrollmentResponse(
    val device_id: String,
    val status: String
)

@Serializable
class NewTokenRequest (
    val device_id: String,
    val token: String
)