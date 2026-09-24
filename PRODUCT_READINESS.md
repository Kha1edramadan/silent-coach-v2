# Silent Coach V2 — Product Readiness

## Current source state
**Functional product beta / release-candidate source.** The source now includes the core product flows and automated core tests, but public 1.0 distribution still requires an Android build and physical-device QA.

## Product areas implemented
- Onboarding and editable profile.
- Goals with editable calorie/protein/carbohydrate/fat targets.
- Flexible programs and days: create, activate, rename, reorder and delete; completed history remains separate.
- Exercise library with custom exercises and favorites.
- Live workout with set-level logging; weight+reps, reps-only and timed tracking; RPE/RIR, set types, previous-performance reference, rest timers and session history.
- Nutrition by grams from per-100g nutrient records.
- Meals, saved meals, custom foods and barcode lookup.
- Food source/verification/revision model; imported barcode data remains explicitly unverified until review.
- Body measurements and weight trend.
- Arabic/English and RTL/LTR switching.
- Curated quotes/ideas with separate language fields, attribution and source URLs.
- Quote notification intervals, quiet hours and a daily personal reminder.
- Local JSON backup export/import.

## Known release gates
- Android 13–16 physical-device QA.
- Final visual design polish and approved Arabic font asset.
- Expand/maintain Egyptian food data with traceable validation. The bundled starter catalog is not a claim of complete supermarket coverage.
- Release signing and distribution configuration.
- Final privacy/terms/support surfaces and matching store disclosures.

See `PRODUCT_FINAL_QA.md` for the exact gate list.
