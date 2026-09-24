# Silent Coach V2

A rebuild of the supplied Silent Coach prototype into a flexible fitness + nutrition companion.

## Core product
- Offline-first SQLite storage.
- Editable programs, days and exercises.
- Set-level workout logging with weight/reps, reps-only and timed exercise tracking, RPE/RIR, set types and rest timers.
- Historical workout/session data kept separate from later plan edits.
- Gram-based nutrition logging, saved meals, custom foods and barcode lookup.
- Food source + verification + revision model.
- Weight/measurement tracking and progress views.
- Arabic/English with RTL/LTR switching.
- Curated daily ideas/quotes with attribution and source links.
- Quote notifications and a configurable daily reminder.
- Local JSON backup/export + restore.

## Food-data policy
The bundled catalog is a **starter reference set**, not a complete Egyptian supermarket catalog. Reference foods, current branded products, community barcode imports, and user-entered foods are kept as separate states. Imported Open Food Facts records remain unverified until reviewed against a traceable source or current package label.

## Build
Use Android Studio with Android SDK 36, or the included GitHub Actions workflow. The workflow runs the core self-test, builds the debug APK, and builds the release AAB.

## Important environment note
The chat execution environment used to develop this project does not contain the Android SDK/Gradle/adb toolchain, so the final Android APK cannot be truthfully marked as device-tested here. Core Java logic is compiled and self-tested locally.

## Release gate
See `PRODUCT_FINAL_QA.md` before treating the build as public 1.0 software.


UI build pipeline updated for installable APK with bundled Cairo Arabic font.
