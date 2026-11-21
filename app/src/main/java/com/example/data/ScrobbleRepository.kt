package com.example.data

import com.example.model.ScrobbleDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ScrobbleRepository(private val dao: ScrobbleDao) {

  val recentScrobbles: Flow<List<ScrobbleDto>> =
    dao.observeRecentScrobbles().map { list -> list.map { it.toDto() } }

  suspend fun recordScrobbleIfNew(
    title: String,
    artist: String,
    album: String,
    appName: String,
    packageName: String,
    durationMs: Long,
  ): Boolean {
    if (title.isBlank()) return false
    val now = System.currentTimeMillis()
    val latest = dao.getLatestScrobble()
    if (
      latest != null &&
      latest.title.equals(title, ignoreCase = true) &&
      latest.artist.equals(artist, ignoreCase = true) &&
      (now - latest.playedAtMs) < 90_000L
    ) {
      return false
    }
    dao.insertScrobble(
      ScrobbleEntity(
        title = title.trim(),
        artist = artist.trim(),
        album = album.trim(),
        appName = appName.trim(),
        packageName = packageName,
        durationMs = durationMs,
        playedAtMs = now,
      )
    )
    return true
  }

  suspend fun getRecentSnapshot(limit: Int = 40): List<ScrobbleDto> =
    dao.getRecentScrobbles(limit).map { it.toDto() }

  suspend fun exportAsJson(): String {
    val all = dao.getAllScrobbles().map { it.toDto() }
    return Json { prettyPrint = true }.encodeToString(all)
  }

  suspend fun exportAsCsv(): String {
    val all = dao.getAllScrobbles()
    val sb = StringBuilder()
    sb.appendLine("id,title,artist,album,app_name,package_name,duration_ms,played_at_ms")
    for (item in all) {
      sb.append(item.id).append(',')
        .append(csvEscape(item.title)).append(',')
        .append(csvEscape(item.artist)).append(',')
        .append(csvEscape(item.album)).append(',')
        .append(csvEscape(item.appName)).append(',')
        .append(csvEscape(item.packageName)).append(',')
        .append(item.durationMs).append(',')
        .append(item.playedAtMs).append('\n')
    }
    return sb.toString()
  }

  suspend fun clearHistory() {
    dao.clearAll()
  }

  private fun csvEscape(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return "\"$escaped\""
  }
}
