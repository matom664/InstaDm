# Instagram WebView Wrapper

This repository contains a production-oriented Android app written in Kotlin with Jetpack Compose, AndroidX WebKit, and the platform WebView. It loads the normal Instagram web experience, preserves the WebView session through normal cookies and storage, blocks Instagram Reels at both the native navigation layer and the WebView DOM layer, and makes a best-effort attempt to restore the last successful Instagram page using WebView cache when the device is offline.

## What it does

- Loads `https://www.instagram.com/` in a WebView.
- Lets the user sign in through Instagram's own web UI.
- Keeps Home, Direct, profiles, posts, stories, and Explore usable.
- Includes a blocking mode menu with **Normal (default)** and **Block Reels**.
- Persists the last successfully viewed allowed Instagram URL.
- Restores cached content when offline if WebView has cached it.
- Exposes simple privacy controls to clear cache or clear the Instagram session.

## Architecture

- `MainActivity` hosts the Compose shell, back navigation, and settings actions.
- `InstagramWebView` owns the stable WebView instance, its clients, cache mode, and pull-to-refresh.
- `InstagramUrlFilter` centralizes Instagram URL validation and optional Reels blocking.
- `ReelsBlocker` injects lightweight JavaScript for optional Reels blocking.
- `ConnectivityMonitor` uses `ConnectivityManager.NetworkCallback` to drive online/offline state.
- `LastViewedRepository` stores only the last allowed URL in DataStore Preferences.
- `OfflineScreen` shows a best-effort offline/error state when WebView cannot render cached content.

## Build

Open the project in Android Studio and sync Gradle, or build from the command line after the wrapper is generated:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

The app uses:

- `minSdk` 26
- Kotlin 2.0.21
- Android Gradle Plugin 8.7.3
- Jetpack Compose BOM 2024.12.01

## How authentication works

Authentication happens entirely inside Instagram's own website in WebView. The app does not ask for credentials, read login forms, extract cookies, extract tokens, or use any Instagram private API. Session persistence comes from normal WebView cookie and storage behavior.

## Blocking modes

The settings menu provides these blocking modes:

1. **Normal (default)**: no route or DOM blocking.
2. **Block Reels**: enables Reels URL and DOM blocking.

When **Block Reels** is selected, blocking has two layers:

1. Native URL filtering in `WebViewClient.shouldOverrideUrlLoading` and `doUpdateVisitedHistory`.
2. Best-effort JavaScript injection that prevents clicks on Reels links, scans newly added DOM nodes, and wraps SPA history methods.

This means direct navigations to `/reels`, `/reels/`, or `/reels/*` are blocked even if Instagram changes its client-side navigation behavior.

## Offline caching

Offline behavior is best-effort only. The app does not create an offline Instagram client or store Instagram content itself. When the device loses connectivity, the WebView cache mode switches to `LOAD_CACHE_ELSE_NETWORK` and the app keeps the current page visible. If WebView has cached enough assets, the last page may still render. If not, the app shows a simple offline screen with retry.

## Privacy model

- No private API usage.
- No scraping.
- No credential interception.
- No custom CA or SSL bypass.
- No cookie logging.
- No message archiving.

The settings menu provides blocking mode selection plus two explicit privacy actions:

- Clear cached Instagram data: clears WebView cache, history, and local storage, but not cookies.
- Clear Instagram session: clears WebView cookies and session data so the user must sign in again.

## Limitations

- WebView offline caching is not guaranteed to preserve every Instagram page.
- Direct Messages may or may not render offline depending on what WebView cached earlier.
- Instagram's DOM and route structure can change, so the JavaScript blocker is best-effort. The native URL filter remains the enforcement layer.
- External links are handled conservatively and may open in the browser or be ignored if the scheme is unsafe.

## Tests

URL filtering unit tests live under `app/src/test/java/com/example/instagramwrapper/InstagramUrlFilterTest.kt` and cover allowed pages, blocked Reels URLs, malformed input, and host/scheme normalization.

## Manual test plan

1. Launch the app and confirm Instagram Home loads.
2. Sign in through the WebView and restart the app to verify session persistence.
3. Open Direct and a normal post or profile.
4. Try to open Reels from the feed and verify that navigation is blocked.
5. Load Instagram online, then relaunch offline and verify cached content or the offline screen.
6. Use Back and confirm WebView history is respected before the Activity closes.
7. Use the settings menu to clear cache and session separately.
