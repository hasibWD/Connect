package com.example.data.insforge

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class InsForgeClient(
  private var baseUrl: String = BuildConfig.INSFORGE_URL.ifBlank { "https://app.insforge.app" },
  private var anonKey: String = BuildConfig.INSFORGE_ANON_KEY,
  private var userToken: String? = null
) {

  private var apiService: InsForgeApiService? = null

  init {
    rebuildService()
  }

  fun updateConfig(newUrl: String, newAnonKey: String, token: String? = userToken) {
    var formattedUrl = newUrl.trim()
    if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
      formattedUrl = "https://$formattedUrl"
    }
    if (!formattedUrl.endsWith("/")) {
      formattedUrl = "$formattedUrl/"
    }
    this.baseUrl = formattedUrl
    this.anonKey = newAnonKey.trim()
    this.userToken = token
    rebuildService()
  }

  fun setAuthToken(token: String?) {
    this.userToken = token
    rebuildService()
  }

  private fun rebuildService() {
    try {
      val headerInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
          .header("Accept", "application/json")
          .header("Content-Type", "application/json")

        if (anonKey.isNotBlank()) {
          requestBuilder.header("apikey", anonKey)
        }

        val bearerToken = userToken ?: anonKey
        if (bearerToken.isNotBlank()) {
          requestBuilder.header("Authorization", "Bearer $bearerToken")
        }

        chain.proceed(requestBuilder.build())
      }

      val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
      }

      val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(headerInterceptor)
        .addInterceptor(logging)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

      val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

      val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

      apiService = retrofit.create(InsForgeApiService::class.java)
    } catch (e: Exception) {
      apiService = null
    }
  }

  fun getService(): InsForgeApiService? = apiService
  fun getBaseUrl(): String = baseUrl
  fun getAnonKey(): String = anonKey

  suspend fun testConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    if (anonKey.isBlank()) {
      return@withContext Pair(false, "Missing InsForge Public Anon Key. Please enter it in Backend Setup.")
    }
    val service = apiService ?: return@withContext Pair(false, "Invalid InsForge URL format.")
    try {
      val response = service.checkHealth()
      if (response.isSuccessful || response.code() in 200..299 || response.code() == 404) {
        Pair(true, "Successfully connected to InsForge (${response.code()})")
      } else if (response.code() == 401 || response.code() == 403) {
        Pair(false, "Authentication Failed (HTTP ${response.code()}). Check your InsForge Anon Key.")
      } else {
        Pair(false, "InsForge responded with HTTP ${response.code()}: ${response.message()}")
      }
    } catch (e: Exception) {
      Pair(false, "Connection error: ${e.localizedMessage ?: "Unable to reach server"}")
    }
  }
}
