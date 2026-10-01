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
| M6 | Onboarding, retention & engagement | 🟨 Code complete (2026-09-29) — on-device acceptance pending |
| M7 | Hardening, QA & release | 🟨 Code complete (2026-09-29) — device regression + Play Console steps pending |
| M8 | App lock — password/PIN, change & forgot-password recovery (branch `version/0.21`) | 🟨 Code complete (2026-09-30) — on-device acceptance pending |

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

- [x] 3-step `OnboardingScreen` (intro → notification access with auto-advance on grant → app picker with installed messengers pre-selected); gated by `onboarding_complete` DataStore flag whose fallback treats upgraders (apps already selected or launch count > 1) as onboarded; splash stays up until the flag is read; POST_NOTIFICATIONS asked once at the end (API 33+)
- [x] Tracking-stopped alert: `ListenerAlerts` posts a "Recording stopped — tap to reconnect" notification 45 s after a disconnect the rebind didn't fix (only if access is still granted and tracking is on); cancelled on reconnect; tap deep-links via `LaunchAction.Reconnect`
- [x] Trash auto-purge after `TRASH_RETENTION_DAYS = 30` on app open (`refreshHistory`) and in the listener retention pass — runs even when history is "keep forever" (S12)
- [x] Actionable `EmptyState`s: History All/Apps (Clear search / Clear filters / Manage apps), Conversations (clear search/filters), Trash + Insights (explanatory description)
- [x] Static launcher shortcuts (`res/xml/shortcuts.xml`): Search (opens History with the search field focused), History, Insights → routed through `LaunchAction`
- [x] `utils/Analytics`: onboarding_complete, home_open, history_open, insights_open, reconnect_tapped, listener_alert_shown

**Acceptance:** fresh install reaches "recording with apps selected" in < 60 s; disabling notification access produces the reconnect notification; trashed items older than 30 days purge.
- [x] Compile green; 56 unit tests green; RateUsCard untouched
- [ ] On-device: fresh install → onboarding shows; upgrade from 0.19 data → onboarding does NOT show
- [ ] On-device: kill the listener binding (not the permission) → alert appears after ~45 s; tap → app opens and Home shows Reconnecting → Recording
- [ ] On-device: long-press launcher icon → 3 shortcuts; Search shortcut lands on History with keyboard open
- [ ] On-device: seed a trashed row with `trashedAt` 31 days ago → gone after next app open

## M7 — Hardening, QA & release
*Fixes: A1 (bounded), A6; plan §4.*

- [x] `MainViewModel` split — **stopped by the risk rule**: Home moved to `HomeViewModel` in M5; History/Trash/AppSelection share invalidation state and need a repository layer + device regression first. Plan recorded in FUTURE_FEATURES → Platform (A1)
- [x] Tests (A6): unit `OnboardingResolutionTest` (5, upgrade-safety of onboarding); instrumented `NotificationDaoTest` (5: cursor paging with tied timestamps, filters, trash purge, retention vs trash, recent preview) and `ComponentSmokeTest` (3: EmptyState action, StatusCard dark, StatCard). Rate-trigger test intentionally skipped — card is frozen (see FUTURE_FEATURES)
- [x] R8: `proguard-rules.pro` keeps line numbers for Crashlytics; `minifyReleaseWithR8` passes, mapping generated
- [ ] Manual regression on the minified build — needs a device
- [ ] Manual regression matrix: Android 8 (minSdk 26), 12 (dynamic color), 14/15/16; light/dark; rotation; process death; multi-profile (work apps)
- [x] versionCode 20 / versionName "0.20"; `docs/RELEASE_NOTES_0.20.md` (Play "What's new" 426/500 chars + internal changelog + Play Console checklist)
- [ ] Play listing screenshots of the new UI — needs a device
- [ ] Data-safety form re-review (consent/UMP changes) — Play Console, owner action
- [ ] Internal → closed testing → staged rollout 10% / 50% / 100% gated on crash-free ≥ 99.5%
- [x] Warning cleanup: `ClickableText` → `LinkAnnotation.Url`, Insights auto-mirrored arrow, nav annotation targets. Remaining 5 are pre-existing (deprecated ads/Parcelable APIs, one always-false check)

### Pre-release checklist
- [x] All unit tests pass (61) — [ ] instrumented tests (`connectedDebugAndroidTest`: 4 migration + 5 DAO + 3 Compose) on a device
- [ ] Upgrade-in-place test: install v0.19 build with data → update to v0.20 → history, trash, settings, premium all intact
- [ ] Premium flow: purchase, restore on reinstall, ads hidden
- [ ] Rating card: appears at launch #3, all three paths behave exactly as v0.19
- [ ] No StrictMode violations; LeakCanary clean
- [ ] Crashlytics + Analytics events verified in DebugView

## M8 — App lock (password / PIN, reset & recovery)
*Notification history holds private messages, OTPs and bank alerts. Anyone who picks up an unlocked phone can read all of it. M8 adds an optional lock, the way phones and banking apps do it: open the app → enter your password → see your history. Promoted from FUTURE_FEATURES → v0.21 "App lock". **Free feature.** Built on branch `version/0.21` (off `version/0.20`) so v0.20 stays releasable; RateUsCard untouched.*

**Scope in one line:** set a lock → it is asked on every open → change it or turn it off with the current password → forgot it? prove you own the phone (or use your recovery code) and set a new one → as a last resort, erase history and start fresh.

### Product decisions (as built)
- **Lock types:** 4–8 digit **PIN** (default, number pad) or **password** (6–64 chars). Optional **fingerprint / face unlock** (`BIOMETRIC_STRONG`) on top; the PIN/password is always the fallback and is always required to change settings or turn the lock off (biometric is *not* accepted for turning it off — stricter than first planned).
- **When it asks:** cold start, and on return from background after *Immediately* (default) / 30 s / 1 min / 5 min. Rotation / theme change never re-lock. Returning from a screen the app itself opened (system settings, share sheet, mail, Play billing, battery exemption, screen-lock check) doesn't re-lock if back within 3 min.
- **Where:** Settings → new "Privacy & security" section → "App lock" (own screen). Not in onboarding. No Home hint card (optional item — skipped to keep Home/RateUsCard untouched).
- **Capture keeps running while locked.** `NotificationListener` is not touched.
- **No accounts, no server:** recovery = (1) phone screen lock, (2) recovery code, (3) erase-and-reset.

### Storage & security
- [x] Secret never stored: 16-byte random salt + `PBKDF2WithHmacSHA256` (120k iterations, stored per hash so it can be raised later) in a **separate** DataStore file `datastore/app_lock.preferences_pb` (`core/lock/AppLockStore`, `SecretHasher`)
- [x] Constant-time compare (`MessageDigest.isEqual`); hashing on `Dispatchers.Default`
- [x] Recovery code stored the same way (hash only, own salt)
- [x] `backup_rules.xml` + `data_extraction_rules.xml` exclude the lock file for cloud backup **and device-to-device transfer** (D2D still runs on Android 12+ even with `allowBackup="false"`)
- [x] Biometric via `androidx.biometric` 1.1.0, `BIOMETRIC_STRONG` only, stored as a flag; turning it on first requires a successful prompt. *Keystore key binding (optional) not done* — the PIN stays the root secret, so a newly enrolled fingerprint gains nothing beyond what the phone already allows
- [x] Brute force (`LockoutPolicy`): 5 free tries, then 30 s → 1 min → 5 min → 15 min → 1 h cap, shared by PIN and recovery code; persisted. *Changed from plan:* uses **only `elapsedRealtime`** (no wall clock) — the wall clock can be moved forward to skip a lockout; after a reboot the time since boot is used as a safe lower bound, so a reboot can't skip it either
- [x] `FLAG_SECURE` while the lock is on and "Hide content in Recents" is on (default): blank Recents thumbnail, no screenshots/recording

### Architecture
- [x] `core/lock/AppLockRepository` — enable / verify / change / reset-after-recovery / regenerate code / disable, failure counter + lockout; store behind an `AppLockStore` interface (in-memory fake in tests), injectable clock and dispatcher
- [x] `core/lock/AppLockController` — `gate: StateFlow<Loading | Open | Locked>`. *Changed from plan:* driven by `MainActivity.onStart/onStop` (skipping `isChangingConfigurations`) instead of `ProcessLifecycleOwner` — single-activity app, no `lifecycle-process` dependency needed
- [x] Hand-offs: `MainActivity` overrides `startActivityForResult` (every `startActivity` and activity-result launch goes through it) to mark a hand-off; cleared on `onResume` if the app never left; `BiometricGate` marks its own prompts
- [x] `MainActivity` is now a `FragmentActivity` (+ `fragment-ktx` 1.8.9 pinned — ads only pulled 1.1.0); splash waits for the lock config as well as onboarding
- [x] `AppLockGate` renders `LockScreen` **instead of** `AppNavGraph`; a `SaveableStateHolder` keeps navigation/scroll state across lock → unlock
- [x] Every entry point goes through the gate: launcher, 3 shortcuts and the reconnect alert all create `MainActivity`; `LaunchAction` is only routed when `AppNavGraph` composes, i.e. after unlock. Widgets/QS tile note added to FUTURE_FEATURES (v0.22)
- [x] `ListenerAlerts` text stays generic (unchanged). *`VISIBILITY_PRIVATE` not added* — the alert contains no history, so hiding it on the lock screen buys nothing

### Screens & flows
- [x] **Set up** (App lock → Turn on): PIN or password → enter → confirm → forgot-PIN options (+ optional fingerprint/face switch) → recovery code shown once with Copy (marked sensitive on Android 13+) and a required "I've saved it" checkbox
- [x] **Lock screen:** lock icon, "Enter your PIN", dots + number pad (✓ key submits — no auto-submit, so variable-length PINs don't burn attempts) or password field, "Incorrect. N attempts left", live lockout countdown, fingerprint button (auto-prompts once per lock), "Forgot PIN?". Back steps back through the flow, then `moveTaskToBack` — never skips the lock. *No shake animation* — red dots + message instead
- [x] **Change:** current → type → new → confirm; recovery code kept; separate "Get a new recovery code" row (needs current PIN)
- [x] **Turn off:** current PIN/password → clears hashes, recovery hash, biometric flag, lockout; timeout/Recents preferences kept; `FLAG_SECURE` removed
- [x] **Settings rows** (visible only when on): change, lock-after timeout, fingerprint/face (only if the phone supports it), hide in Recents, forgot-PIN options, new recovery code, turn off
- [x] All strings in `strings.xml`; M3 theme roles only; pad keys have TalkBack labels, dots announce "N digits entered", headers are `heading()`; lock + flow screens scroll (landscape / large font)

### Forgot password — recovery ladder
1. [x] **Phone screen lock** (shown only if the method allows it *and* `KeyguardManager.isDeviceSecure`): API 30+ `BiometricPrompt(DEVICE_CREDENTIAL)`, API 26–29 `createConfirmDeviceCredentialIntent` via an activity-result launcher → "Set a new lock"
2. [x] **Recovery code** `XXXX-XXXX-XXXX` (alphabet without 0/O/1/I; case, spaces and dashes ignored) → "Set a new lock" → a **new** code is shown (old one spent). Same lockout as the PIN
3. [x] **Erase history and reset lock:** explains what is deleted/kept, type `ERASE` + 10 s countdown → `NotificationDao.deleteAllNotifications()` (history + trash), in-memory history dropped via `MainViewModel.onHistoryErased()`, lock cleared → app opens. Premium, selected apps, settings kept

- [x] Setup offers "Phone screen lock or recovery code" (default) vs "Recovery code only" with a one-line trade-off; can be changed later (needs current PIN)
- [x] No phone screen lock → option 1 hidden, setup forces code-only and shows a warning; same warning on the settings screen
- [x] Removing the phone lock later hides option 1 automatically; erase-and-reset is always there — never permanently stuck
- [ ] Uninstall / Clear storage help text — not added (low value; can go in the FAQ/About later)

### Analytics & reliability
- [x] Events: `app_lock_enabled` (type, method, biometric), `app_lock_disabled`, `app_lock_changed` (type), `app_lock_recovered` (method), `app_lock_erase_reset`, `app_lock_lockout` — never PIN content/length/hash
- [x] Crashlytics custom key `app_lock_on`; DataStore corruption is recorded and falls back to "no lock" (fail-open by design so a corrupt file can't brick the app)

### Tests
- [x] Unit `AppLockRepositoryTest` (10): hash-only storage, per-salt hashes, right/wrong PIN, lockout blocks even the right PIN, escalation, reboot, recovery code normalize + shared counter, reset rotates code, change keeps code, disable clears all
- [x] Unit `AppLockControllerTest` (7): cold start, Immediately, timeout, hand-off within/after grace, hand-off dropped on resume, enable/disable from settings, start without stop
- [x] Unit `LockRulesTest` (6) + `NewSecretControllerTest` (4). **97 unit tests green**
- [x] Instrumented `LockScreenTest` (5): locked gate never composes app content, PIN pad submits digits, wrong-attempt message, forgot options hide the screen-lock option when unavailable, erase needs the word; `NotificationDaoTest` +1 (`deleteAllNotifications`). Compiled — **not yet run on a device**
- [ ] Instrumented end-to-end: shortcut / reconnect-alert launch shows the lock first, then the right screen after unlock — needs a device harness with a seeded lock
- [x] `minifyReleaseWithR8` passes with the new dependencies

**Acceptance:** with the lock on, no path (launcher, shortcut, notification tap, Recents, back button, rotation, process death) shows history before the correct PIN/password or biometric; a user who forgot the PIN can always get back in — via phone screen lock, recovery code, or erase-and-reset — without reinstalling; turning the lock off leaves the app exactly as in v0.20.
- [ ] On-device: enable PIN → kill app → reopen → lock shows; wrong ×5 → 30 s lockout; lockout survives reboot
- [ ] On-device: timeout 1 min → background 30 s → no prompt; background 2 min → prompt
- [ ] On-device: open notification-access settings / share / billing from the app and come back → no prompt (Immediately mode)
- [ ] On-device: Recents thumbnail blank; screenshot blocked while lock on
- [ ] On-device: Forgot PIN via phone screen lock on API 26–29 and on API 30+; via recovery code; via erase-and-reset (history gone, premium + selected apps kept)
- [ ] On-device: phone with no screen lock → only recovery code + erase offered
- [ ] On-device: fingerprint unlock auto-prompts; cancelling falls back to the PIN pad
- [ ] On-device: capture keeps recording while the app is locked (post test notifications, unlock, see them)
- [ ] Regression: RateUsCard still appears at launch #3 (after unlock) with unchanged behaviour; onboarding, consent and billing flows don't trigger a re-lock
