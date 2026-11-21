package com.keyhistory.repository

data class HistoryRecord(
    val id: Long,
    val content: String,
    val packageName: String?,
    val applicationName: String?,
    val timestamp: Long,
    val category: String,
    val encrypted: Boolean,
    val decryptionFailed: Boolean = false,
) {
    val appLabel: String get() = applicationName ?: packageName ?: "Desconhecido"
}

data class HistoryQuery(
    val text: String = "",
    val exactPhrase: Boolean = false,
    val from: Long = 0L,
    val to: Long = Long.MAX_VALUE,
    val packageName: String? = null,
)

data class DashboardStats(
    val total: Int = 0,
    val today: Int = 0,
    val apps: Int = 0,
    val storageBytes: Long = 0,
    val lastActivity: Long? = null,
)
