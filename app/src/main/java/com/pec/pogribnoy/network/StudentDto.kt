package com.pec.pogribnoy.network

import com.google.gson.annotations.SerializedName

data class StudentDto(
    val id: String,
    @SerializedName("full_name") val fullName: String,
    val organization: String,
    @SerializedName("issue_date") val issueDate: String,
    val specialty: String,
    val course: String,
    val hash: String? = null,
    @SerializedName("avatar_base64") val avatarBase64: String? = null
)

data class LoginRequestDto(
    val code: String,
    val mood: String? = "neutral"
)

data class UploadResponseDto(
    val message: String,
    val matched_count: Int,
    val modified_count: Int,
    @SerializedName("avatar_preview") val avatarPreview: String? = null
)
