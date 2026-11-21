package com.keyhistory.repository

import android.content.Context
import com.keyhistory.database.AppUsage
import com.keyhistory.database.HistoryDao
import com.keyhistory.database.HistoryEntity
import com.keyhistory.database.KeyHistoryDatabase
import com.keyhistory.security.CryptoManager
import com.keyhistory.utils.SearchTokenizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class HistoryRepository(
    private val context: Context,
    private val dao: HistoryDao,
    private val crypto: CryptoManager,
) {

    suspend fun save(
        content: String,
        packageName: String?,
        applicationName: String?,
        category: String,
        timestamp: Long = System.currentTimeMillis(),
    ): Long = withContext(Dispatchers.IO) {
        val tokens = SearchTokenizer.indexTerms(content).map { crypto.blindToken(it) }
        val entity = HistoryEntity(
            content = crypto.encrypt(content),
            packageName = packageName,
            applicationName = applicationName,
            timestamp = timestamp,
            category = category,
            encrypted = true,
        )
        dao.insertWithTokens(entity, tokens)
    }

    fun observe(query: HistoryQuery, limit: Int = 500): Flow<List<HistoryRecord>> {
        val terms = SearchTokenizer.queryTerms(query.text)
        val source = if (terms.isEmpty()) {
            dao.observeRange(query.from, query.to, query.packageName, if (query.text.isBlank()) limit else limit * 4)
        } else {
            val tokens = terms.map { crypto.blindToken(it) }
            dao.observeSearch(query.from, query.to, query.packageName, tokens, tokens.size, limit)
        }
        return source.map { list ->
            val records = list.map { decryptRecord(it) }
            if (query.text.isBlank()) records
            else records.filter { SearchTokenizer.matches(it.content, query.text, query.exactPhrase) }.take(limit)
        }.flowOn(Dispatchers.Default)
    }

    fun observeRecord(id: Long): Flow<HistoryRecord?> =
        dao.observeById(id).map { it?.let(::decryptRecord) }.flowOn(Dispatchers.Default)

    suspend fun recordsForExport(ids: List<Long>?, from: Long, to: Long): List<HistoryRecord> =
        withContext(Dispatchers.IO) {
            val entities = if (ids != null) ids.chunked(500).flatMap { dao.getByIds(it) } else dao.getRange(from, to, null)
            entities.map { decryptRecord(it) }.filter { !it.decryptionFailed }
        }

    fun observeCount() = dao.observeCount()
    fun observeCountSince(from: Long) = dao.observeCountSince(from)
    fun observeAppCount() = dao.observeAppCount()
    fun observeLastTimestamp() = dao.observeLastTimestamp()
    fun observeTimestampsSince(from: Long) = dao.observeTimestampsSince(from)
    fun observeAppUsage(): Flow<List<AppUsage>> = dao.observeAppUsage()

    fun storageBytes(): Long {
        val db = context.getDatabasePath(KeyHistoryDatabase.NAME)
        return listOf(db, java.io.File(db.path + "-wal"), java.io.File(db.path + "-shm"))
            .filter { it.exists() }.sumOf { it.length() }
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) { dao.deleteById(id) }
    suspend fun delete(ids: Collection<Long>) = withContext(Dispatchers.IO) {
        ids.toList().chunked(500).sumOf { dao.deleteByIds(it) }
    }
    suspend fun deleteByPackage(pkg: String) = withContext(Dispatchers.IO) { dao.deleteByPackage(pkg) }
    suspend fun deleteOlderThan(ts: Long) = withContext(Dispatchers.IO) { dao.deleteOlderThan(ts) }
    suspend fun deleteAll() = withContext(Dispatchers.IO) { dao.deleteAll() }

    private fun decryptRecord(e: HistoryEntity): HistoryRecord {
        var failed = false
        val text = if (e.encrypted) {
            runCatching { crypto.decrypt(e.content) }.getOrElse { failed = true; "[Conteúdo indisponível: falha na descriptografia]" }
        } else e.content
        return HistoryRecord(e.id, text, e.packageName, e.applicationName, e.timestamp, e.category, e.encrypted, failed)
    }
}
