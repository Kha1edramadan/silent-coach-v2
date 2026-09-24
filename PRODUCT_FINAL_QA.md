# Silent Coach V2 — Final QA Gates

This document is the release gate, not a claim that the current chat environment has already passed physical-device QA.

## Automated checks completed in this environment
- Core Java source compiles.
- `CoreSelfTest` returns `CORE_SELF_TEST=PASS`.
- MainActivity delimiter balance verified.
- Local-call scan found no unresolved application-local method references after the final patch.
- Workout timed tracking is aligned with exercise `tracking` (`TIMED`, `REPS`, `WEIGHT_REPS`).
- Backup import/export paths are wired.

## Required Android checks before public release
- Build debug APK and release AAB with API 36.
- Install and exercise-test on Android 13, 14, 15 and 16.
- First-run onboarding, back navigation, rotation/process recreation.
- Create/rename/delete programs and days; reorder days and exercises.
- Start/resume/finish/discard a workout; verify history is immutable after program edits.
- Verify weight/reps, reps-only, and timed exercises; rest timers; skipped exercises; set types.
- Verify metric/imperial conversion does not mutate stored metric values.
- Search foods, edit grams, save/reuse meals, custom foods, barcode success/failure/offline paths.
- Verify imported food remains unverified until reviewed and that food revisions preserve historical meal values.
- Verify Arabic RTL and complete string coverage; add the approved Arabic font asset before public release.
- Quote notifications: 1/2/3/4 hour settings, quiet hours, daily reminder, Android 13+ notification permission, reboot rescheduling.
- Backup round-trip: export -> clear app data -> import -> compare core records.
- Accessibility: content descriptions, touch target sizes, contrast, TalkBack and font scaling.
- Release crash-free smoke test and privacy/data-disclosure review.

## Food-data gate
No bundled food may be presented as a current branded Egyptian supermarket product merely because a community/API source returned a value. Each verified branded item needs a traceable source or current package-label review and a verification date.
