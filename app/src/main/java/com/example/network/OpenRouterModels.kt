package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenRouterChatRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    val tools: List<OpenRouterTool>
)

@JsonClass(generateAdapter = true)
data class OpenRouterMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class OpenRouterTool(
    val type: String,
    val parameters: OpenRouterWebSearchParameters
)

@JsonClass(generateAdapter = true)
data class OpenRouterWebSearchParameters(
    @Json(name = "allowed_domains") val allowedDomains: List<String>,
    @Json(name = "max_results") val maxResults: Int
)

@JsonClass(generateAdapter = true)
data class OpenRouterChatResponse(
    val choices: List<OpenRouterChoice> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OpenRouterChoice(
    val message: OpenRouterResponseMessage? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterResponseMessage(
    val content: String? = null
)

// Strict-JSON shape the model is instructed to answer in - parsed out of message.content.
@JsonClass(generateAdapter = true)
data class DeadlineCheckResponse(
    val findings: List<DeadlineRuleFinding> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DeadlineRuleFinding(
    val ruleTitle: String = "",
    val changed: Boolean = false,
    val newRuleDescription: String? = null,
    val confidence: Double = 0.0,
    val sourceUrl: String? = null
)
