# Bibble Companion Asset Directory

Place your generated transparent video or animation assets in this directory (`app/src/main/assets/bibble/`).

## Expected Asset Names
- `entrance.mp4` or `entrance.webm`: Bibble pops into view, bounces, waves with pastel sparkles (3-4s).
- `dance.mp4` or `dance.webm`: Looping dance swaying left & right to rhythm (4-6s loop).
- `heart.mp4` or `heart.webm`: Heart gesture with floating pink/purple heart particles (3-4s).
- `sleepy.mp4` or `sleepy.webm`: Resting, peaceful sleepy pose for paused music with drooped tuft (4-5s loop).
- `happy.mp4` or `happy.webm`: Excited hops, clapping, confetti/sparkles for favoriting a song (3-4s).
- `discover.mp4` or `discover.webm`: Playfully tilting head and trying to catch floating musical notes (4-5s loop).
- `bibble-fallback.png`: Transparent high-resolution Bibble character PNG.

The `BibbleAssetManager` will automatically detect any placed files here and use them seamlessly. If any file is not yet present, the app automatically falls back to Compose physics animation with the authentic Bibble character artwork.
