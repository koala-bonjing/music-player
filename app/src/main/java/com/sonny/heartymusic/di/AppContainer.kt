package com.sonny.heartymusic.di

import android.content.Context
import com.sonny.heartymusic.data.local.HeartyDatabase
import com.sonny.heartymusic.data.network.OkHttpDownloader
import com.sonny.heartymusic.data.repository.LibraryRepository
import com.sonny.heartymusic.data.repository.LyricsRepository
import com.sonny.heartymusic.data.repository.MusicRepository
import com.sonny.heartymusic.player.MusicPlayerManager
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    val downloader: OkHttpDownloader by lazy {
        OkHttpDownloader(okHttpClient)
    }

    val database: HeartyDatabase by lazy {
        HeartyDatabase.getInstance(context)
    }

    val libraryRepository: LibraryRepository by lazy {
        LibraryRepository(database.heartyDao())
    }

    val musicRepository: MusicRepository by lazy {
        MusicRepository()
    }

    val musicPlayerManager: MusicPlayerManager by lazy {
        MusicPlayerManager(context.applicationContext, musicRepository, libraryRepository)
    }

    val lyricsRepository: LyricsRepository by lazy {
        LyricsRepository(okHttpClient)
    }
}
