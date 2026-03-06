package com.dompetku.data.repository

import com.dompetku.data.local.dao.AccountDao
import com.dompetku.data.local.dao.AttachmentDao
import com.dompetku.data.local.dao.TransactionDao
import com.dompetku.data.local.entity.*
import com.dompetku.domain.model.Account
import com.dompetku.domain.model.Attachment
import com.dompetku.domain.model.Transaction
import com.dompetku.domain.model.TransactionType
import com.dompetku.util.DateUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// ── TransactionRepository ─────────────────────────────────────────────────────
@Singleton
class TransactionRepository @Inject constructor(
    private val dao:  TransactionDao,
    private val gson: Gson,
) {
    val allTransactions: Flow<List<Transaction>> = dao.observeAll().map { it.map { e -> e.hydrate(gson) } }

    /** Alias used by ViewModels */
    fun observeAll(): Flow<List<Transaction>> = allTransactions

    fun transactionsByAccount(accountId: String): Flow<List<Transaction>> =
        dao.observeByAccount(accountId).map { it.map { e -> e.hydrate(gson) } }

    fun transactionsInRange(from: String, to: String): Flow<List<Transaction>> =
        dao.observeByDateRange(from, to).map { it.map { e -> e.hydrate(gson) } }

    suspend fun getById(id: String): Transaction? = dao.getById(id)?.hydrate(gson)

    suspend fun insert(txn: Transaction)           { dao.insert(txn.toEntityFull(gson)) }
    suspend fun insertAll(txns: List<Transaction>) { dao.insertAll(txns.map { it.toEntityFull(gson) }) }
    suspend fun update(txn: Transaction)           { dao.update(txn.toEntityFull(gson)) }
    suspend fun delete(txn: Transaction)           { dao.deleteById(txn.id) }
    suspend fun delete(id: String)                 { dao.deleteById(id) }
    suspend fun deleteAll()                        { dao.deleteAll() }
    suspend fun deleteByAccount(accountId: String) { dao.deleteByAccount(accountId) }

    /** Records a balance adjustment as a synthetic income/expense transaction */
    suspend fun recordBalanceAdjustment(accountId: String, accountName: String, delta: Long) {
        insert(Transaction(
            id        = UUID.randomUUID().toString(),
            type      = if (delta > 0) TransactionType.income else TransactionType.expense,
            amount    = Math.abs(delta),
            category  = "Penyesuaian Saldo",
            note      = "Penyesuaian saldo — $accountName",
            date      = DateUtils.todayStr(),
            time      = DateUtils.nowTimeStr(),
            accountId = accountId,
        ))
    }

    /** Transfer: persist the transfer transaction only — balance mutation via AccountRepository */
    suspend fun recordTransfer(txn: Transaction) = insert(txn)

    // ── Analytics helpers ─────────────────────────────────────────────────────
    suspend fun sumByTypeInRange(type: TransactionType, from: String, to: String): Long =
        dao.sumByTypeAndDateRange(type.name, from, to) ?: 0L

    suspend fun expenseByCategoryInRange(from: String, to: String) =
        dao.expenseByCategoryInRange(from, to)

    // ── Gson helpers ──────────────────────────────────────────────────────────
    private val mapType  = object : TypeToken<Map<String, String>>() {}.type
    private val listType = object : TypeToken<List<String>>() {}.type

    private fun TransactionEntity.hydrate(gson: Gson): Transaction {
        val details: Map<String, String> =
            runCatching<Map<String, String>> { gson.fromJson(detailsJson, mapType) }.getOrDefault(emptyMap())
        val ids: List<String> =
            runCatching<List<String>> { gson.fromJson(attachmentIdsJson, listType) }.getOrDefault(emptyList())
        return toDomain().copy(details = details, attachmentIds = ids)
    }

    private fun Transaction.toEntityFull(gson: Gson): TransactionEntity =
        toEntity(
            detailsJson       = gson.toJson(details),
            attachmentIdsJson = gson.toJson(attachmentIds),
        )
}

// ── AccountRepository ─────────────────────────────────────────────────────────
@Singleton
class AccountRepository @Inject constructor(
    private val dao: AccountDao,
) {
    val allAccounts: Flow<List<Account>> = dao.observeAll().map { it.map(AccountEntity::toDomain) }
    val totalBalance: Flow<Long>         = dao.observeTotalBalance().map { it ?: 0L }

    /** Alias used by ViewModels */
    fun observeAll(): Flow<List<Account>> = allAccounts

    suspend fun getById(id: String): Account? = dao.getById(id)?.toDomain()
    suspend fun insert(account: Account)       { dao.insert(account.toEntity()) }
    suspend fun update(account: Account)       { dao.update(account.toEntity()) }
    suspend fun delete(account: Account)       { dao.deleteById(account.id) }
    suspend fun delete(id: String)             { dao.deleteById(id) }
    suspend fun deleteAll()                    { dao.deleteAll() }

    suspend fun adjustBalance(accountId: String, delta: Long) {
        val acc = dao.getById(accountId) ?: return
        dao.update(acc.copy(balance = acc.balance + delta))
    }

    suspend fun applyTransfer(fromId: String, toId: String, amount: Long, adminFee: Long) {
        adjustBalance(fromId, -(amount + adminFee))
        adjustBalance(toId, +amount)
    }
}

// ── AttachmentRepository ──────────────────────────────────────────────────────
@Singleton
class AttachmentRepository @Inject constructor(
    private val dao: AttachmentDao,
) {
    suspend fun getByTransaction(txnId: String): List<Attachment> =
        dao.getByTransaction(txnId).map(AttachmentEntity::toDomain)

    suspend fun insert(attachment: Attachment)   { dao.insert(attachment.toEntity()) }
    suspend fun delete(attachment: Attachment)   { dao.delete(attachment.toEntity()) }
    suspend fun deleteByTransaction(txnId: String) { dao.deleteByTransaction(txnId) }
    suspend fun deleteAll()                        { dao.deleteAll() }
}