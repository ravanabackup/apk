package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ScrobbleEntity::class], version = 1, exportSchema = false)
abstract class WaveBridgeDatabase : RoomDatabase() {
  abstract fun scrobbleDao(): ScrobbleDao

  companion object {
    @Volatile
    private var INSTANCE: WaveBridgeDatabase? = null

    fun getInstance(context: Context): WaveBridgeDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          WaveBridgeDatabase::class.java,
          "wavebridge.db",
        ).build().also { INSTANCE = it }
      }
    }
  }
}
