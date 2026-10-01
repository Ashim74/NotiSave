# Future Features — Big Updates After v0.20

Ideas deliberately kept **out** of the v0.20 redesign so that release stays focused. Grouped into candidate releases; order and grouping can change based on v0.20 analytics, reviews, and support email themes.

Effort: S (days) · M (1–2 weeks) · L (multi-week)

---

## v0.21 candidate — "Your data, your rules" (backup, export & privacy)

Most-requested category for notification-history apps; also the strongest premium hook.

| Feature | Effort | Notes |
|---------|--------|-------|
| **Export history** (CSV / JSON / plain text; share sheet) | M | Free: last 7 days; Premium: full range. Natural upsell. |
| **Local backup & restore** (DB + settings to a user-picked SAF folder) | M | Protects against reinstall data loss — top uninstall regret. |
| **Auto-backup schedule** (WorkManager, daily/weekly) | S | Builds on manual backup. |
| **App lock** (PIN / password + biometric, forgot-password recovery) | M | Notification history is sensitive; big trust win. Free feature. **Planned in detail as M8 in [MILESTONES.md](MILESTONES.md).** |
| **Private apps** (hide chosen apps' history behind biometric re-auth) | M | Pairs with app lock. |
| **Sensitive-content redaction** (per-app "save titles only, not message bodies") | S | Extends the existing per-app title-filter framework. |

## v0.22 candidate — "At a glance" (widgets & quick access)

Frequency-of-use drivers; every widget impression is a brand impression.

| Feature | Effort | Notes |
|---------|--------|-------|
| **Home-screen widget: recent notifications** (Glance API) | M | The single biggest "frequently used" lever available. **Must honour the M8 app lock** (hide content while the lock is on). |
| **Widget: today's stats** (count + top app mini chart) | S | Reuses Insights queries. |
| **Quick Settings tile** (tracking on/off + open app) | S | Cheap, power-user favorite. |
| **Daily digest notification** (opt-in, quiet: "Yesterday: 312 notifications, top: WhatsApp") | M | Careful: opt-in only, easy off — a notification app must respect notifications. |
| **Wear OS / tablet & foldable layouts** (two-pane history) | L | Foldable two-pane first; Wear later if demand. |

## v0.23 candidate — "Understand your notifications" (search & intelligence)

Differentiation against simpler history apps.

| Feature | Effort | Notes |
|---------|--------|-------|
| **Full-text search with FTS** (Room FTS4/FTS5 virtual table) | M | Instant search at 100k+ rows; groundwork for everything below. |
| **Smart categories** (Messages / Social / Promotions / OTPs / System via rules) | L | Rule-based first, no ML dependency. |
| **OTP lane** (auto-detected one-time codes, copy button, auto-expire) | M | Extremely sticky daily-use feature. |
| **Notification frequency alerts** ("Zomato sent 47 promos this week — mute it?") | M | Positions the app as advocate, not just archive. |
| **Advanced insights** (heatmap week view, per-app trends, quiet-hours analysis, year in review) | M–L | "Year in review" share cards = free marketing. |
| **Duplicate/spam collapse in history view** (group identical promos) | S | Reuses fingerprint column. |

## Premium 2.0 (design alongside v0.21)

Today premium = ad removal only. A second value layer increases conversion without paywalling the core promise (free users keep full capture + default retention).

- Premium: unlimited retention (free keeps a generous default), full-range export, auto-backup, custom themes/icons, future widget styles.
- Consider a small subscription tier alongside the one-time purchase once cloud-adjacent costs exist; keep lifetime "Remove Ads" owners grandfathered into ad-free forever.

## Platform & quality (ongoing, slot into any release)

- **Toolchain upgrade (compileSdk 37 / AGP 9.x / Gradle 9.6)**: required to move past Compose BOM 2026.06.01 (Compose 1.12+ compiles against API 37). AGP 9 changes Kotlin plugin handling and Gradle 9 drops deprecated APIs, so this needs its own branch and a full regression pass — do it right after v0.20 ships, not inside it.
- **Localization**: strings are extracted in v0.20 (M4) — add top Play-market languages (hi, pt-BR, es, id, de, ar) with per-locale store listings.
- **Baseline Profiles + Macrobenchmark** module for startup/scroll performance.
- **Paging 3** migration if windowed paging from M2 shows limits.
- **Predictive back** gesture support.
- **In-App Review API** consideration: the current intent-based rating flow is kept per product decision; if Play policy ever forces a change, `ReviewManager` is the sanctioned path — revisit only then.
- **Finish MainViewModel split** + adopt lightweight DI if ViewModel count grows past ~6. Status after 0.20: Home data moved to `HomeViewModel` (M5); History / Trash / AppSelection extraction was deliberately **not** done in 0.20 — trash restore, history paging, per-app history and the conversation controller all invalidate each other through shared state, so splitting them safely needs a shared repository layer first and on-device regression we couldn't run. Do it as: (1) `NotificationRepository` owning the DAO + change signals, (2) move Trash, (3) move per-app history, (4) move main history + conversations.
- **Rate-card logic tests** — the card is frozen by product decision, so its trigger (`launchCount >= 3 && showRateUsCard`) stays inline in `HomeScreen`; if it's ever allowed to change, extract it to a pure function and unit-test the three paths.
- **CI**: GitHub Actions running unit tests + lint on PRs (repo already uses PR flow).

## Explicit non-goals (revisit only with strong evidence)

- **Cloud sync of notification content** — privacy expectations + data-safety burden outweigh benefit; backups stay local/user-controlled.
- **Reading/actioning notifications from other devices** — out of scope of the app's promise.
- **Aggressive engagement notifications** — nothing beyond the opt-in digest and the tracking-stopped alert; trust is the product.
