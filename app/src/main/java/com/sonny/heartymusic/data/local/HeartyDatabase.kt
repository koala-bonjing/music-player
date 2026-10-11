package com.sonny.heartymusic.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sonny.heartymusic.data.local.dao.HeartyDao
import com.sonny.heartymusic.data.local.entity.FavoriteSongEntity
import com.sonny.heartymusic.data.local.entity.PlayHistoryEntity
import com.sonny.heartymusic.data.local.entity.PlaylistEntity
import com.sonny.heartymusic.data.local.entity.PlaylistSongCrossRef

@Database(
    entities = [
        FavoriteSongEntity::class,
        PlayHistoryEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HeartyDatabase : RoomDatabase() {
    abstract fun heartyDao(): HeartyDao

    companion object {
        @Volatile
        private var INSTANCE: HeartyDatabase? = null

        fun getInstance(context: Context): HeartyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HeartyDatabase::class.java,
                    "hearty_music.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
