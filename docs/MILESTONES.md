# v0.20 Milestones & Progress Tracker

Companion to [UPDATE_PLAN.md](UPDATE_PLAN.md). Issue IDs (S1, U4, A3 …) refer to the audit tables there.

**How to use:** work milestones in order (each de-risks the next). Tick checkboxes as tasks land; update the status table. Every milestone ends with the app building, installing, and passing tests.

| Milestone | Theme | Status |
|-----------|-------|--------|
| M1 | Listener & service stability | 🟨 Code complete (2026-09-29) — on-device acceptance pending |
| M2 | Performance & memory | 🟨 Code complete (2026-09-29) — on-device acceptance pending |
| M3 | Dependencies, ads & consent | 🟨 Code complete (2026-09-29) — on-device acceptance pending |
| M4 | Design system & theming | 🟨 Code complete (2026-09-29) — on-device acceptance pending |
| M5 | Navigation redesign (bottom nav + screens) | 🟨 Code complete (2026-09-29) — on-device acceptance pending |
| M6 | Onboarding, retention & engagement | ⬜ Not started |
| M7 | Hardening, QA & release | ⬜ Not started |

Status legend: ⬜ Not started · 🟨 In progress · ✅ Done

---

## M1 — Listener & service stability
*The capture pipeline must survive anything. Fixes: S1–S4, S11 (service part), S14.*

- [x] Remove `runBlocking` from `NotificationListener.onCreate`; prime allowed-apps/tracking/retention/filters asynchronously — capture coroutines `await` a `prefsPrimed` deferred so startup notifications are never dropped (S1)
- [x] Add `CoroutineExceptionHandler` to `serviceScope`; wrap `hasRecentDuplicate` / `insertApp` / retention delete in try/catch with Crashlytics non-fatal recording via new `utils/CrashReporter` (S2)
- [x] Override `onListenerConnected` / `onListenerDisconnected`; live `NotificationListener.isConnected` + persisted `listener_connected` DataStore key; `requestRebind` on disconnect + `service/ListenerReconnector` escalating to component toggle from `MainActivity.onResume` (S3)
- [x] Cache notification-access check; hot path now uses binding state instead of `Settings.Secure` (S4)
- [x] Align `SettingState.userToggleTracking` default with DataStore default (S14)
- [x] Add Crashlytics custom keys: listener_connected, tracking_enabled, allowed_app_count (S11)
- [x] Unit tests: `RetentionGateTest` (5) + `ListenerReconnectorTest` (6); all 53 unit tests green

**Acceptance:** kill the listener via `adb shell am crash` / revoke-regrant access → capture resumes without app open; no `runBlocking` anywhere in service; forced SQLite failure logs a non-fatal instead of crashing.
- [x] No `runBlocking` in service (verified by grep)
- [ ] On-device: revoke → regrant notification access, confirm capture resumes and Crashlytics key `listener_connected` flips (needs a physical device / emulator)
- [ ] On-device: force-stop app, post notifications from a tracked app, confirm capture resumes after `MainActivity.onResume`

## M2 — Performance & memory
*No main-thread I/O, bounded memory. Fixes: S5–S8, S10, S13.*

- [x] Central `core/apps/AppInfoCache` (byte-bounded LRU of 96 px `ImageBitmap`s + label cache + negative cache, IO loading); adopted in Mapper, SelectApp, HistoryUiComponents (`HistoryAppIcon(packageName)`), conversation controller, and the listener's app-label lookup (S6, S7)
- [x] `NotificationModel` / `ConversationModel` / `ConversationDetailUiState`: `Drawable appIcon` removed; icons resolve at render time from the cache (S7)
- [x] Per-app latest-notification mapping moved to `Dispatchers.IO` (S5)
- [x] `DateTimeFormatter` hoisted to `ReadableTimeFormatter` (one instance per locale) (S7)
- [x] History list capped at `MAX_LOADED_HISTORY = 3000` rows (`HistoryLoadState.isCapped` exposed for the M5 UI hint); `releaseAppHistory` / `releaseConversationDetail` evict state — and the conversation detail's live observer coroutine — when their screens leave composition (S8)
- [x] Atomic `UserPreferences.incrementLaunchCount()`; counted once per ViewModel lifetime so rotation / theme change no longer counts (S10) — *rating threshold (3) and flow unchanged*
- [x] Room schemas exported to `app/schemas` (`4.json`); `room-testing` dependency + androidTest assets wired; missing 2→3 migration test added in the existing raw-SQL style (S13). Note: historical 1–3 schema JSONs don't exist, so `MigrationTestHelper` becomes usable from 4→5 onward.

**Acceptance:** no dropped frames scrolling 1k+ history rows on a mid-range device; StrictMode clean for disk/network on main; rotation does not increment launch count.
- [x] Unit: `ReadableTimeFormatterTest` (3); 56 unit tests green; androidTest sources compile
- [ ] On-device: run instrumented migration tests (`connectedDebugAndroidTest`), now 4 migration/DAO tests
- [ ] On-device: scroll 1k+ rows with GPU profiling; rotate on Home and confirm launch count unchanged

## M3 — Dependencies, ads & consent
*Modern stack, policy-safe monetization. Fixes: S9, A2, A3.*

- [x] Compose BOM 2024.09.00 → **2026.06.01** (Compose 1.11.4, Material3 1.4.0); pinned `ui-text-google-fonts` / `animation` overrides removed; all deps moved into the version catalog (A3). *Decision:* BOM 2026.08.00+ (Compose 1.12) requires compileSdk 37 → AGP 9.x → Gradle 9.6; that toolchain jump is tracked separately (see FUTURE_FEATURES → Platform).
- [x] Removed M2 `material` (pull-to-refresh migrated to M3 `PullToRefreshBox` in History, Apps tab and AppHistory — pulled forward from M5), accompanist-systemuicontroller, gson, duplicate Room/animation declarations; added explicit `material-icons-core` (previously transitive via M2) (A2)
- [x] `MobileAds.initialize()` on `Dispatchers.IO`, gated on consent, guarded so it runs once (S9)
- [x] UMP 4.0.0 consent flow (`ads/AdsConsentManager`): `requestConsentInfoUpdate` + `loadAndShowConsentFormIfRequired` on every launch for non-premium users; "Privacy options" row in Settings appears when UMP reports it as required (S9)
- [x] `CollapsibleAdBanner` renders nothing until `adsReady`; AdView pauses/resumes with lifecycle and is destroyed in `onDispose` (S9)
- [x] Debug builds use Google's collapsible-banner test unit (S9)
- [x] Ad unit ID in `BuildConfig.BANNER_AD_UNIT_ID` per build type
- [x] Bonus: play-services-ads 24.9.0 → 25.5.0; `kotlinOptions.jvmTarget` → `compilerOptions` DSL; `lifecycle-runtime-compose` added

**Acceptance:** release build compiles minified; ads load with consent flow on a fresh EEA-locale install; LeakCanary (debug) shows no AdView/Activity leaks.
- [x] Debug + release Kotlin compile green; 56 unit tests green
- [ ] On-device: fresh install with an EEA test geography (UMP `ConsentDebugSettings`) → consent form shows before any ad; "Privacy options" row appears in Settings
- [ ] On-device: navigate Home ↔ History repeatedly, confirm no AdView leak (LeakCanary or `dumpsys meminfo`)
- [ ] Known deprecation to revisit: `AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize` (warning only)

## M4 — Design system & theming
*One visual language before screens are rebuilt. Fixes: U2, U3, U6, U7, A4 (partial).*

- [x] `core-splashscreen` 1.0.1 with `Theme.NotificationHistory.Starting` (launcher foreground icon on the surface color); base theme now light/dark via `values-night/themes.xml` with `window_background` matching the Compose surface; template purple/teal colors removed (U3)
- [x] In-app theme setting System / Light / Dark: `ThemeMode` enum, `theme_mode` DataStore key, `MainViewModel.themeMode` (eager), `AppTheme(darkTheme = themeMode.isDark())`, system-bar icon contrast follows the in-app choice; "Appearance → Theme" row + radio dialog in Settings (U3)
- [x] Hardcoded colors purged: SettingScreen warning card → `tertiaryContainer`, SelectApp/About `Color.Gray` → `onSurfaceVariant`, SelectApp search highlight now shares `highlightedText()` (theme roles). Only `Color(0x…)` literals left are the token definitions in `Color.kt` (U2)
- [x] Deleted medium/high-contrast schemes, `ColorFamily`, `unspecified_scheme`; `Color.kt` 219 → 78 lines (U6)
- [x] Component kit in `presentation/components/`: `SectionHeader` (adopted in Settings + About), `EmptyState` (`HistoryEmptyState` now delegates to it), `AppListItem` (adopted in History → Apps tab), `StatusCard` + `StatCard` (ready for the M5 Home rebuild) (U7)
- [x] ~~RateUsCard restyle~~ — **dropped by product decision (2026-09-29): RateUsCard is frozen; no visual, string or logic changes.** Its hardcoded strings/raw `TextStyle` stay as-is.
- [x] Hardcoded UI strings extracted in History, AppHistory, Conversations, SelectApp, DeleteConfirmationDialog, NotificationDetailsDialog, IntentUtil (+ ~45 new `strings.xml` entries); "Choose App" title unified to "Manage Apps" (A4, U7). RateUsCard intentionally excluded.
- [x] Bonus: deprecated `Divider` → `HorizontalDivider`, non-mirrored `ArrowBack`/`KeyboardArrowRight` fixed in About/Settings/History (compiler warnings 24 → 8; remaining are Home/Insights/TabRow, owned by M5)

**Acceptance:** every screen correct in light/dark/dynamic-color; zero literal `Color(0x…)` in screen code; strings lint check passes.
- [x] Compile green; 56 unit tests green; grep confirms no `Color(0x…)`/`Color.Gray` outside `Color.kt` and no hardcoded `Text("…")`/`contentDescription` outside RateUsCard
- [ ] On-device: cold start shows splash with app icon on both light and dark; no white flash before the first Compose frame
- [ ] On-device: Settings → Theme → Dark while the OS is light: every screen (incl. per-app filter warning card, About, Manage Apps highlight) renders dark; status-bar icons flip to light
- [ ] On-device: Android 12+ dynamic color still applies in all three theme modes

## M5 — Navigation redesign
*The visible redesign. Fixes: U1, U4, U5, A5; plan §3.1–3.3.*

- [x] Bottom navigation (`AppNavGraph`): Home / History / Insights / Settings via `NavigationBar`, `navigateToTab()` (single-top, per-tab state save/restore), fade for tab switches vs. slide for pushed screens, ad banner sits above the bar, back arrows removed from the three tab screens (U5)
- [x] Home rebuild: `CaptureStatusCard` (Recording / Paused / Reconnecting / Access needed, with today's saved count), Today stat row (notifications, apps, top app → Insights), Manage-apps card, contextual warnings (no apps selected, battery), Recent preview (last 5 via new `HomeViewModel` + `observeRecentActive`) with "See all". **RateUsCard call, trigger and callbacks unchanged** — only wrapped in a `Box` to keep its 16 dp margin (plan §3.2)
- [x] History simplification: `HistoryView { All, Conversations, Apps }` segmented control replaces TabRow + nested selector; filter bar shared by All/Conversations; capped-list hint shown when `isCapped` (U4)
- [x] Migrate pull-to-refresh to M3 `PullToRefreshBox` in History + AppHistory; drop M2 dep (U1) — *done early in M3*
- [x] URI-encode `app_notifications/{packageName}` and `SettingScreen/{packageName}` route args (A5)
- [x] Naming consistency: "Manage Apps" everywhere; search placeholders/content descriptions unified (done across M4/M5) (U7)
- [x] Settings tab: "History retention" row + dialog (7/14/30/90 days/Keep forever) wired to `updateHistoryRetentionDays`; theme picker (M4)

**Acceptance:** all existing functionality reachable in ≤ same tap count; History/Conversations/Apps in one tap from anywhere; navigation state survives process death.
- [x] Compile green; 56 unit tests green; `RateUsCard.kt` untouched (git), Home call site diff = indentation only
- [ ] On-device: tab switching keeps scroll/filter state per tab; back from any tab returns to Home; Insights → top app opens History with that app filter
- [ ] On-device: Home hero shows Recording with today's count; revoke access → "Permission required" with Allow; force-stop listener → "Reconnecting…" then recovers
- [ ] On-device: ad banner renders above the bottom bar without overlapping; no double bottom padding on tab screens

## M6 — Onboarding, retention & engagement
*Reasons to return. Plan §3.4–3.5. Fixes: S12.*

- [ ] 3-step first-run onboarding: value intro → notification access → app selection (common messengers preselected)
- [ ] Tracking-stopped detection + polite self-notification with reconnect deep link
- [ ] Trash auto-purge after 30 days (opportunistic on app open / retention pass) (S12)
- [ ] Empty states with action buttons for history, conversations, insights, trash, search results
- [ ] Static app shortcuts: Search, History, Insights
- [ ] Firebase Analytics events for the new funnel: onboarding_complete, home_open, history_open, insights_open, reconnect_tapped

**Acceptance:** fresh install reaches "recording with apps selected" in < 60 s; disabling notification access produces the reconnect notification; trashed items older than 30 days purge.

## M7 — Hardening, QA & release
*Fixes: A1 (bounded), A6; plan §4.*

- [ ] Split `MainViewModel`: extract History/Trash/AppSelection ViewModels behind existing state shapes (A1 — stop when risk outweighs benefit; log leftovers in FUTURE_FEATURES tech-debt section)
- [ ] Tests: launch-count/rating-trigger unit tests, DAO paging tests, Compose smoke tests for the 4 tabs (A6)
- [ ] Proguard/R8 pass on release build: full manual regression on minified build
- [ ] Manual regression matrix: Android 8 (minSdk 26), 12 (dynamic color), 14/15/16; light/dark; rotation; process death; multi-profile (work apps)
- [ ] versionCode 20 / versionName "0.20"; release notes; Play listing screenshots of new UI
- [ ] Data-safety form re-review (consent/UMP changes)
- [ ] Internal → closed testing → staged rollout 10% / 50% / 100% gated on crash-free ≥ 99.5%

### Pre-release checklist
- [ ] All 42+ unit tests and instrumented migration tests pass
- [ ] Upgrade-in-place test: install v0.19 build with data → update to v0.20 → history, trash, settings, premium all intact
- [ ] Premium flow: purchase, restore on reinstall, ads hidden
- [ ] Rating card: appears at launch #3, all three paths behave exactly as v0.19
- [ ] No StrictMode violations; LeakCanary clean
- [ ] Crashlytics + Analytics events verified in DebugView
