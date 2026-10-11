# Project: Hearty Music — personal YT Music-style Android app

## Who I'm working with
Sonny, junior dev. Strong in TypeScript/React/Next.js, new to native Android.
- Keep replies terse. When introducing an Android concept, give a 1-2 line mental model, mapping it to React where it fits (e.g. ViewModel ≈ Zustand store, Composable ≈ component, StateFlow ≈ state).
- Explain the "why" briefly, then fix. Don't dump walls of text.

## Goal
Personal-use app (sideloaded APK, never Play Store). Browse/search music, play it
in the background, show lyrics, favorites, playlists, share songs/playlists.

## Stack (do not change without asking)
- Kotlin + Jetpack Compose + Material 3, single module, MVVM
  (ViewModel + Repository + StateFlow)
- Navigation Compose, Coil for images, DataStore for settings
- Media3 (ExoPlayer + MediaSessionService) for playback
- NewPipeExtractor (via JitPack) for search + audio stream URLs
  - Needs a custom `Downloader` implementation using OkHttp
  - Stream URLs expire: resolve the URL right before playback (ResolvingDataSource), never at queue time
- LRCLIB (https://lrclib.net) for lyrics, synced (LRC) with plain fallback
- Room for local data. NO backend, NO server database in v1.
- DI: manual or Koin. No Hilt.
- Check the latest stable versions of every dependency before adding. Don't guess versions.

## Features (v1 order)
1. Search + play audio
2. Background playback + notification/lock-screen controls
3. Favorites, playlists (Room)
4. Lyrics (synced, auto-scroll)
5. Sharing:
   - Song: Android share sheet with a music.youtube.com link
   - Playlist: export compact share code (gzip + base64 of name + video IDs) or .json file via share sheet; import by paste or open file

## UI / design
Minimal, soft, girly. Lots of whitespace.
- Palette: background #FFF8F6, primary dusty pink #E8A0B4, secondary lavender #CDB4DB, text #3D2C33, subtle surface #FDEDEF
- Rounded corners 20-28dp, soft shadows, no harsh borders
- Rounded friendly font (Nunito or Quicksand via downloadable Google Fonts)
- Big album art on the player screen, simple bottom nav (Home, Search, Library)
- Light theme first; dark theme later

## Rules
- Plan before coding any new feature; list the files you'll touch.
- Small commits, one feature at a time. Stop after each phase so I can test on my phone.
- No `!!`, no hardcoded strings (use strings.xml), no giant composables (split them).
- Handle failures: extractor errors, no internet, expired URL -> retry once, then show a friendly message.
- Don't add libraries I didn't approve.
- Personal-use only. Don't add anything meant for publishing or monetizing.
