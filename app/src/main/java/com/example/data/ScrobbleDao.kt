package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScrobbleDao {
  @Query("SELECT * FROM scrobbles ORDER BY playedAtMs DESC LIMIT 500")
  fun observeRecentScrobbles(): Flow<List<ScrobbleEntity>>

  @Query("SELECT * FROM scrobbles ORDER BY playedAtMs DESC LIMIT :limit")
  suspend fun getRecentScrobbles(limit: Int = 50): List<ScrobbleEntity>

  @Query("SELECT * FROM scrobbles ORDER BY playedAtMs DESC")
  suspend fun getAllScrobbles(): List<ScrobbleEntity>

  @Query("SELECT * FROM scrobbles ORDER BY playedAtMs DESC LIMIT 1")
  suspend fun getLatestScrobble(): ScrobbleEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertScrobble(entity: ScrobbleEntity): Long

  @Query("DELETE FROM scrobbles")
  suspend fun clearAll()
}
