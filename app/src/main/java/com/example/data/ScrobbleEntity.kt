package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ScrobbleDto

@Entity(tableName = "scrobbles")
data class ScrobbleEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val artist: String,
  val album: String,
  val appName: String,
  val packageName: String,
  val durationMs: Long,
  val playedAtMs: Long = System.currentTimeMillis(),
) {
  fun toDto(): ScrobbleDto = ScrobbleDto(
    id = id,
    title = title,
    artist = artist,
    album = album,
    appName = appName,
    packageName = packageName,
    durationMs = durationMs,
    playedAtMs = playedAtMs,
  )
}
