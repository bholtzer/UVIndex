package com.bihstudio.uvindex.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ─── Entity ─────────────────────────────────────────────────────────────────

@Entity(tableName = "uv_cache")
data class UVCacheEntity(
    @PrimaryKey val id: String,       // "$lat_$lon"
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val currentUV: Double,
    val hourlyJson: String,           // JSON serialised UVHourly list
    val timestamp: Long
)

// ─── DAO ─────────────────────────────────────────────────────────────────────

@Dao
interface UVCacheDao {

    @Query("SELECT * FROM uv_cache WHERE id = :id")
    suspend fun getCache(id: String): UVCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(entity: UVCacheEntity)

    @Query("DELETE FROM uv_cache WHERE timestamp < :cutoff")
    suspend fun deleteOldCache(cutoff: Long)

    @Query("SELECT * FROM uv_cache")
    fun observeAll(): Flow<List<UVCacheEntity>>
}

// ─── Database ─────────────────────────────────────────────────────────────────

@Database(entities = [UVCacheEntity::class], version = 1, exportSchema = false)
abstract class UVDatabase : RoomDatabase() {
    abstract fun uvCacheDao(): UVCacheDao
}
