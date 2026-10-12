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
Minimal, soft, girly. Lots of whitespace. Screens follow the Stitch "Cozy Moments" designs.
- Palette: source of truth is `ui/theme/Color.kt` (Cozy* tokens): bg #FFFDF9, card #FFF2F5, accent #E87A90, accent light #FFB8C6, text #3D2B35, muted #8C7480. Use tokens, never raw hex in screens.
- Rounded corners 20-28dp, soft shadows, no harsh borders
- Font: Quicksand (downloadable Google Fonts, `ui/theme/Type.kt`)
- Big album art on the player screen, simple bottom nav (Home, Search, Library)
- Light theme first; dark theme later

## Rules
- Plan before coding any new feature; list the files you'll touch.
- Small commits, one feature at a time. Stop after each phase so I can test on my phone.
- No `!!`, no hardcoded strings (use strings.xml), no giant composables (split them).
- Handle failures: extractor errors, no internet, expired URL -> retry once, then show a friendly message.
- Don't add libraries I didn't approve.
- Personal-use only. Don't add anything meant for publishing or monetizing.

## Status
Done: Phase 0 scaffold, 1 search+play, 2 background playback, 3 Room (favorites, history, playlist create), lyrics (synced + karaoke view).
Next / known gaps:
- No real queue: `MusicPlayerManager.play()` uses `setMediaItem` (single item) -> no auto-advance, notification next/prev and shuffle do nothing.
- Playlists: can't add songs; `getSongsForPlaylist` joins `favorite_songs` (wrong table); `songCount` never updated.
- Sharing: song uses youtu.be (spec: music.youtube.com); playlist share code/import not built.
- Placeholder UI (fake BPM/genre tags, "Cozy Buds Pro", non-filtering chips), hardcoded strings, oversized composables (PlayerScreen, LibraryScreen, HomeScreen).

## Build & run (Windows)
- JDK: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"`
- Build + install on USB phone: `.\gradlew.bat installDebug`
- adb: `C:\Users\sonny\AppData\Local\Android\Sdk\platform-tools\adb.exe`
