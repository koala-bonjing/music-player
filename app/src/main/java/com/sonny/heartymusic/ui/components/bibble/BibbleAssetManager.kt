package com.sonny.heartymusic.ui.components.bibble

import android.content.Context
import com.sonny.heartymusic.ui.screens.home.BIBBLE_AVATAR_URL

/**
 * Centralized asset mapping for the Bibble Companion.
 *
 * This allows replacing animations later with transparent WebM, MP4, or Lottie
 * without rewriting the component logic.
 *
 * Suggested asset placement in android assets folder (app/src/main/assets/bibble/):
 * - assets/bibble/entrance.webm (or .mp4)
 * - assets/bibble/dance.webm
 * - assets/bibble/heart.webm
 * - assets/bibble/sleepy.webm
 * - assets/bibble/happy.webm
 * - assets/bibble/discover.webm
 * - assets/bibble/bibble-fallback.png
 *
 * If video assets are missing, the system gracefully falls back to the authentic
 * high-resolution character image [BIBBLE_AVATAR_URL] with dynamic Compose
 * physics animations (bouncing, swaying, breathing, particle canvas).
 */
object BibbleAssetManager {

    /**
     * Default character image URL for authentic Barbie: Fairytopia Bibble.
     */
    const val DEFAULT_CHARACTER_URL = BIBBLE_AVATAR_URL

    /**
     * Map of states to relative asset file paths.
     */
    private val ASSET_MAP = mapOf(
        BibbleAnimationState.ENTERING to "bibble/entrance.mp4",
        BibbleAnimationState.DANCING to "bibble/dance.mp4",
        BibbleAnimationState.HEART to "bibble/heart.mp4",
        BibbleAnimationState.SLEEPY to "bibble/sleepy.mp4",
        BibbleAnimationState.HAPPY to "bibble/happy.mp4",
        BibbleAnimationState.DISCOVERING to "bibble/discover.mp4"
    )

    /**
     * Checks if a local video asset exists in the app's assets directory.
     * Never throws exceptions.
     */
    fun hasVideoAsset(context: Context, state: BibbleAnimationState): Boolean {
        val path = ASSET_MAP[state] ?: return false
        return try {
            val fileName = path.substringAfterLast("/")
            val dir = path.substringBeforeLast("/", "")
            val list = context.assets.list(dir)
            list?.contains(fileName) == true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Returns the asset path for a state, if any.
     */
    fun getAssetPath(state: BibbleAnimationState): String? {
        return ASSET_MAP[state]
    }
}
