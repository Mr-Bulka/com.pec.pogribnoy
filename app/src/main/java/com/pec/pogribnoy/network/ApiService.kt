package com.pec.pogribnoy.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.MultipartBody

interface ApiService {
    @POST("api/qr/login")
    suspend fun login(@Body request: LoginRequestDto): StudentDto

    @GET("api/qr/student/{code}")
    suspend fun getStudent(@Path("code") code: String): StudentDto

    @Multipart
    @POST("api/qr/student/{code}/avatar")
    suspend fun uploadAvatar(
        @Path("code") code: String,
        @Part file: MultipartBody.Part
    ): UploadResponseDto
}
