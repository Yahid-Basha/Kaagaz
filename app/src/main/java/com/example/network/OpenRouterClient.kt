package com.example.network

import android.util.Log
import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object OpenRouterClient {
    private const val TAG = "OpenRouterClient"
    private const val BASE_URL = "https://openrouter.ai/"

    // Empty whenever OPENROUTER_API_KEY isn't configured - callers must treat a blank key as
    // "sync unavailable", never send a request with it.
    //
    // Read via reflection rather than a direct BuildConfig.OPENROUTER_API_KEY reference: the
    // Secrets Gradle Plugin only generates that field when it finds the key in .env (or
    // .env.example), and different secret-delivery setups (a local .env, AI Studio's Secrets
    // panel, a plain runtime env var) don't reliably agree on whether/when that happens. A
    // missing BuildConfig field must never be a compile error for an optional integration -
    // reflection degrades to the env-var fallback (or "") instead.
    val apiKey: String by lazy { resolveApiKey() }

    private fun resolveApiKey(): String {
        val fromBuildConfig = try {
            val field = Class.forName("com.example.BuildConfig").getField("OPENROUTER_API_KEY")
            field.get(null) as? String
        } catch (_: Throwable) {
            Log.i(TAG, "BuildConfig.OPENROUTER_API_KEY not generated (key not configured)")
            null
        }
        if (!fromBuildConfig.isNullOrBlank()) return fromBuildConfig

        return try {
            System.getenv("OPENROUTER_API_KEY") ?: ""
        } catch (_: Throwable) {
            ""
        }
    }

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $apiKey")
            .build()
        chain.proceed(request)
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val moshi: Moshi by lazy { Moshi.Builder().build() }

    val apiService: OpenRouterApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenRouterApiService::class.java)
    }
}
