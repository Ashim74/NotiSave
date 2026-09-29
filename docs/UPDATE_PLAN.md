# Notification History — v0.20 Update Plan

**Play Store:** [com.droidnova.notificationhistory](https://play.google.com/store/apps/details?id=com.droidnova.notificationhistory)
**Current release:** v0.19 (versionCode 19) · **Target release:** v0.20 (versionCode 20)
**Plan created:** 2026-09-29

---

## 1. Goals

This update is a stability + redesign release. In priority order:

1. **More stable** — the notification listener must never lose notifications and never crash; the app must feel fast (no main-thread jank).
2. **Better UI design** — one consistent Material 3 design language, proper dark mode everywhere, modern navigation.
3. **Easier and simpler to use** — fewer layers of tabs/modes/filters, clear onboarding, better empty states.
4. **Better retention & more frequent use** — give users reasons to open the app daily and trust that it is working in the background.
5. **Rating card** — keep the current logic (launch-count threshold, 5-star → Play Store, <5 → feedback email, "Rate Later" → reset counter). Only visual polish and string extraction; no behavioral change.

---

## 2. Where we are today (v0.19 audit summary)

### What already works well (keep, don't rewrite)
- Room schema v4 with proper incremental migrations (1→2→3→4) and indexes.
- Cursor-based paging (100/page) for history, conversations, and per-app history.
- Two-layer dedupe (in-memory TTL cache + persistent fingerprint check) — unit tested.
- Conversation detection (shortcutId > title > sender keys) — unit tested.
- Notification content extraction with sanitization — unit tested.
- Insights with adaptive bucketing — unit tested.
- Recoverable trash (soft delete + restore).
- Billing flow with signature verification and auto-reconnection.

### Problems found (what this update fixes)

**Stability / reliability**
| # | Issue | Location |
|---|-------|----------|
| S1 | `runBlocking` with 4 DataStore reads on main thread in listener `onCreate` | `service/DedupeNotificationListener.kt:42-48` |
| S2 | No try/catch around `insertApp` / `hasRecentDuplicate` / retention delete in service coroutine; no `CoroutineExceptionHandler` — a SQLite error crashes the listener | `DedupeNotificationListener.kt:90-139` |
| S3 | `onListenerConnected` / `onListenerDisconnected` not overridden; no `requestRebind` — after system kills the listener, capture silently stops | service |
| S4 | `Settings.Secure` read + string split on the main thread for **every** posted notification | `DedupeNotificationListener.kt:76-78` |
| S5 | Per-app latest-notification collector maps entities → models (PackageManager icon + label lookups) on the **Main** dispatcher | `MainViewModel.kt:201-207` |
| S6 | `getApplicationIcon().toBitmap()` inside composition per row; `toBitmap()` on every recomposition without `remember` | `SelectAppScreen.kt:359-365`, `HistoryUiComponents.kt:115` |
| S7 | No app-icon cache — `pm.getApplicationIcon` per entity; `Drawable` held in every `NotificationModel`; `DateTimeFormatter` created per row | `data/mapper/Mapper.kt` |
| S8 | `_history` list grows unbounded as pages append; `appHistoryStates`/`appHistorySearchJobs` maps never evicted | `MainViewModel.kt` |
| S9 | AdView never destroyed; `MobileAds.initialize()` never called; no UMP/GDPR consent flow (Play policy risk in EEA) | `ads/CollapsibleAdBanner.kt` |
| S10 | Launch count uses read-then-write instead of atomic `edit {}`; increments on every Activity recreation (rotation counts as a "launch") | `MainViewModel.kt:269-274` |
| S11 | Crashlytics is included but never used: no custom keys, no non-fatal logging in catch blocks | project-wide |
| S12 | Trash never auto-purges — deleted items accumulate forever | `TrashScreen` / retention logic |
| S13 | Room schemas not exported (`exportSchema` / `room.schemaLocation` missing) — migration tests hand-build old schemas; no 2→3 migration test | `data/db/AppDatabase.kt` |
| S14 | `SettingState.userWantsTracking` default (`false`) disagrees with DataStore flow default (`true`) | `data_shared/SettingState.kt` |

**UI / design**
| # | Issue | Location |
|---|-------|----------|
| U1 | M2 + M3 mixed: M2 `pullRefresh` in History and AppHistory, rest is M3 | `HistoryScreen.kt`, `AppHistoryScreen.kt` |
| U2 | Hardcoded colors that break dark mode | `SettingScreen.kt:174-192`, `SelectAppScreen.kt`, `AboutScreen.kt` |
| U3 | XML theme is not DayNight; no SplashScreen API; template purple/teal still in `colors.xml` | `res/values/themes.xml` |
| U4 | History screen stacks 3 navigation layers: TabRow (Messages/Apps) + SegmentedButton (All/Conversations) + filter chips | `HistoryScreen.kt` |
| U5 | No bottom navigation — everything funnels through Home cards | `AppNavGraph.kt` |
| U6 | Unused code: medium/high-contrast schemes, `ColorFamily`, accompanist-systemuicontroller imports | `Theme.kt`, `AppNavGraph.kt` |
| U7 | Inconsistent titling ("Choose App" vs "Manage apps"); raw `TextStyle` in RateUsCard | various |

**Architecture / tech debt**
| # | Issue | Location |
|---|-------|----------|
| A1 | One 738-line `MainViewModel` owns everything (history, trash, filters, apps, settings, premium, permissions) | `MainViewModel.kt` |
| A2 | No DI; manual singletons; duplicate Room dependency declarations; unused gson; deprecated accompanist | gradle / project |
| A3 | Compose BOM 2024.09.00 is ~1 year old vs Kotlin 2.2 / AGP 8.10 | `libs.versions.toml` |
| A4 | ~60 hardcoded English strings in composables; single locale only | History, SelectApp, RateUsCard, dialogs |
| A5 | `app_notifications/{packageName}` route arg not URI-encoded | `AppNavGraph.kt` |
| A6 | No tests for ViewModels, DAO paging, rate-card logic; no Compose UI tests | `app/src/test` |

---

## 3. The redesign

### 3.1 New information architecture — bottom navigation

Replace the "Home hub with cards → everything" model with a **4-tab bottom navigation bar**. This is the single biggest driver of "easy to use" and "more frequently used": every core surface is one tap away instead of two-plus.

```
┌────────────────────────────────────────┐
│              (screen content)          │
├────────────────────────────────────────┤
│   Home    History   Insights   Settings│
└────────────────────────────────────────┘
```

- **Home** — status-first dashboard (see 3.2).
- **History** — simplified to **2 chips, not 3 layers**: top-level segmented control `All | Conversations | Apps` replaces TabRow + SegmentedButton nesting; filter bar (app + date) stays below it.
- **Insights** — promoted from a buried card to a first-class tab (retention driver).
- **Settings** — current AppSettings content + new options (theme toggle, retention control, auto-purge).
- Secondary screens (Trash, Manage Apps, per-app filters, About, conversation detail) remain pushed routes without the bottom bar or with it hidden.

### 3.2 Home redesign — "is it working?" at a glance

Users of a notification-history app have one core anxiety: *is it still recording?* Home becomes a status dashboard:

1. **Hero status card** — big, unambiguous: `● Recording` / `⏸ Paused` / `⚠ Access needed` / `⚠ Reconnecting`, with today's captured count ("214 notifications saved today").
2. **Today at a glance** — mini stat row (today's count, top app, busiest hour) that deep-links into Insights.
3. **Recent notifications preview** — the last 3–5 captured notifications with a "See all" → History. (New: gives instant "it works!" feedback and a reason to open the app.)
4. **Health warnings** — battery optimization, zero apps selected, listener disconnected — only shown when actionable.
5. **RateUsCard** — same trigger logic as today, restyled to match the new design.

### 3.3 Visual design system

- **Pure Material 3**: drop the M2 dependency; migrate pull-to-refresh to M3 `PullToRefreshBox`.
- **Design tokens only**: remove every hardcoded `Color(...)`; the per-app filter warning card, search highlight, and gray texts move to `colorScheme` roles (e.g. `tertiaryContainer` for warnings) so dark mode is correct everywhere.
- **Theme**: DayNight XML theme + SplashScreen API; in-app theme setting (System / Light / Dark) stored in DataStore; keep dynamic color on 12+ with the green seed fallback.
- **Typography**: keep Lato; route every text through `MaterialTheme.typography` (fix RateUsCard's raw `TextStyle`).
- **Motion**: keep 300 ms slide transitions; add fade-through for bottom-nav tab switches.
- **Cleanup**: delete unused contrast schemes, `ColorFamily`, accompanist, template `colors.xml` entries.

### 3.4 Simplicity & onboarding

- **First-run onboarding (3 steps)**: what the app does → grant notification access (existing bottom sheet) → pick apps (preselect common messengers, offer "Select all"). Today a new user lands on Home with everything off and must discover the setup themselves — this is where retention dies.
- **Empty states with actions**: every empty list (history, conversations, insights, trash, search) gets an illustration/icon + one-line explanation + action button.
- **Consistent naming**: "Manage apps" everywhere; unify search placeholders and content descriptions.

### 3.5 Retention & engagement features (in-scope for v0.20)

| Feature | Why it drives retention |
|---------|------------------------|
| **Tracking-stopped detection + gentle notification** ("Notification History was disconnected — tap to reconnect") | Users who silently lose data uninstall; users who trust the app keep it |
| **Recent-notifications preview on Home** | Instant proof of value on every open |
| **Today's stats on Home + Insights tab promotion** | A daily "check my stats" habit loop |
| **Trash auto-purge (30 days) + retention setting UI** (backend already exists — `updateHistoryRetentionDays` is currently unreachable) | User control = trust; free premium hook later (longer retention) |
| **App shortcuts (long-press launcher icon)**: Search, History, Insights | Reduces friction for frequent users |

Deliberately **out of scope** for v0.20 (moved to [FUTURE_FEATURES.md](FUTURE_FEATURES.md)): widgets, daily digest notifications, export/backup, app lock, new premium tiers.

### 3.6 Rating card — kept as-is

Preserved behavior (per current `HomeScreen.kt` + `RateUsCard.kt`):
- Shows on Home when `launchCount >= 3` and `show_us_rate_card` is true.
- 5 stars → Play Store (`IntentUtil.openRateUs`) + permanently hidden.
- <5 stars → "Give Feedback" → support email + counter reset (card returns after 3 launches).
- "Rate Later" → counter reset (card returns after 3 launches).

Allowed changes (non-behavioral):
- Restyle card to the new design system; move hardcoded strings to `strings.xml`.
- Fix the atomic-increment bug (S10) so the *count* is correct — threshold and flow unchanged.

### 3.7 Stability engineering (the invisible half of the update)

- **Listener hardening**: replace `runBlocking` with cached-first async priming; wrap all DB work in try/catch + `CoroutineExceptionHandler` reporting to Crashlytics; override `onListenerConnected`/`onListenerDisconnected`; use the `ComponentName` toggle + `requestRebind` recovery path; move the per-notification permission check off the hot path (cache it, refresh on connect/disconnect).
- **Performance**: central `AppIconCache` (LRU, package → ImageBitmap) used by history, select-app, conversations; icon loading off the main thread; store package name in models instead of `Drawable`; hoist `DateTimeFormatter` to a singleton; cap in-memory history window or migrate to Paging 3.
- **Ads/consent**: `MobileAds.initialize` on background thread; UMP consent flow before ad requests; destroy AdView on dispose; test ad unit IDs in debug builds.
- **Crash intelligence**: Crashlytics custom keys (listener connected, tracking on, allowed-app count) + non-fatal recording in every catch block, so 0.20+ issues are diagnosable.
- **Data safety**: export Room schemas + add `MigrationTestHelper`-based tests including the missing 2→3; align `SettingState` defaults with DataStore defaults.

### 3.8 Tech debt paydown (bounded)

- Update Compose BOM to current, remove pinned overrides, remove M2/accompanist/gson duplicates.
- Split `MainViewModel` incrementally: extract `HistoryViewModel`, `TrashViewModel`, `AppSelectionViewModel` behind the existing state shapes (no big-bang rewrite; screens keep working during the split).
- URI-encode the `app_notifications` route arg.
- Extract all hardcoded strings to `strings.xml` (prepares localization as a future feature).
- No DI framework this release — manual wiring is fine at this scale; a lightweight `AppContainer` object replaces scattered `getInstance()` calls.

---

## 4. Release plan

- **Version:** `versionCode 20`, `versionName "0.20"`, branch `version/0.20`.
- **Milestone order** is dependency-ordered — see [MILESTONES.md](MILESTONES.md). Stability first (it de-risks everything after), then design system, then IA/screens, then retention features, then release hardening.
- **Rollout:** internal testing → closed testing (existing testers) → production staged rollout 10% → 50% → 100%, gated on Crashlytics crash-free rate ≥ 99.5%.
- **Pre-release checklist** lives at the bottom of MILESTONES.md.

## 5. Success metrics (check 2–4 weeks after full rollout)

| Metric | Baseline source | Target |
|--------|-----------------|--------|
| Crash-free sessions | Crashlytics | ≥ 99.5% |
| Day-7 retention | Play Console | +20% relative |
| Median sessions/user/week | Firebase Analytics | ↑ vs 0.19 |
| Play Store rating trend | Play Console | hold or improve; more written reviews via kept rating flow |
| ANR rate | Play Vitals | below Play threshold (main-thread fixes) |
| Uninstall rate | Play Console | ↓ vs 0.19 |
