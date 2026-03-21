package com.dompetku.domain.model

// ── Transaction ───────────────────────────────────────────────────────────────
data class Transaction(
    val id:            String,
    val type:          TransactionType,          // income | expense | transfer
    val amount:        Long,
    val adminFee:      Long        = 0L,
    val category:      String,
    val note:          String      = "",
    val date:          String,                   // YYYY-MM-DD local
    val time:          String,                   // HH:mm local
    val accountId:     String,
    val fromId:        String?     = null,        // transfer only
    val toId:          String?     = null,        // transfer only
    val detected:      Boolean?    = null,        // smart-category auto-detected
    val details:       Map<String, String> = emptyMap(),  // contextual fields (JSON)
    val attachmentIds: List<String>         = emptyList(),
)

enum class TransactionType { income, expense, transfer }

// ── Account ───────────────────────────────────────────────────────────────────
data class Account(
    val id:            String,
    val type:          AccountType,
    val name:          String,
    val balance:       Long,
    val last4:         String?     = null,
    val gradientStart: Long,
    val gradientEnd:   Long,
    val brandKey:      String?     = null,
    val sortOrder:     Int         = 0,
)

enum class AccountType {
    bank, ewallet, emoney, cash, savings, investment, credit, other
}

// ── Attachment ────────────────────────────────────────────────────────────────
data class Attachment(
    val id:            String,                   // "att_{uuid}"
    val transactionId: String,
    val filePath:      String,
    val mimeType:      String,
)

// ── UserProfile ───────────────────────────────────────────────────────────────
data class UserProfile(
    val name:    String = "",
    val age:     Int    = 0,
    val job:     String = "",
    val edu:     String = "",
)

// ── AppPreferences (mirrors DataStore keys) ───────────────────────────────────
data class AppPreferences(
    val onboarded:     Boolean = false,
    val pinEnabled:    Boolean = false,
    val pinHash:       String  = "",
    val bioEnabled:    Boolean = false,
    val soundEnabled:  Boolean = true,
    val hideBalance:   Boolean = false,
    val lang:          String  = "id",
    val monthlyBudget: Long    = 0L,
    val savedPct:      Int     = 0,
    val userProfile:   UserProfile = UserProfile(),
    val avatarPath:    String  = "",
    val notifEnabled:       Boolean = true,
    val vibrationEnabled:  Boolean = true,
)
