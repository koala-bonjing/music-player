package com.sonny.heartymusic

import com.sonny.heartymusic.data.network.OkHttpDownloader
import com.sonny.heartymusic.data.repository.MusicRepository
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Test
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList

class SearchTest {
    @Test
    fun testSearch() {
        val okHttpClient = OkHttpClient()
        NewPipe.init(OkHttpDownloader(okHttpClient))
        val repo = MusicRepository()
        val result = runBlocking {
            repo.searchSongs("Adele")
        }
        val suggestions = runBlocking {
            ServiceList.YouTube.getSuggestionExtractor().suggestionList("adele")
        }
        org.junit.Assert.assertTrue(result.isSuccess)
        org.junit.Assert.assertTrue(suggestions.isNotEmpty())
    }
}
