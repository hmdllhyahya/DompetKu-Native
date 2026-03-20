package com.dompetku.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dompetku.domain.model.Account
import com.dompetku.domain.model.AccountType
import com.dompetku.domain.model.Attachment
import com.dompetku.domain.model.Transaction
import com.dompetku.domain.model.TransactionType

// ── TransactionEntity ─────────────────────────────────────────────────────────
@Entity(
    tableName = "transactions",
    indices = [
        Index("date"),                        // ORDER BY date DESC, range queries
        Index("accountId"),                   // observeByAccount, delete cascade
        Index("type"),                        // type filter
        Index(value = ["date", "type"]),      // analytics: type+date combined
    ],
)
data class TransactionEntity(
    @PrimaryKey val id:            String,
    val type:          String,          // TransactionType name
    val amount:        Long,
    val adminFee:      Long        = 0L,
    val category:      String,
    val note:          String      = "",
    val date:          String,          // YYYY-MM-DD local
    val time:          String,          // HH:mm local
    val accountId:     String,
    val fromId:        String?     = null,
    val toId:          String?     = null,
    val detected:      Boolean?    = null,
    val detailsJson:   String      = "{}",   // JSON map of contextual fields
    val attachmentIdsJson: String  = "[]",   // JSON array of attachment IDs
) {
    fun toDomain(): Transaction = Transaction(
        id            = id,
        type          = TransactionType.valueOf(type),
        amount        = amount,
        adminFee      = adminFee,
        category      = category,
        note          = note,
        date          = date,
        time          = time,
        accountId     = accountId,
        fromId        = fromId,
        toId          = toId,
        detected      = detected,
        // details and attachmentIds are hydrated by repository via Gson
    )
}

fun Transaction.toEntity(
    detailsJson: String       = "{}",
    attachmentIdsJson: String = "[]",
) = TransactionEntity(
    id                = id,
    type              = type.name,
    amount            = amount,
    adminFee          = adminFee,
    category          = category,
    note              = note,
    date              = date,
    time              = time,
    accountId         = accountId,
    fromId            = fromId,
    toId              = toId,
    detected          = detected,
    detailsJson       = detailsJson,
    attachmentIdsJson = attachmentIdsJson,
)

// ── AccountEntity ─────────────────────────────────────────────────────────────
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id:            String,
    val type:          String,
    val name:          String,
    val balance:       Long,
    val last4:         String?     = null,
    val gradientStart: Long,
    val gradientEnd:   Long,
    val brandKey:      String?     = null,
    val sortOrder:     Int         = 0,
) {
    fun toDomain() = Account(
        id            = id,
        type          = AccountType.valueOf(type),
        name          = name,
        balance       = balance,
        last4         = last4,
        gradientStart = gradientStart,
        gradientEnd   = gradientEnd,
        brandKey      = brandKey,
        sortOrder     = sortOrder,
    )
}

fun Account.toEntity() = AccountEntity(
    id            = id,
    type          = type.name,
    name          = name,
    balance       = balance,
    last4         = last4,
    gradientStart = gradientStart,
    gradientEnd   = gradientEnd,
    brandKey      = brandKey,
    sortOrder     = sortOrder,
)

// ── AttachmentEntity ──────────────────────────────────────────────────────────
@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity        = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns  = ["transactionId"],
            onDelete      = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("transactionId")],
)
data class AttachmentEntity(
    @PrimaryKey val id:            String,   // "att_{uuid}"
    val transactionId: String,
    val filePath:      String,
    val mimeType:      String,
) {
    fun toDomain() = Attachment(
        id            = id,
        transactionId = transactionId,
        filePath      = filePath,
        mimeType      = mimeType,
    )
}

fun Attachment.toEntity() = AttachmentEntity(
    id            = id,
    transactionId = transactionId,
    filePath      = filePath,
    mimeType      = mimeType,
)
