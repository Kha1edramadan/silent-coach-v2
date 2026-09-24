# Silent Coach V2 — Product Specification

## Core principle
The app should never make the user fit the app. Programs, exercises, sets, foods, meals and targets are editable data.

## Workout
- Programs: create, activate, day naming, days/week.
- Days: rename and reorder by day order.
- Exercises: add, replace, delete, move up/down, custom exercises, favorites.
- Exercise prescription: sets, rep range, rest, RPE, RIR, notes.
- Live session: set-level weight/reps/RPE/completion, add set, skip exercise, rest timer, session notes.
- History: completed sessions, volume and set counts, previous best reference.
- Snapshot rule: editing a program never rewrites completed workout history.

## Nutrition
- Per-100g calculations with arbitrary gram input.
- Meals grouped by the current day.
- Saved meals reusable as templates.
- Custom foods and barcode-imported products.
- Food state and verification shown to users.
- Food revisions are snapshotted into meal items.

## Goals
- Weight, height, age, sex, activity, goal.
- Initial calorie/macro estimate uses Mifflin-St Jeor + editable targets.
- Targets remain user-editable.
- The app does not represent an estimate as a medical diagnosis or a guaranteed requirement.

## Progress
- Weight check-ins.
- Body measurements.
- Workout frequency.
- Logged training volume.
- Workout history.

## Localization
- English and Arabic.
- Runtime RTL/LTR switching.
- Arabic UI text is written for natural app language rather than literal machine translation.

## Reliability requirements
- No secret API keys inside the APK.
- Network failures must not block offline logging.
- Completed nutrition logs preserve the original revision.
- Completed workout logs preserve the actual sets performed.
