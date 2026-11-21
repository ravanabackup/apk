package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wavebridge_prefs")

data class BridgeSettings(
  val serverPort: Int = 8765,
  val autoStartServer: Boolean = false,
  val scrobbleDelaySec: Int = 5,
)

class SettingsRepository(private val context: Context) {

  private object Keys {
    val SERVER_PORT = intPreferencesKey("server_port")
    val AUTO_START = booleanPreferencesKey("auto_start")
    val SCROBBLE_DELAY_SEC = intPreferencesKey("scrobble_delay_sec")
  }

  val settingsFlow: Flow<BridgeSettings> = context.dataStore.data.map { prefs ->
    BridgeSettings(
      serverPort = prefs[Keys.SERVER_PORT] ?: 8765,
      autoStartServer = prefs[Keys.AUTO_START] ?: false,
      scrobbleDelaySec = prefs[Keys.SCROBBLE_DELAY_SEC] ?: 5,
    )
  }

  suspend fun getCurrentSettings(): BridgeSettings = settingsFlow.first()

  suspend fun setServerPort(port: Int) {
    val clamped = port.coerceIn(1024, 65535)
    context.dataStore.edit { it[Keys.SERVER_PORT] = clamped }
  }

  suspend fun setAutoStartServer(enabled: Boolean) {
    context.dataStore.edit { it[Keys.AUTO_START] = enabled }
  }

  suspend fun setScrobbleDelaySec(seconds: Int) {
    val clamped = seconds.coerceIn(2, 30)
    context.dataStore.edit { it[Keys.SCROBBLE_DELAY_SEC] = clamped }
  }
}
