package com.sonny.heartymusic.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val coverEmoji: String = "💌",
    val songCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
