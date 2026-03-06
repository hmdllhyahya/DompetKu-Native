package com.dompetku.data.local.dao

import androidx.room.*
import com.dompetku.data.local.entity.AccountEntity
import com.dompetku.data.local.entity.AttachmentEntity
import com.dompetku.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

// ── TransactionDao ────────────────────────────────────────────────────────────
@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC, time DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC, time DESC")
    fun observeByAccount(accountId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :from AND :to ORDER BY date DESC, time DESC")
    fun observeByDateRange(from: String, to: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<TransactionEntity>)

    @Update
    suspend fun update(entity: TransactionEntity)

    @Delete
    suspend fun delete(entity: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND date BETWEEN :from AND :to")
    suspend fun sumByTypeAndDateRange(type: String, from: String, to: String): Long?

    @Query("""
        SELECT category, SUM(amount) as total FROM transactions
        WHERE type = 'expense' AND date BETWEEN :from AND :to
        GROUP BY category ORDER BY total DESC
    """)
    suspend fun expenseByCategoryInRange(from: String, to: String): List<CategorySum>
}

data class CategorySum(val category: String, val total: Long)

// ── AccountDao ────────────────────────────────────────────────────────────────
@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AccountEntity)

    @Update
    suspend fun update(entity: AccountEntity)

    @Delete
    suspend fun delete(entity: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()

    @Query("SELECT SUM(balance) FROM accounts")
    fun observeTotalBalance(): Flow<Long?>
}

// ── AttachmentDao ─────────────────────────────────────────────────────────────
@Dao
interface AttachmentDao {

    @Query("SELECT * FROM attachments WHERE transactionId = :txnId")
    suspend fun getByTransaction(txnId: String): List<AttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AttachmentEntity)

    @Delete
    suspend fun delete(entity: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE transactionId = :txnId")
    suspend fun deleteByTransaction(txnId: String)

    @Query("DELETE FROM attachments")
    suspend fun deleteAll()
}
