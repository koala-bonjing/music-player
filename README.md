# Hearty Music (music-player)

Personal YouTube Music-style Android app built with Jetpack Compose, Material 3, and Media3.

## Features
- **YouTube Music Search & Stream:** Powered by NewPipeExtractor with Just-In-Time audio stream URL resolution.
- **Background Playback & Media Notification:** AndroidX Media3 (`MediaSessionService` + `ExoPlayer`) with lock-screen and notification controls.
- **Girly Pastel Theme:** Soft aesthetic with Google Quicksand typography and Material 3 components.
- **Bottom Navigation:** Quick access to Home, Search, and Library.

## Tech Stack
- **UI:** Jetpack Compose + Material 3 + Navigation Compose
- **Architecture:** MVVM + StateFlow + Manual DI (`AppContainer`)
- **Audio:** AndroidX Media3 1.5.1 (`ExoPlayer`, `MediaSessionService`, `MediaController`)
- **Networking & Extraction:** OkHttp 4.12.0 + NewPipeExtractor
- **Image Loading:** Coil Compose
