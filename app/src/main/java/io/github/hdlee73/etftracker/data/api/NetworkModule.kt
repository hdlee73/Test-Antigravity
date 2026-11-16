package io.github.hdlee73.etftracker.data.api

import io.github.hdlee73.etftracker.data.prefs.AppPrefs
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun createClient(prefs: AppPrefs): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val token = prefs.appToken
                val reqBuilder = chain.request().newBuilder()
                    .header("Accept", "application/json")
                if (token.isNotBlank()) {
                    reqBuilder.header("X-App-Token", token)
                }
                chain.proceed(reqBuilder.build())
            }
            .build()
    }

    fun createApi(prefs: AppPrefs): EveryCorpApi {
        val rawUrl = prefs.apiBaseUrl.trim().trimEnd('/') + "/"
        val safeUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            rawUrl
        } else {
            "https://$rawUrl"
        }
        return Retrofit.Builder()
            .baseUrl(safeUrl)
            .client(createClient(prefs))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(EveryCorpApi::class.java)
    }
}
