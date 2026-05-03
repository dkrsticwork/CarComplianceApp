package com.carcomplianceapp.data.remote

import com.carcomplianceapp.domain.model.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

sealed class AiResult {
    data class Success(val tasks: List<AiTask>) : AiResult()
    data class Error(val error: ApiKeyError, val message: String) : AiResult()
}

data class AiTask(
    val title: String,
    val category: String,
    val dueDateWindow: String,
    val urgency: String,
    val why: String,
    val dueDate: String?
)

@Singleton
class AiApiService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {

    // ── Key detection ─────────────────────────────────────────────────────────

    fun detectProvider(apiKey: String): AiProvider {
        return when {
            apiKey.startsWith("sk-ant-") -> AiProvider.ANTHROPIC
            apiKey.startsWith("sk-") && apiKey.length > 40 -> AiProvider.OPENAI
            apiKey.startsWith("AIza") -> AiProvider.GOOGLE
            apiKey.startsWith("mis") || (apiKey.length == 32 && apiKey.all { it.isLetterOrDigit() }) -> AiProvider.MISTRAL
            else -> AiProvider.COHERE
        }
    }

    // ── Main entry point ──────────────────────────────────────────────────────

    suspend fun generateComplianceTasks(
        car: Car,
        apiKeyConfig: ApiKeyConfig,
        documentSummaries: List<String> = emptyList()
    ): AiResult = withContext(Dispatchers.IO) {

        val prompt = buildPrompt(car, documentSummaries)

        return@withContext when (apiKeyConfig.provider) {
            AiProvider.OPENAI -> callOpenAi(apiKeyConfig.rawKey, prompt)
            AiProvider.ANTHROPIC -> callAnthropic(apiKeyConfig.rawKey, prompt)
            AiProvider.GOOGLE -> callGoogle(apiKeyConfig.rawKey, prompt)
            AiProvider.MISTRAL -> callMistral(apiKeyConfig.rawKey, prompt)
            AiProvider.COHERE -> callCohere(apiKeyConfig.rawKey, prompt)
        }
    }

    // ── Prompt builder ────────────────────────────────────────────────────────

    private fun buildPrompt(car: Car, documentSummaries: List<String>): String {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val docSection = if (documentSummaries.isNotEmpty()) {
            "Attached document summaries:\n" + documentSummaries.joinToString("\n")
        } else {
            "No documents attached."
        }

        return """
You are a car compliance expert. Generate a personalized obligation list for this car owner.

TODAY: $today

CAR:
- Make/Model: ${car.make} ${car.model}
- Year: ${car.year}
- Fuel: ${car.fuelType.displayName}
- Country: ${car.countryCode}
- Last service: ${car.lastServiceDate ?: "unknown"}
- Insurance expiry: ${car.insuranceExpiry ?: "unknown"}
- Registration expiry: ${car.registrationExpiry ?: "unknown"}
- Odometer: ${car.odometerKm?.let { "$it km" } ?: "unknown"}

$docSection

Generate 4–8 compliance tasks covering:
1. Legal obligations (registration, technical inspection, emissions) specific to ${car.countryCode}
2. Insurance requirements
3. Maintenance tasks (oil, brakes, tires, fluids) based on age and mileage
4. Documentation requirements

RULES:
- If you are uncertain about an exact date, express it as a range (e.g., "May–June 2025")
- Never fabricate specific legal deadlines you don't know for certain
- Urgency: "CRITICAL" = overdue, "HIGH" = within 7 days, "MEDIUM" = within 30 days, "LOW" = future
- Category: one of LEGAL, MAINTENANCE, INSURANCE, DOCUMENTATION
- Each task MUST include a clear "why" explanation (1–2 sentences, plain language)

Respond ONLY with a valid JSON array. No preamble, no markdown, no explanation outside the JSON.

Format:
[
  {
    "title": "Technical inspection",
    "category": "LEGAL",
    "dueDate": "2025-06-15",
    "dueDateWindow": "June 2025",
    "urgency": "MEDIUM",
    "why": "Annual technical inspection is required by law in Serbia for vehicles over 4 years old."
  }
]
""".trimIndent()
    }

    // ── OpenAI ────────────────────────────────────────────────────────────────

    private suspend fun callOpenAi(key: String, prompt: String): AiResult {
        val body = JsonObject().apply {
            addProperty("model", "gpt-4o-mini")
            add("messages", gson.toJsonTree(listOf(
                mapOf("role" to "system", "content" to "You are a car compliance expert. Always respond with valid JSON only."),
                mapOf("role" to "user", "content" to prompt)
            )))
            addProperty("max_tokens", 2000)
            addProperty("temperature", 0.2)
        }
        return makeRequest(
            url = "https://api.openai.com/v1/chat/completions",
            key = key,
            body = body.toString(),
            authHeader = "Bearer $key",
            extractor = { response ->
                val choices = response.getAsJsonArray("choices")
                choices[0].asJsonObject
                    .getAsJsonObject("message")
                    .get("content").asString
            }
        )
    }

    // ── Anthropic ─────────────────────────────────────────────────────────────

    private suspend fun callAnthropic(key: String, prompt: String): AiResult {
        val body = JsonObject().apply {
            addProperty("model", "claude-haiku-4-5-20251001")
            addProperty("max_tokens", 2000)
            add("messages", gson.toJsonTree(listOf(
                mapOf("role" to "user", "content" to prompt)
            )))
            addProperty("system", "You are a car compliance expert. Always respond with valid JSON only.")
        }
        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("x-api-key", key)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .build()

        return executeRequest(request) { response ->
            val content = response.getAsJsonArray("content")
            content[0].asJsonObject.get("text").asString
        }
    }

    // ── Google Gemini ─────────────────────────────────────────────────────────

    private suspend fun callGoogle(key: String, prompt: String): AiResult {
        val body = JsonObject().apply {
            add("contents", gson.toJsonTree(listOf(
                mapOf("role" to "user", "parts" to listOf(mapOf("text" to prompt)))
            )))
            add("generationConfig", gson.toJsonTree(mapOf(
                "temperature" to 0.2,
                "maxOutputTokens" to 2000
            )))
        }
        return makeRequest(
            url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$key",
            key = key,
            body = body.toString(),
            authHeader = null,
            extractor = { response ->
                response.getAsJsonArray("candidates")[0]
                    .asJsonObject.getAsJsonObject("content")
                    .getAsJsonArray("parts")[0]
                    .asJsonObject.get("text").asString
            }
        )
    }

    // ── Mistral ───────────────────────────────────────────────────────────────

    private suspend fun callMistral(key: String, prompt: String): AiResult {
        val body = JsonObject().apply {
            addProperty("model", "mistral-small-latest")
            add("messages", gson.toJsonTree(listOf(
                mapOf("role" to "system", "content" to "You are a car compliance expert. Always respond with valid JSON only."),
                mapOf("role" to "user", "content" to prompt)
            )))
            addProperty("max_tokens", 2000)
            addProperty("temperature", 0.2)
        }
        return makeRequest(
            url = "https://api.mistral.ai/v1/chat/completions",
            key = key,
            body = body.toString(),
            authHeader = "Bearer $key",
            extractor = { response ->
                response.getAsJsonArray("choices")[0]
                    .asJsonObject.getAsJsonObject("message")
                    .get("content").asString
            }
        )
    }

    // ── Cohere ────────────────────────────────────────────────────────────────

    private suspend fun callCohere(key: String, prompt: String): AiResult {
        val body = JsonObject().apply {
            addProperty("model", "command-r-plus")
            addProperty("message", prompt)
            addProperty("preamble", "You are a car compliance expert. Always respond with valid JSON only.")
            addProperty("temperature", 0.2)
        }
        return makeRequest(
            url = "https://api.cohere.com/v1/chat",
            key = key,
            body = body.toString(),
            authHeader = "Bearer $key",
            extractor = { response ->
                response.get("text").asString
            }
        )
    }

    // ── Shared HTTP logic ─────────────────────────────────────────────────────

    private suspend fun makeRequest(
        url: String,
        key: String,
        body: String,
        authHeader: String?,
        extractor: (JsonObject) -> String
    ): AiResult {
        val builder = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json".toMediaType()))
            .addHeader("content-type", "application/json")
        authHeader?.let { builder.addHeader("Authorization", it) }
        return executeRequest(builder.build(), extractor)
    }

    private suspend fun executeRequest(
        request: Request,
        extractor: (JsonObject) -> String
    ): AiResult = withContext(Dispatchers.IO) {
        try {
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            when (response.code) {
                200, 201 -> {
                    val json = gson.fromJson(responseBody, JsonObject::class.java)
                    val content = extractor(json)
                    parseTasksFromContent(content)
                }
                401 -> AiResult.Error(ApiKeyError.INVALID_KEY,
                    "Your API key is invalid or has been revoked. Please check your key in Settings and enter a valid one.")
                402 -> AiResult.Error(ApiKeyError.INSUFFICIENT_FUNDS,
                    "Your account has insufficient funds. Please add credits to your AI provider account to continue generating compliance tasks.")
                403 -> AiResult.Error(ApiKeyError.EXPIRED,
                    "Your API key has expired or lacks permission. Check your provider dashboard and renew or replace your key.")
                429 -> AiResult.Error(ApiKeyError.RATE_LIMITED,
                    "Too many requests. Please wait a few minutes and try again. Your tasks from last time are still saved.")
                else -> {
                    // Check for billing errors in body
                    val lowerBody = responseBody.lowercase()
                    when {
                        lowerBody.contains("insufficient_quota") || lowerBody.contains("billing") ->
                            AiResult.Error(ApiKeyError.INSUFFICIENT_FUNDS,
                                "Your AI account has run out of credits. Add billing to your provider account to refresh tasks.")
                        lowerBody.contains("expired") || lowerBody.contains("invalid_api_key") ->
                            AiResult.Error(ApiKeyError.INVALID_KEY,
                                "Your API key appears to be invalid or expired. Please update it in Settings.")
                        else ->
                            AiResult.Error(ApiKeyError.UNKNOWN,
                                "Unexpected error (${response.code}). Your saved tasks are still available. Try again later.")
                    }
                }
            }
        } catch (e: IOException) {
            AiResult.Error(ApiKeyError.NETWORK_ERROR,
                "Network error: couldn't reach the AI provider. Check your internet connection. Your saved tasks are still available.")
        } catch (e: Exception) {
            AiResult.Error(ApiKeyError.UNKNOWN, "Unexpected error: ${e.message}. Your saved tasks are still available.")
        }
    }

    // ── Parse JSON response into AiTask list ──────────────────────────────────

    private fun parseTasksFromContent(content: String): AiResult {
        return try {
            // Strip markdown code fences if present
            val cleaned = content
                .replace("```json", "")
                .replace("```", "")
                .trim()
                // Find JSON array
                .let { raw ->
                    val start = raw.indexOf('[')
                    val end = raw.lastIndexOf(']')
                    if (start != -1 && end != -1) raw.substring(start, end + 1) else raw
                }

            val tasks = gson.fromJson(cleaned, Array<AiTask>::class.java).toList()
            AiResult.Success(tasks)
        } catch (e: Exception) {
            AiResult.Error(ApiKeyError.UNKNOWN,
                "AI responded but the response couldn't be parsed. Your saved tasks are still available. Try refreshing.")
        }
    }
}
