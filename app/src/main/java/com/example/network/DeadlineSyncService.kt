package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI

// A trusted-source finding a user should review before any obligation date is touched -
// this app never auto-overwrites a due date from a sync result.
data class SyncFinding(
    val category: String,
    val ruleTitle: String,
    val oldRule: String,
    val newRule: String,
    val sourceUrl: String,
    val confidence: Float
)

sealed class SyncOutcome {
    data class Success(
        val findings: List<SyncFinding>,
        val checkedAt: Long,
        val categoriesChecked: Int
    ) : SyncOutcome()

    data class Unavailable(val reason: String) : SyncOutcome()
}

private data class HardcodedRule(val title: String, val currentDescription: String)

private data class SyncCategoryDef(
    val category: String,
    val allowedDomains: List<String>,
    val rules: List<HardcodedRule>
)

/**
 * Cross-checks the hardcoded obligation deadlines in DocumentClassifier.getTriggeredObligations
 * against official sources via OpenRouter's web_search tool, restricted per category to the same
 * trusted domain already listed in SyncScreen's sources list. Never modifies obligations itself -
 * callers surface any confident finding to the user as a reviewable notice.
 */
object DeadlineSyncService {
    private const val TAG = "DeadlineSyncService"
    private const val MODEL = "minimax/minimax-m2"

    // Higher bar than document-classification's 0.5 - a false positive here would show the user
    // an incorrect claim about an official regulatory change, not just a mis-tagged field.
    private const val CONFIDENCE_THRESHOLD = 0.7f

    // Keep these in sync with the day-counts in DocumentClassifier.getTriggeredObligations.
    private val CATEGORIES = listOf(
        SyncCategoryDef(
            category = "RC / PUC (Vehicle)",
            allowedDomains = listOf("parivahan.gov.in"),
            rules = listOf(
                HardcodedRule("PUC Emission Renewal", "180 days after issue/PUC date"),
                HardcodedRule("FASTag KYC Verification", "90 days after issue"),
                HardcodedRule("Driving Licence (DL) Validity", "1825 days (5 years) after issue")
            )
        ),
        SyncCategoryDef(
            category = "PAN Card",
            allowedDomains = listOf("incometax.gov.in"),
            rules = listOf(HardcodedRule("PAN-Aadhaar Link Verification", "180 days after issue"))
        ),
        SyncCategoryDef(
            category = "LPG Connection",
            allowedDomains = listOf("cx.indianoil.in"),
            rules = listOf(HardcodedRule("LPG Biometric e-KYC Status", "30 days after issue"))
        ),
        SyncCategoryDef(
            category = "Insurance Policy",
            allowedDomains = listOf("licindia.in", "irdai.gov.in"),
            rules = listOf(
                HardcodedRule("Policy Annual Premium Renewal", "365 days after issue"),
                HardcodedRule("Policy Nominee & Beneficiary Check", "180 days after issue")
            )
        )
    )

    private val moshi = Moshi.Builder().build()
    private val deadlineCheckAdapter = moshi.adapter(DeadlineCheckResponse::class.java)

    suspend fun runSync(context: Context): SyncOutcome = withContext(Dispatchers.IO) {
        val apiKey = OpenRouterClient.apiKey
        if (apiKey.isBlank()) {
            Log.w(TAG, "OPENROUTER_API_KEY not configured, skipping sync")
            return@withContext SyncOutcome.Unavailable("OpenRouter API key not configured")
        }
        if (!hasInternetConnection(context)) {
            return@withContext SyncOutcome.Unavailable("No internet connection")
        }

        val findings = mutableListOf<SyncFinding>()
        var successCount = 0

        for (def in CATEGORIES) {
            try {
                val request = OpenRouterChatRequest(
                    model = MODEL,
                    messages = listOf(OpenRouterMessage(role = "user", content = buildPrompt(def))),
                    tools = listOf(
                        OpenRouterTool(
                            type = "openrouter:web_search",
                            parameters = OpenRouterWebSearchParameters(
                                allowedDomains = def.allowedDomains,
                                maxResults = 5
                            )
                        )
                    )
                )

                val response = OpenRouterClient.apiService.chatCompletion(request)
                val content = response.choices.firstOrNull()?.message?.content
                if (content.isNullOrBlank()) {
                    Log.w(TAG, "Empty OpenRouter response for category ${def.category}")
                    continue
                }

                successCount++
                val parsed = parseDeadlineCheckResponse(content)
                if (parsed == null) {
                    Log.w(TAG, "Unparseable OpenRouter response for category ${def.category}: $content")
                    continue
                }

                for (finding in parsed.findings) {
                    if (!finding.changed) continue
                    val rule = def.rules.find { it.title == finding.ruleTitle } ?: continue
                    val confidence = finding.confidence.toFloat().coerceIn(0f, 1f)
                    if (confidence < CONFIDENCE_THRESHOLD) continue
                    val sourceUrl = finding.sourceUrl
                    // Don't trust the model's claimed source blindly - it must actually be on
                    // the domain we restricted the search to, same principle as validating a
                    // model-reported documentNumber against its expected regex.
                    if (sourceUrl.isNullOrBlank() || !isUrlOnAllowedDomain(sourceUrl, def.allowedDomains)) continue

                    findings.add(
                        SyncFinding(
                            category = def.category,
                            ruleTitle = rule.title,
                            oldRule = rule.currentDescription,
                            newRule = finding.newRuleDescription?.takeIf { it.isNotBlank() }
                                ?: "Changed - see source for details",
                            sourceUrl = sourceUrl,
                            confidence = confidence
                        )
                    )
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Sync check failed for category ${def.category}", e)
            }
        }

        if (successCount == 0) {
            SyncOutcome.Unavailable("Could not reach OpenRouter")
        } else {
            SyncOutcome.Success(
                findings = findings,
                checkedAt = System.currentTimeMillis(),
                categoriesChecked = successCount
            )
        }
    }

    private fun buildPrompt(def: SyncCategoryDef): String {
        val rulesList = def.rules.joinToString("\n") { "- ${it.title}: currently ${it.currentDescription}" }
        val domainsList = def.allowedDomains.joinToString(", ")
        return """
            You are verifying Indian regulatory/consumer compliance deadlines. Search ONLY these
            official domain(s): $domainsList. For each rule below, check whether an official
            announcement on those domains has changed or extended the standard interval.
            Respond with ONLY strict JSON, no markdown, no code fences, no commentary, in exactly
            this shape: {"findings":[{"ruleTitle":string,"changed":boolean,"newRuleDescription":string or null,"confidence":number between 0 and 1,"sourceUrl":string or null}]}.
            Include exactly one entry per rule below, using ruleTitle exactly as given. If you find
            no official change for a rule, set changed=false, sourceUrl=null, and confidence to how
            sure you are that nothing changed.

            Rules to check:
            $rulesList
        """.trimIndent()
    }

    private fun isUrlOnAllowedDomain(url: String, allowedDomains: List<String>): Boolean {
        return try {
            val host = URI(url).host?.lowercase() ?: return false
            allowedDomains.any { domain ->
                val d = domain.lowercase()
                host == d || host.endsWith(".$d")
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun parseDeadlineCheckResponse(content: String): DeadlineCheckResponse? {
        var cleaned = content.trim()
        if (cleaned.startsWith("```json", ignoreCase = true)) {
            cleaned = cleaned.substring(7)
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3)
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length - 3)
        }
        cleaned = cleaned.trim()

        return try {
            deadlineCheckAdapter.fromJson(cleaned)
        } catch (e: Exception) {
            val start = cleaned.indexOf('{')
            val end = cleaned.lastIndexOf('}')
            if (start != -1 && end != -1 && start < end) {
                try {
                    deadlineCheckAdapter.fromJson(cleaned.substring(start, end + 1))
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }

    private fun hasInternetConnection(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Throwable) {
            false
        }
    }
}
