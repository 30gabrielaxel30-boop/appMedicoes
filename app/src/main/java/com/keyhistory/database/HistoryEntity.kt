package com.keyhistory.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "history",
    indices = [Index("timestamp"), Index("packageName")]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Conteúdo cifrado com AES-256-GCM (formato "v1:" + Base64(IV || ciphertext || tag)). */
    val content: String,
    val packageName: String?,
    val applicationName: String?,
    val timestamp: Long,
    val category: String,
    val encrypted: Boolean,
)

/** Índice de pesquisa cego: apenas HMACs de prefixos de palavras, nunca texto em claro. */
@Entity(
    tableName = "search_tokens",
    primaryKeys = ["historyId", "token"],
    foreignKeys = [
        ForeignKey(
            entity = HistoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["historyId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("token")]
)
data class SearchTokenEntity(
    val historyId: Long,
    val token: String,
)

data class AppUsage(
    @ColumnInfo(name = "packageName") val packageName: String,
    @ColumnInfo(name = "applicationName") val applicationName: String?,
    @ColumnInfo(name = "total") val total: Int,
)
