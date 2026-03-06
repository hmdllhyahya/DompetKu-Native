package com.dompetku.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dompetku.data.local.dao.AccountDao
import com.dompetku.data.local.dao.AttachmentDao
import com.dompetku.data.local.dao.TransactionDao
import com.dompetku.data.local.entity.AccountEntity
import com.dompetku.data.local.entity.AttachmentEntity
import com.dompetku.data.local.entity.TransactionEntity

@Database(
    entities  = [TransactionEntity::class, AccountEntity::class, AttachmentEntity::class],
    version   = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao():     AccountDao
    abstract fun attachmentDao():  AttachmentDao

    companion object {
        const val DATABASE_NAME = "dompetku.db"
    }
}
