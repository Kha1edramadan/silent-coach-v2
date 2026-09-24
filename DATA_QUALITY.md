# Silent Coach V2 — Food Data Quality Rules

## What counts as trusted
The app distinguishes `verified`, `reference`, `imported`, `user`, and `starter` records. A record must have a traceable source before it is promoted to `verified` in production.

## Source hierarchy
1. Egyptian Food Composition Tables / National Nutrition Institute via FAO/INFOODS for generic Egyptian foods.
2. USDA FoodData Central for generic references and branded references where appropriate.
3. Open Food Facts for barcode discovery/enrichment.
4. Current package nutrition label review for Egyptian supermarket products.

## Important rule
A supermarket catalog is not automatically a nutrition database. Product names, sizes, formulations and labels can change. The app stores `source_id`, `revision`, `verification`, and `last_verified` so records can be audited.

## Historical integrity
When a food is logged, the meal item stores the food revision and the calculated grams/macros at that time. Updating a food record later must not rewrite historical meals.

## Raw vs cooked
Generic foods are tagged with a preparation/state such as `raw`, `cooked`, `drained`, or `as_served`. These are separate concepts because the nutrient density can change after cooking or draining.

## Product ingestion
Barcode-imported values are marked `imported`, not `verified`. The UI tells the user to compare them with the current package label before relying on them. User-entered foods remain `user` data.
