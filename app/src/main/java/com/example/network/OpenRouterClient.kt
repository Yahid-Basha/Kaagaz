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
    val apiKey: String by lazy { resolveApiKey().value }

    // Distinguishes *why* the key is missing so the sync-unavailable message on screen can say
    // something more actionable than a generic "not configured" - this has been the actual
    // sticking point across several build attempts.
    val apiKeyStatus: String by lazy { resolveApiKey().status }

    private data class ResolvedKey(val value: String, val status: String)

    private fun resolveApiKey(): ResolvedKey {
        val buildConfigField = try {
            Class.forName("com.example.BuildConfig").getField("OPENROUTER_API_KEY")
        } catch (_: Throwable) {
            null
        }

        if (buildConfigField == null) {
            Log.i(TAG, "BuildConfig.OPENROUTER_API_KEY field does not exist - Secrets plugin never saw the key at build time")
            val fromEnv = try {
                System.getenv("OPENROUTER_API_KEY")
            } catch (_: Throwable) {
                null
            }
            if (!fromEnv.isNullOrBlank()) return ResolvedKey(fromEnv, "ok")
            return ResolvedKey("", "missing-buildconfig-field")
        }

        val value = try {
            buildConfigField.get(null) as? String
        } catch (_: Throwable) {
            null
        }
        if (value.isNullOrBlank()) {
            Log.i(TAG, "BuildConfig.OPENROUTER_API_KEY exists but is blank - key line likely empty/commented in whatever .env the build actually read")
            return ResolvedKey("", "blank-buildconfig-field")
        }
        return ResolvedKey(value, "ok")
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
