# Silent Coach V2 — Product Audit

## Current verdict
**Advanced functional beta. Not yet a release-ready 1.0.**

## What is already product-grade in the source foundation
- Flexible programs/days/exercises.
- Set-level logging with weight, reps, RPE/RIR, rest and set type.
- Previous-performance reference.
- Offline-first storage.
- Historical workout and nutrition snapshots.
- Per-100g nutrition scaling with arbitrary grams.
- Food source, verification, revision and quality flags.
- Barcode import path with explicit unverified state.
- Arabic/English RTL/LTR switching.
- Daily quotes/ideas with attribution fields and source links.
- Quote-frequency, quiet-hours and daily reminder settings.
- Local JSON backup export/import.

## Release blockers
1. **Android device build + QA:** the current execution environment has no Android SDK/Gradle/adb, so the APK cannot be truthfully marked tested here.
2. **Visual system:** the current UI is still a programmatic View implementation. It needs final component design, typography, Cairo font asset, spacing scale, navigation polish, accessibility, loading/error/empty/success states, and interaction polish.
3. **Training depth:** add supersets/circuits, timed/distance tracking where relevant, progression rules, multi-week program scheduling/deloads, calendar, plate calculator, and editable past workout values.
4. **Food breadth:** grow the starter catalog into a maintained Egyptian generic-food + packaged-product pipeline. Do not label imported/community data as verified without traceable review.
5. **Distribution:** release signing, store listing assets, crash reporting, public privacy policy, terms/contact/support surface, and release QA matrix.

## Definition of done for 1.0
- A signed APK/AAB builds with target SDK 36.
- No P0 crashes in onboarding, workout, nutrition, barcode failure, notifications, backup/restore, language and unit switches.
- Data round-trip test: backup -> clear -> restore produces equivalent core records.
- Food audit: every bundled verified product has traceable source + verification date.
- Workout audit: editing the current program never rewrites completed historical sets.
- Arabic audit: complete RTL review with the chosen Arabic font.
- Android 13/14/15/16 device/emulator QA completed.
- Store privacy/data disclosures match the actual network behavior.
