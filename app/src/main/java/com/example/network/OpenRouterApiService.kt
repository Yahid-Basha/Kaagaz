package com.example.network

import retrofit2.http.Body
import retrofit2.http.POST

interface OpenRouterApiService {
    @POST("api/v1/chat/completions")
    suspend fun chatCompletion(@Body request: OpenRouterChatRequest): OpenRouterChatResponse
}
