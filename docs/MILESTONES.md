# v0.20 Milestones & Progress Tracker

Companion to [UPDATE_PLAN.md](UPDATE_PLAN.md). Issue IDs (S1, U4, A3 …) refer to the audit tables there.

**How to use:** work milestones in order (each de-risks the next). Tick checkboxes as tasks land; update the status table. Every milestone ends with the app building, installing, and passing tests.

| Milestone | Theme | Status |
|-----------|-------|--------|
| M1 | Listener & service stability | ⬜ Not started |
| M2 | Performance & memory | ⬜ Not started |
| M3 | Dependencies, ads & consent | ⬜ Not started |
| M4 | Design system & theming | ⬜ Not started |
| M5 | Navigation redesign (bottom nav + screens) | ⬜ Not started |
| M6 | Onboarding, retention & engagement | ⬜ Not started |
| M7 | Hardening, QA & release | ⬜ Not started |

Status legend: ⬜ Not started · 🟨 In progress · ✅ Done

---

## M1 — Listener & service stability
*The capture pipeline must survive anything. Fixes: S1–S4, S11 (service part), S14.*

- [ ] Remove `runBlocking` from `NotificationListener.onCreate`; prime allowed-apps/tracking/retention/filters asynchronously with safe defaults until first emission (S1)
- [ ] Add `CoroutineExceptionHandler` to `serviceScope`; wrap `hasRecentDuplicate` / `insertApp` / retention delete in try/catch with Crashlytics non-fatal recording (S2)
- [ ] Override `onListenerConnected` / `onListenerDisconnected`; persist connected-state; add `requestRebind` + component-toggle recovery (S3)
- [ ] Cache notification-access check; stop reading `Settings.Secure` per posted notification (S4)
- [ ] Align `SettingState.userWantsTracking` default with DataStore default (S14)
- [ ] Add Crashlytics custom keys: listener_connected, tracking_enabled, allowed_app_count (S11)
- [ ] Unit tests for the new capture-gate/caching logic; keep all 42 existing unit tests green

**Acceptance:** kill the listener via `adb shell am crash` / revoke-regrant access → capture resumes without app open; no `runBlocking` anywhere in service; forced SQLite failure logs a non-fatal instead of crashing.

## M2 — Performance & memory
*No main-thread I/O, bounded memory. Fixes: S5–S8, S10, S13.*

- [ ] Central `AppIconCache` (LRU, package → ImageBitmap, background loading); adopt in Mapper, SelectApp, HistoryUiComponents, conversation controller (S6, S7)
- [ ] `NotificationModel`: replace `Drawable appIcon` with package reference resolved via cache (S7)
- [ ] Move per-app latest-notification mapping off Main dispatcher (S5)
- [ ] Hoist `DateTimeFormatter` to a singleton (S7)
- [ ] Bound in-memory history (windowed pages) and evict `appHistoryStates` map entries on back-navigation (S8)
- [ ] Atomic `edit {}` launch-count increment; count once per process launch, not per Activity recreation (S10) — *rating threshold/flow unchanged*
- [ ] Export Room schemas (`room.schemaLocation`); add `MigrationTestHelper` tests incl. missing 2→3 (S13)

**Acceptance:** no dropped frames scrolling 1k+ history rows on a mid-range device; StrictMode clean for disk/network on main; rotation does not increment launch count.

## M3 — Dependencies, ads & consent
*Modern stack, policy-safe monetization. Fixes: S9, A2, A3.*

- [ ] Update Compose BOM to current; remove pinned `ui-text-google-fonts` / `animation` overrides; resolve API changes (A3)
- [ ] Remove: M2 `material` dependency (after M5 pull-refresh migration lands — coordinate), accompanist-systemuicontroller, gson (unused), duplicate Room declarations (A2)
- [ ] `MobileAds.initialize()` on background thread at app start (S9)
- [ ] UMP consent flow (Google User Messaging Platform) before ad requests; EEA-compliant (S9)
- [ ] Destroy AdView in `onDispose`; pause/resume with lifecycle (S9)
- [ ] Debug builds use Google test ad unit IDs (S9)
- [ ] Move hardcoded ad unit ID to BuildConfig

**Acceptance:** release build compiles minified; ads load with consent flow on a fresh EEA-locale install; LeakCanary (debug) shows no AdView/Activity leaks.

## M4 — Design system & theming
*One visual language before screens are rebuilt. Fixes: U2, U3, U6, U7, A4 (partial).*

- [ ] SplashScreen API + DayNight XML theme; purge template `colors.xml` (U3)
- [ ] In-app theme setting: System / Light / Dark (new DataStore key) (U3)
- [ ] Remove every hardcoded `Color(...)` in SettingScreen, SelectAppScreen, AboutScreen, search highlight → `colorScheme` roles (U2)
- [ ] Delete unused contrast schemes, `ColorFamily`, unused imports (U6)
- [ ] Shared component kit: `StatusCard`, `StatCard`, `EmptyState`, `SectionHeader`, `AppListItem` in `presentation/components/` (U7)
- [ ] RateUsCard restyle: typography scale, tokens, strings → `strings.xml` — *logic untouched* (U7, 3.6)
- [ ] Extract all ~60 hardcoded UI strings to `strings.xml` (A4)

**Acceptance:** every screen correct in light/dark/dynamic-color; zero literal `Color(0x…)` in screen code; strings lint check passes.

## M5 — Navigation redesign
*The visible redesign. Fixes: U1, U4, U5, A5; plan §3.1–3.3.*

- [ ] Bottom navigation: Home / History / Insights / Settings; secondary screens as pushed routes (U5)
- [ ] Home rebuild: hero status card, today-at-a-glance stats, recent-notifications preview (last 5), contextual warnings, RateUsCard (plan §3.2)
- [ ] History simplification: one segmented control `All | Conversations | Apps` replacing TabRow + nested selector; unified filter bar (U4)
- [ ] Migrate pull-to-refresh to M3 `PullToRefreshBox` in History + AppHistory; then drop M2 dep (with M3) (U1)
- [ ] URI-encode `app_notifications/{packageName}` route arg (A5)
- [ ] Naming consistency: "Manage apps" everywhere; unified search placeholders/content descriptions (U7)
- [ ] Settings tab: add retention-days control (wire up existing `updateHistoryRetentionDays`) and theme picker

**Acceptance:** all existing functionality reachable in ≤ same tap count; History/Conversations/Apps in one tap from anywhere; navigation state survives process death.

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
