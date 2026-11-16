package com.example.data.insforge

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

data class InsForgeAuthRequest(
  val email: String,
  val password: String,
  val data: Map<String, String>? = null
)

data class InsForgeUserDto(
  val id: String = "",
  val email: String? = null
)

data class InsForgeAuthResponse(
  val access_token: String? = null,
  val token_type: String? = null,
  val user: InsForgeUserDto? = null
)

data class InsForgeProfileDto(
  val id: String = "",
  val username: String = "",
  val full_name: String = "",
  val avatar_url: String? = null,
  val bio: String? = null,
  val is_online: Boolean? = false,
  val role: String? = "user"
)

data class InsForgeMessageDto(
  val id: String = "",
  val chat_id: String = "",
  val sender_id: String = "",
  val content: String = "",
  val type: String = "text",
  val media_url: String? = null,
  val status: String = "sent",
  val created_at: String = ""
)

interface InsForgeApiService {

  // Auth Endpoints
  @POST("auth/v1/signup")
  suspend fun signUp(
    @Body request: InsForgeAuthRequest
  ): Response<InsForgeAuthResponse>

  @POST("auth/v1/token?grant_type=password")
  suspend fun signIn(
    @Body request: InsForgeAuthRequest
  ): Response<InsForgeAuthResponse>

  @POST("auth/v1/recover")
  suspend fun recoverPassword(
    @Body request: Map<String, String>
  ): Response<ResponseBody>

  // Health / Status Check
  @GET("rest/v1/")
  suspend fun checkHealth(): Response<ResponseBody>

  // Database REST Endpoints
  @GET("rest/v1/profiles")
  suspend fun getProfiles(
    @Query("select") select: String = "*"
  ): Response<List<InsForgeProfileDto>>

  @POST("rest/v1/profiles")
  suspend fun upsertProfile(
    @Body profile: InsForgeProfileDto,
    @Header("Prefer") prefer: String = "resolution=merge-duplicates"
  ): Response<ResponseBody>

  @GET("rest/v1/messages")
  suspend fun getMessages(
    @Query("chat_id") chatIdFilter: String,
    @Query("select") select: String = "*",
    @Query("order") order: String = "created_at.asc"
  ): Response<List<InsForgeMessageDto>>

  @POST("rest/v1/messages")
  suspend fun sendMessage(
    @Body message: InsForgeMessageDto
  ): Response<ResponseBody>

  @PATCH("rest/v1/messages")
  suspend fun markMessagesAsRead(
    @Query("chat_id") chatIdFilter: String,
    @Query("sender_id") senderNotFilter: String,
    @Body statusUpdate: Map<String, String> = mapOf("status" to "read")
  ): Response<ResponseBody>

  @GET("rest/v1/typing_indicators")
  suspend fun getTypingIndicators(
    @Query("chat_id") chatIdFilter: String,
    @Query("is_typing") isTypingFilter: String = "eq.true",
    @Query("select") select: String = "*"
  ): Response<List<InsForgeTypingDto>>

  @POST("rest/v1/typing_indicators")
  suspend fun upsertTypingIndicator(
    @Body indicator: InsForgeTypingDto,
    @Header("Prefer") prefer: String = "resolution=merge-duplicates"
  ): Response<ResponseBody>
}

data class InsForgeTypingDto(
  val chat_id: String = "",
  val user_id: String = "",
  val user_name: String = "",
  val is_typing: Boolean = false
)
