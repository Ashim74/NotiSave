# v0.20 Release Notes

## Play Store "What's new" (paste as-is — under Play's 500-character limit)

```
A fresh new design and a more reliable app!

• Bottom navigation: Home, History, Insights and Settings one tap away
• New Home: see that recording is on, today's stats and latest notifications
• Simpler History: All, Conversations and Apps in one tap
• Dark theme and a History retention setting
• Alerts you if recording stops, and reconnects automatically
• Trash auto-cleans after 30 days
• Faster, smoother and more stable
```

## Internal changelog (for the team)

**Stability** — listener no longer blocks the main thread on start; database failures can no longer crash the listener; dead listener bindings are detected and rebound automatically, with a user-facing alert if they stay dead; Crashlytics custom keys and non-fatal reporting.

**Performance** — shared app-icon cache (downscaled, byte-bounded), no Drawables held per row, list mapping off the main thread, bounded in-memory history, released per-screen state.

**Monetization & policy** — Google UMP consent flow (GDPR / US states) before any ad request, `MobileAds.initialize` gated on consent, Privacy options entry in Settings, AdView lifecycle fixed (no leak), test ad unit in debug builds.

**Design** — Material 3 only, splash screen, System/Light/Dark theme, dark-mode fixes on every screen, shared component kit, strings extracted to resources.

**Navigation & engagement** — bottom navigation, status-first Home, single-control History, 3-step onboarding (existing users skip it), launcher shortcuts (Search / History / Insights), actionable empty states, analytics funnel events.

**Unchanged by design** — the rate-us card: same trigger (3rd launch), same flows, same look.

**Build** — versionCode 20 / versionName 0.20; Compose BOM 2026.06.01; play-services-ads 25.5.0; UMP 4.0.0; R8 keeps line numbers for Crashlytics.

## Play Console items to update before rollout

1. **Data safety form** — confirm the ads/consent answers still match (UMP now collects consent; ad SDK behaviour unchanged). No new data types are collected; notification content still never leaves the device.
2. **New permission `POST_NOTIFICATIONS`** — used only for the "Recording stopped" alert; no declaration form needed, but mention it if reviewers ask.
3. **`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`** — unchanged from 0.19, but it is a policy-reviewed permission; keep the justification ready ("notification listener must run continuously to record history").
4. **Screenshots** — the whole UI changed; refresh phone screenshots (Home, History, Insights, Settings, dark mode).
