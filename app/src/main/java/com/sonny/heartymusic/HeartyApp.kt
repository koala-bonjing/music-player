package com.sonny.heartymusic

import android.app.Application
import com.sonny.heartymusic.di.AppContainer
import org.schabi.newpipe.extractor.NewPipe

class HeartyApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NewPipe.init(container.downloader)
    }
}
