package com.carcomplianceapp.domain.model

import java.time.LocalDate

// ── Car ──────────────────────────────────────────────────────────────────────

data class Car(
    val id: Long = 0,
    val nickname: String,
    val make: String,
    val model: String,
    val year: Int,
    val fuelType: FuelType,
    val countryCode: String,
    val lastServiceDate: LocalDate? = null,
    val insuranceExpiry: LocalDate? = null,
    val registrationExpiry: LocalDate? = null,
    val odometerKm: Int? = null,
    val attachedDocumentPaths: List<String> = emptyList(),
    val createdAt: LocalDate = LocalDate.now()
)

enum class FuelType(val displayName: String) {
    PETROL("Petrol"),
    DIESEL("Diesel"),
    ELECTRIC("Electric"),
    HYBRID("Hybrid"),
    LPG("LPG"),
    UNKNOWN("Unknown")
}

// ── Task ─────────────────────────────────────────────────────────────────────

data class ComplianceTask(
    val id: Long = 0,
    val carId: Long,
    val title: String,
    val category: TaskCategory,
    val dueDate: LocalDate?,
    val dueDateWindow: String,          // Human-readable range e.g. "June–July 2025"
    val status: TaskStatus,
    val urgency: UrgencyLevel,
    val why: String,                    // Transparent explanation
    val isUserEditable: Boolean = true,
    val isUserOverride: Boolean = false,
    val snoozedUntil: LocalDate? = null,
    val completedAt: LocalDate? = null,
    val createdAt: LocalDate = LocalDate.now()
)

enum class TaskCategory(val displayName: String) {
    LEGAL("Legal"),
    MAINTENANCE("Maintenance"),
    INSURANCE("Insurance"),
    DOCUMENTATION("Documentation")
}

enum class TaskStatus {
    UPCOMING, OVERDUE, DONE, SNOOZED
}

enum class UrgencyLevel {
    CRITICAL,   // overdue
    HIGH,       // within 7 days
    MEDIUM,     // within 30 days
    LOW         // planned / future
}

// ── API Key ───────────────────────────────────────────────────────────────────

data class ApiKeyConfig(
    val rawKey: String,
    val provider: AiProvider,
    val isValid: Boolean = true,
    val lastError: ApiKeyError? = null
)

enum class AiProvider(val displayName: String, val baseUrl: String) {
    OPENAI("OpenAI", "https://api.openai.com/"),
    ANTHROPIC("Anthropic", "https://api.anthropic.com/"),
    GOOGLE("Google Gemini", "https://generativelanguage.googleapis.com/"),
    MISTRAL("Mistral AI", "https://api.mistral.ai/"),
    COHERE("Cohere", "https://api.cohere.com/")
}

enum class ApiKeyError {
    EXPIRED,
    INSUFFICIENT_FUNDS,
    INVALID_KEY,
    RATE_LIMITED,
    NETWORK_ERROR,
    UNKNOWN
}

// ── Notification ──────────────────────────────────────────────────────────────

data class NotificationPreferences(
    val thirtyDays: Boolean = true,
    val sevenDays: Boolean = true,
    val oneDay: Boolean = true
)
