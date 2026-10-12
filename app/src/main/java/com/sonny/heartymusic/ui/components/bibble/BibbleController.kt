package com.sonny.heartymusic.ui.components.bibble

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Controller managing the Bibble Companion state machine, transitions,
 * speech messages, and integration with music player events.
 */
class BibbleController(
    private val scope: CoroutineScope
) {
    var state by mutableStateOf(BibbleAnimationState.HIDDEN)
        private set

    var speech by mutableStateOf<BibbleSpeech?>(null)
        private set

    private var transitionJob: Job? = null
    private var heartToggle = false

    val isVisible: Boolean
        get() = state != BibbleAnimationState.HIDDEN

    /**
     * Toggles Bibble on or off from the player screen button.
     */
    fun toggleCompanion(isPlaying: Boolean) {
        if (state == BibbleAnimationState.HIDDEN) {
            openCompanion(isPlaying)
        } else {
            // Already visible: trigger playful reaction instead of reopening
            onBibbleTapped(isPlaying)
        }
    }

    /**
     * Entrance animation sequence: pops into view, displays greeting,
     * then smoothly transitions to the current player state (dancing or sleepy).
     */
    fun openCompanion(isPlaying: Boolean) {
        transitionJob?.cancel()
        state = BibbleAnimationState.ENTERING
        setSpeech("Yaaay! Hi bestie! 🌸")

        transitionJob = scope.launch {
            delay(1800)
            if (state == BibbleAnimationState.ENTERING) {
                transitionToRestingState(isPlaying)
            }
        }
    }

    /**
     * Reacts to player play/pause state changes without restarting entrance.
     */
    fun onPlayStateChanged(isPlaying: Boolean) {
        if (!isVisible || state == BibbleAnimationState.ENTERING || state == BibbleAnimationState.LEAVING) return

        // If in transient reaction, let it finish or update resting target
        if (state == BibbleAnimationState.DANCING && !isPlaying) {
            transitionJob?.cancel()
            state = BibbleAnimationState.SLEEPY
            setSpeech("Taking a cozy nap... zzz ☁️")
        } else if (state == BibbleAnimationState.SLEEPY && isPlaying) {
            transitionJob?.cancel()
            state = BibbleAnimationState.DANCING
            setSpeech("Woohoo! Dancing time! 🎵")
        }
    }

    /**
     * Reacts to the user favoriting a song.
     */
    fun onSongFavorited(isPlaying: Boolean) {
        if (!isVisible) return

        transitionJob?.cancel()
        state = BibbleAnimationState.HAPPY
        setSpeech("Added to Liked Songs! Love this so much! 💖")

        transitionJob = scope.launch {
            delay(3200)
            transitionToRestingState(isPlaying)
        }
    }

    /**
     * Direct tap on Bibble character: cycles playful heart / happy reactions.
     */
    fun onBibbleTapped(isPlaying: Boolean) {
        if (!isVisible || state == BibbleAnimationState.LEAVING) return

        transitionJob?.cancel()
        heartToggle = !heartToggle
        if (heartToggle) {
            state = BibbleAnimationState.HEART
            setSpeech("Sending you so much love! 💕")
        } else {
            state = BibbleAnimationState.HAPPY
            setSpeech("Hehehe you're the sweetest! ✨")
        }

        transitionJob = scope.launch {
            delay(3000)
            transitionToRestingState(isPlaying)
        }
    }

    /**
     * Dispatches an explicit interactive action triggered by the user via companion chips.
     */
    fun dispatchAction(action: BibbleUserAction, isPlaying: Boolean) {
        if (!isVisible) return

        transitionJob?.cancel()
        when (action) {
            BibbleUserAction.DANCE -> {
                state = BibbleAnimationState.DANCING
                setSpeech("Let's dance together! 🎵💃")
            }
            BibbleUserAction.LOVE -> {
                state = BibbleAnimationState.HEART
                setSpeech("Sending you lovely hearts! 💖")
                transitionJob = scope.launch {
                    delay(3000)
                    transitionToRestingState(isPlaying)
                }
            }
            BibbleUserAction.CATCH_NOTE -> {
                state = BibbleAnimationState.DISCOVERING
                setSpeech("Look at that shiny note! *hops* ✨")
                transitionJob = scope.launch {
                    delay(3500)
                    transitionToRestingState(isPlaying)
                }
            }
            BibbleUserAction.NAP -> {
                state = BibbleAnimationState.SLEEPY
                setSpeech("Nap time... shhh... zzz ☁️")
            }
            BibbleUserAction.DISMISS -> {
                dismiss()
            }
        }
    }

    /**
     * Dismisses Bibble with a gentle exit animation.
     */
    fun dismiss() {
        if (state == BibbleAnimationState.HIDDEN || state == BibbleAnimationState.LEAVING) return

        transitionJob?.cancel()
        state = BibbleAnimationState.LEAVING
        setSpeech("Bye bye! See you soon! 🌸")

        transitionJob = scope.launch {
            delay(380)
            state = BibbleAnimationState.HIDDEN
            speech = null
        }
    }

    private fun transitionToRestingState(isPlaying: Boolean) {
        state = if (isPlaying) BibbleAnimationState.DANCING else BibbleAnimationState.SLEEPY
    }

    private fun setSpeech(message: String) {
        speech = BibbleSpeech(message)
    }
}

/**
 * Creates and remembers a [BibbleController] instance bound to the composable lifecycle.
 */
@Composable
fun rememberBibbleController(): BibbleController {
    val scope = rememberCoroutineScope()
    return remember { BibbleController(scope) }
}
