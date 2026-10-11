package com.sonny.heartymusic.di

import android.content.Context
import com.sonny.heartymusic.data.network.OkHttpDownloader
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

    val musicRepository: MusicRepository by lazy {
        MusicRepository()
    }

    val musicPlayerManager: MusicPlayerManager by lazy {
        MusicPlayerManager(context.applicationContext, musicRepository)
    }

    val lyricsRepository: com.sonny.heartymusic.data.repository.LyricsRepository by lazy {
        com.sonny.heartymusic.data.repository.LyricsRepository(okHttpClient)
    }
}
