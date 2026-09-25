# Silent Coach — AI Studio Master Rebuild Prompt

## IMPORTANT CONTEXT

You are rebuilding **Silent Coach**, a native Android fitness + nutrition application.

The GitHub repository may contain an older Java implementation under:
- app/src/main/java/com/silentcoach/v2/

Treat that legacy Java implementation as **reference material only**.
Do NOT extend it and do NOT mix it with the new architecture.

The target implementation is:
- Kotlin
- Jetpack Compose
- Material 3
- Room
- ViewModel + Repository architecture
- Offline-first
- Arabic + English
- RTL + LTR
- Modern Android UX
- Production-quality nutrition logging

The user also has a newer Kotlin/Compose source archive named:
**silent-coach-v2-food-ui.zip**

When that archive is available to you, use it as the canonical starting point. Preserve correct functionality from it, then apply this specification.

---

# 1. FIRST: AUDIT BEFORE CODING

Before making large changes, inspect the entire available project.

Map:
- Gradle setup
- application/module configuration
- AndroidManifest
- navigation
- screens
- reusable Compose components
- theme
- typography
- data models
- Room database
- migrations
- DAOs
- repositories
- ViewModels
- nutrition logic
- workout logic
- profile/onboarding
- notifications
- localization
- assets/fonts
- tests

Identify:
- duplicated implementations
- old Java code
- placeholder UI
- fake/demo data
- hard-coded assumptions
- destructive database migrations
- missing loading/error/empty states
- inaccessible touch targets
- UI density problems
- broken navigation
- missing offline handling
- missing nutrition edit/delete flows

Do not start by blindly rewriting files.

Create a concise internal implementation map, then implement.

---

# 2. TARGET ARCHITECTURE

Use a clean, understandable structure:

app/
  src/main/java/.../
    data/
      dao/
      database/
      entity/
      remote/
      repository/
    domain/
      model/
    localization/
    notifications/
    ui/
      components/
      screens/
      theme/
      viewmodel/

Use:
- immutable state where practical
- StateFlow
- viewModelScope
- repository abstraction
- Room for local persistence
- Retrofit/OkHttp for remote food data
- Compose UI
- Material 3
- proper Room migrations

Do NOT introduce unnecessary architecture layers just for ceremony.

---

# 3. NUTRITION SYSTEM — THIS IS THE MAIN PRIORITY

The nutrition experience must feel like a real food-tracking product, not a demo database.

## Food discovery methods

The user must be able to add food through:

1. Search local foods
2. Search online foods/products
3. Scan barcode
4. Recent foods
5. Favorites
6. Custom food
7. Saved meals/templates
8. Recipes / composed meals

The absence of a food from the bundled list must NEVER block the user.

### Example

Searching for:

"تونة"

must not fail simply because there is no exact local row.

Support relevant variants such as:
- canned tuna in water
- canned tuna in oil
- drained tuna
- branded tuna products returned by the online database
- custom tuna entry if the exact product is unavailable

Support Arabic and English search terms and aliases.

---

# 4. FOOD DATA MODEL

Food records should support at minimum:

- id
- name
- nameAr
- brand
- category
- state
- caloriesPer100g
- proteinPer100g
- carbsPer100g
- fatPer100g
- fiberPer100g
- sugarPer100g
- saturatedFatPer100g
- sodiumMgPer100g
- cholesterolMgPer100g when available
- servingGrams
- servingLabel
- barcode
- externalId
- imageUrl
- provenance
- source
- isCustom
- isFavorite

Useful provenance states:
- VERIFIED
- REFERENCE
- IMPORTED
- USER_CREATED
- STARTER

Do not present imported third-party nutrition as medically or scientifically verified.

---

# 5. FOOD DATABASE STRATEGY

Use a layered model:

LOCAL
  → fast offline search and frequently used foods

REMOTE
  → Open Food Facts or another appropriate product database

CUSTOM
  → user-controlled fallback

CACHE
  → persist successfully resolved remote products locally

Search behavior:
- local search should be immediate
- remote search should only happen after deliberate user action or controlled debounce
- never send an API request for every keystroke
- show loading state
- show failure state
- show "not found"
- allow manual entry
- cache successful product lookups

For barcode:
- check local database first
- then remote product lookup
- then offer custom product entry if missing

Do not fabricate external nutrition data.

---

# 6. BARCODE SCANNING

Implement a real Android barcode scanner.

Preferred implementation:
Google ML Kit / Google Play Services Code Scanner or an equivalent robust Android barcode solution.

Requirements:
- clear scanning state
- success feedback
- cancel/back behavior
- invalid/empty result handling
- lookup by scanned code
- product not found state
- manual/custom fallback

Do not use a fake scan button.

---

# 7. FOOD LOGGING

Logging flow should be:

Add Food
→ choose Search / Scan / Recent / Favorite / Custom
→ select food
→ choose serving
→ set quantity
→ preview nutrition
→ choose meal
→ log

Support:
- grams
- milliliters where relevant
- pieces
- servings
- tablespoons
- teaspoons
- practical common units

The user must be able to edit a logged item later.

Editing should recalculate calories/macros from the current quantity while preserving historical integrity.

Every logged food item needs:
- edit
- delete
- quantity adjustment

---

# 8. MEALS

Default meal groups:
- Breakfast
- Lunch
- Dinner
- Snacks

Also support:
- custom meal
- rename
- delete
- add food
- delete food item
- reorder where useful
- save meal as template

Saved meal templates:
- create
- rename
- apply
- delete

Applying a template must create new historical meal items.
Do not mutate historical logs by reference.

---

# 9. DAILY NUTRITION DASHBOARD

Home/Nutrition should prioritize:

1. Calories
2. Protein
3. Carbohydrates
4. Fat
5. Meals
6. Quick add

Secondary information stays secondary.

Do not fill the screen with tiny metrics.

Use Bento-style grouping:
- one dominant hero nutrition card
- macro cards
- meal cards
- utility cards
- quick actions

Everything should breathe.

---

# 10. UI DIRECTION — 2026

The entire application needs a coherent visual language.

## Core aesthetic

- near-black foundation
- restrained fluorescent/lime green accent
- green should feel energetic but controlled
- no aggressive neon wash
- no gamer RGB aesthetic
- no random gradients

Use:
- dark glass surfaces
- subtle translucency
- thin borders
- soft depth
- restrained blur
- layered cards
- Bento-inspired composition
- strong typography
- clear alignment
- generous negative space

The interface should feel:
- premium
- modern
- calm
- precise
- athletic
- focused

Not:
- childish
- over-animated
- crowded
- generic dashboard
- template-like
- over-decorated

---

# 11. DESIGN TOKENS

Centralize:
- colors
- typography
- spacing
- corner radii
- elevations
- borders
- glass surface styles

Primary palette direction:
- near black background
- dark green-black surfaces
- restrained lime/fluorescent green
- white / cool gray text

Use a spacing system rather than arbitrary values everywhere.

Use a consistent radius hierarchy.

---

# 12. TYPOGRAPHY

Support both Arabic and English properly.

Recommended:
- Cairo for Arabic
- Inter for Latin/English

Requirements:
- Arabic RTL layout
- English LTR layout
- correct mixed-language behavior
- readable line heights
- clear hierarchy
- numbers should remain easy to scan

Do not use one font blindly for every script.

---

# 13. RESPONSIVE LAYOUT

The UI must adapt to:
- small phones
- standard phones
- large phones
- tablets

Avoid:
- hard-coded widths
- clipped text
- overlapping controls
- giant empty areas
- cramped cards

Use adaptive layouts where useful.

---

# 14. CONTENT FLEXIBILITY

Major app sections must support appropriate CRUD operations.

Nutrition:
- add
- edit
- delete
- favorite
- customize
- reorder when relevant

Meals:
- add
- edit
- delete
- save
- duplicate
- apply

Workout:
- programs
- days
- exercises
- sets
- history

Profile:
- targets
- units
- language
- preferences

Do not hard-code a rigid UI that becomes difficult to modify.

---

# 15. INTERACTION AND MOTION

Use subtle motion for:
- screen transitions
- card entrance
- progress changes
- bottom sheets
- scanner states
- selection states
- navigation

Animation should communicate state and hierarchy.

Avoid:
- excessive bouncing
- infinite decorative animations
- distracting motion
- constant pulsing

Respect reduced-motion preferences.

---

# 16. STATES

Every major screen and async operation should have:

- loading
- success
- empty
- error
- offline
- retry

Examples:

Remote food search:
loading → results / empty / error

Barcode:
scanning → lookup → product / not found / failure

Meal:
empty → first item → multiple items

Do not leave blank screens with no explanation.

---

# 17. OFFLINE-FIRST

Core features must work without internet:

- existing foods
- favorites
- recent foods
- custom foods
- meals
- templates
- logged nutrition
- workout logging
- progress tracking
- profile data

Internet should enhance product discovery, not become a dependency for basic use.

---

# 18. DATA SAFETY

Never use destructive migration for production user data.

When schema changes:
- add Room migrations
- preserve existing rows
- preserve logged history
- preserve profile
- preserve workout history
- preserve nutrition history

Do not casually rename/delete columns without migration.

---

# 19. PERFORMANCE

Avoid:
- loading huge lists unnecessarily
- remote API calls for every keystroke
- expensive recomposition caused by unstable state
- synchronous database operations on main thread

Use:
- Flow
- suspend functions
- proper coroutine dispatching
- stable Compose state
- lazy lists

---

# 20. ACCESSIBILITY

Check:
- touch target sizes
- content descriptions
- contrast
- readable font sizes
- dynamic content
- screen-reader semantics
- reduced motion

Do not rely on color alone to communicate state.

---

# 21. QUALITY BAR

Before saying the rebuild is complete, verify:

Navigation:
- every tab works
- back works
- sheets dismiss correctly
- state survives navigation

Nutrition:
- search
- Arabic search
- English search
- barcode
- remote lookup
- missing product
- custom food
- edit food
- delete food
- serving changes
- meal assignment
- saved template
- apply template
- deletion

Database:
- app reinstall/build behavior
- migration path
- no destructive migration
- data persistence

UI:
- phone
- large phone
- tablet
- RTL
- LTR
- empty states
- loading
- errors
- offline

---

# 22. BUILD STABILITY

Keep the project easy to build in Google AI Studio / Android Studio.

Do not require:
- unavailable secrets
- private local files
- developer-specific absolute paths
- mandatory release keystore for debug builds

Release signing should not break normal development builds when credentials are absent.

Keep environment-specific settings optional.

---

# 23. GOOGLE AI STUDIO EXECUTION ORDER

Implement in this order:

PHASE A
Audit project and resolve legacy/current architecture confusion.

PHASE B
Make the Kotlin/Compose project the canonical implementation.

PHASE C
Rebuild nutrition data model + Room migrations.

PHASE D
Implement local food search + categories + favorites + recent foods.

PHASE E
Implement remote food search + caching + error/offline handling.

PHASE F
Implement barcode scanning + remote barcode lookup + custom fallback.

PHASE G
Rebuild meal logging/edit/delete/templates.

PHASE H
Redesign complete UI/UX.

PHASE I
Motion + accessibility + responsive polish.

PHASE J
Run comprehensive QA and remove placeholders.

Do NOT attempt all phases as one uncontrolled rewrite.

---

# 24. CRITICAL AI BEHAVIOR

Never:
- invent nutrition values and label them verified
- replace working logic just to simplify it
- delete user data
- create fake barcode functionality
- create fake online search
- create fake UI interactions
- leave buttons with no behavior
- produce a beautiful but non-functional mockup
- mix legacy Java and the new Kotlin architecture

Always:
- inspect before modifying
- preserve working logic
- build real flows
- handle errors
- keep data editable
- keep the UI coherent
- verify dependencies
- verify navigation
- verify persistence

The final result should feel like a real 2026 Android product, not a generated prototype.
