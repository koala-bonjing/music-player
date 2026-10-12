package com.sonny.heartymusic.ui.components.bibble

/**
 * The six primary animation states for the Bibble Companion,
 * plus entrance and dismissal states.
 */
enum class BibbleAnimationState {
    HIDDEN,
    ENTERING,    // Animation 1: pops into view, bounces, waves
    DANCING,     // Animation 2: sways left/right, bounces to rhythm
    HEART,       // Animation 3: heart gesture, floating pink hearts
    SLEEPY,      // Animation 4: sits down, restful slow breathing, zzz
    HAPPY,       // Animation 5: excited hops, claps, confetti/sparkles
    DISCOVERING, // Animation 6: curious tilt, attempts to catch floating note
    LEAVING      // Exit transition
}

/**
 * User action triggers for interacting with Bibble.
 */
enum class BibbleUserAction {
    DANCE,
    LOVE,
    CATCH_NOTE,
    NAP,
    DISMISS
}

/**
 * Model representing a cozy speech bubble from Bibble.
 */
data class BibbleSpeech(
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
