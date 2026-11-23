# TIMELOG — Android starter project

TIMELOG is an offline-first Android digital wellbeing and phone-inactivity tracker.

## Requirements
- Android Studio with JDK 17
- Android SDK 35
- Minimum Android 10 (API 29)

## Open
1. Extract this ZIP.
2. Open the `TIMELOG_Android` folder in Android Studio.
3. Allow Gradle sync to finish.
4. Run on an Android 10+ device.
5. Open Settings → Special app access → Usage access and enable TIMELOG.

## Included in this starter
- Integrated official TIMELOG bright-burgundy launcher logo (all Android density buckets)
- Kotlin + Jetpack Compose app shell
- Minimal icon-first expandable navigation
- Frost White and Pure Black theme toggle
- UsageStats-based daily app foreground time
- UsageEvents-based foreground transition count (launch count is approximate)
- Screen-off / first-unlock sleep tracking scaffold
- Local SharedPreferences sleep state
- Android 10+ minimum SDK

## Important limitations / next implementation work
This is a starter implementation, not a finished production tracker.
- Android's UsageStats data is aggregated and can vary by OEM; exact hourly app slices and complete historical records need a persistent event-ingestion/database layer.
- Ordinary apps cannot reliably observe every touchscreen tap in other apps. Unlock and foreground events are used as available human-presence signals.
- The sleep worker is a basic inactivity confirmation scaffold. A production implementation must cancel/reconcile pending work on intervening unlocks, persist session records in Room, handle reboot/time changes, and validate foreground use for the full 5-minute wake window.
- Local Backup/Restore UI is currently a scaffold; SAF import/export, schema versioning, checksum and encryption are pending.
- Alerts, limits, focus rules, full analytics, search and privacy dashboard are planned modules, not yet fully implemented.
- Android background restrictions and OEM battery management can delay or suppress background work. The app must show data-quality gaps rather than claiming perfect monitoring.

## Privacy
Core design is local-first. No analytics SDK, advertising SDK, cloud account, or network permission is included in this starter.


## GitHub build
A GitHub Actions workflow is included at `.github/workflows/android-build.yml`.
Push this project to a GitHub repository; the workflow builds `:app:assembleDebug`
with JDK 17 and Gradle 8.9 and uploads `app-debug.apk` as an Actions artifact.
No Gradle wrapper is included in this starter; CI installs the pinned Gradle version.

## Important
A successful compile does not mean all planned tracking features are complete.
The current sleep receiver is only a scaffold: Android restricts manifest delivery
of screen on/off and user-present broadcasts. Reliable implementation requires a
runtime receiver while the app/service is active plus reconciliation on app resume,
and OEM background restrictions must be handled. Lifetime history, real hourly
aggregation, full backup/restore, alerts and analytics remain to be implemented.
