package com.keyhistory.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Insert
    suspend fun insert(entity: HistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTokens(tokens: List<SearchTokenEntity>)

    @Transaction
    suspend fun insertWithTokens(entity: HistoryEntity, tokens: Collection<String>): Long {
        val id = insert(entity)
        if (tokens.isNotEmpty()) insertTokens(tokens.map { SearchTokenEntity(id, it) })
        return id
    }

    @Query(
        """SELECT * FROM history
           WHERE timestamp BETWEEN :from AND :to
             AND (:packageName IS NULL OR packageName = :packageName)
           ORDER BY timestamp DESC LIMIT :limit"""
    )
    fun observeRange(from: Long, to: Long, packageName: String?, limit: Int): Flow<List<HistoryEntity>>

    @Query(
        """SELECT * FROM history
           WHERE timestamp BETWEEN :from AND :to
             AND (:packageName IS NULL OR packageName = :packageName)
             AND id IN (
                 SELECT historyId FROM search_tokens
                 WHERE token IN (:tokens)
                 GROUP BY historyId
                 HAVING COUNT(DISTINCT token) = :tokenCount
             )
           ORDER BY timestamp DESC LIMIT :limit"""
    )
    fun observeSearch(
        from: Long,
        to: Long,
        packageName: String?,
        tokens: List<String>,
        tokenCount: Int,
        limit: Int,
    ): Flow<List<HistoryEntity>>

    @Query(
        """SELECT * FROM history
           WHERE timestamp BETWEEN :from AND :to
             AND (:packageName IS NULL OR packageName = :packageName)
           ORDER BY timestamp DESC"""
    )
    suspend fun getRange(from: Long, to: Long, packageName: String?): List<HistoryEntity>

    @Query("SELECT * FROM history WHERE id IN (:ids) ORDER BY timestamp DESC")
    suspend fun getByIds(ids: List<Long>): List<HistoryEntity>

    @Query("SELECT * FROM history WHERE id = :id")
    fun observeById(id: Long): Flow<HistoryEntity?>

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    suspend fun getAll(): List<HistoryEntity>

    @Query("SELECT COUNT(*) FROM history")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM history WHERE timestamp >= :from")
    fun observeCountSince(from: Long): Flow<Int>

    @Query("SELECT COUNT(DISTINCT packageName) FROM history WHERE packageName IS NOT NULL")
    fun observeAppCount(): Flow<Int>

    @Query("SELECT MAX(timestamp) FROM history")
    fun observeLastTimestamp(): Flow<Long?>

    @Query("SELECT timestamp FROM history WHERE timestamp >= :from")
    fun observeTimestampsSince(from: Long): Flow<List<Long>>

    @Query(
        """SELECT packageName, MAX(applicationName) AS applicationName, COUNT(*) AS total
           FROM history WHERE packageName IS NOT NULL
           GROUP BY packageName ORDER BY total DESC"""
    )
    fun observeAppUsage(): Flow<List<AppUsage>>

    @Query("SELECT COUNT(*) FROM search_tokens")
    suspend fun tokenCount(): Int

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM history WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>): Int

    @Query("DELETE FROM history WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String): Int

    @Query("DELETE FROM history WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long): Int

    @Query("DELETE FROM history")
    suspend fun deleteAll(): Int
}
