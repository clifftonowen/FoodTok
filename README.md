# FoodTok

[![CI](https://github.com/clifftonowen/FoodTok/actions/workflows/ci.yml/badge.svg)](https://github.com/clifftonowen/FoodTok/actions/workflows/ci.yml)
[![Download APK](https://img.shields.io/badge/download-latest%20APK-3ddc84)](https://github.com/clifftonowen/FoodTok/releases/latest)
[![Platform](https://img.shields.io/badge/Android-7.0%2B%20(API%2024)-3ddc84)](#getting-started)
[![Language](https://img.shields.io/badge/Java-11-b07219)](#tech-stack)

> **TikTok, but for recipes.** Swipe a vertical video feed, search by the ingredients already
> in your fridge, and ask an AI about any dish. Native Android (Java) on Supabase + Gemini.

**What you are looking at**

- **Feed** &mdash; nested `ViewPager2` over a pooled ExoPlayer. Vertical swipe between recipes,
  horizontal between *Ingredients / For You / Chat*.
- **Ingredient search** &mdash; a hand-written **Trie** (`data/Trie.java`), O(L) autocomplete
  over the full ingredient catalog, multi-chip queries ranked by match count.
- **AI** &mdash; Gemini 2.5 Flash behind **Supabase Edge Functions**, so the API key never
  ships in the APK. Per-recipe chatbot with conversation memory, plus on-demand allergen
  detection, instruction generation and calorie estimation.
- **For You** &mdash; a custom **max-heap priority queue** re-ranks the feed against a HashMap
  interest profile after every like, save or not-interested.

<!-- DEMO MEDIA — paste this block in once docs/demo.gif exists, replacing the bullet list
     above with a two-column layout so the GIF and the bullets sit side by side:

<table>
<tr>
<td width="320" valign="top">

<img src="docs/demo.gif" width="300" alt="FoodTok demo: swiping the vertical recipe video feed, generating AI allergen and instruction insights, chatting with the per-recipe assistant, and searching by ingredient with Trie autocomplete">

</td>
<td valign="top">

...the four bullets above go here, unchanged...

</td>
</tr>
</table>
-->

<!-- Appetize browser demo: uncomment once the app is uploaded.
**[Try it in your browser](https://appetize.io/app/APPETIZE_PUBLIC_KEY)** &middot;
-->
**[Download the APK](https://github.com/clifftonowen/FoodTok/releases/latest)** &middot;
**[How it works](#architecture)** &middot;
**[Data structures](#data-structures--algorithms)** &middot;
**[Full feature list](#feature-set)**

<sub>Built for SUTD 50.001 Information Systems &amp; Programming, Spring 2026.</sub>

---

## Table of Contents

- [Overview](#overview)
- [Feature Set](#feature-set)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Data Structures & Algorithms](#data-structures--algorithms)
- [OOP Design](#oop-design)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)
- [Getting Started](#getting-started)
- [Build & Test](#build--test)
- [Next Steps](#next-steps)
- [Team](#team)
- [License](#license)

---

## Overview

FoodTok is a vertical-video recipe discovery app modeled after the short-form video feeds users already know. It is delivered as a native Android client backed by Supabase (Postgres, Auth, Storage) and Google Gemini for AI-driven features. Every recipe carries structured ingredient metadata, enabling ingredient-on-hand search, allergen filtering, and a recommendation engine that adapts to user behavior in real time.

The project was scoped to demonstrate strong object-oriented design, custom data structures from SUTD 50.004, and a production-shaped service architecture suitable for extension into a real product.

---

## Feature Set

### Authentication & Onboarding
- Email/password sign-up and login via **Supabase GoTrue** with JWT session persistence.
- **Transparent JWT refresh** on 401 via an OkHttp `Authenticator`, so the UI never sees an expired token.
- Post-signup **3-step onboarding flow** (avatar, cuisine preferences, allergen blacklist) that seeds the user's recommendation profile on first launch.

### Feed & Discovery
- **TikTok-style nested ViewPager2** — vertical swipe between recipes, horizontal swipe between Ingredients / For You / Chat panes.
- **ExoPlayer-based video playback** with a pooled player architecture (`FeedVideoPlayerPool`) and `VideoCache` for smooth autoplay and low jank.
- **Grid feed** alternative view for browse-style discovery.
- **Saved-recipes feed** pulling from the user's `interactions` table.
- **Interaction buttons** (like, save, not-interested, comment) that write through to Postgres and feed back into the recommendation model.

### Recommendation Engine
- Custom **max-heap priority queue** (`RecipePriorityQueue`) for O(log n) ranking of candidate recipes.
- **HashMap-backed interest profile** on `User` — tags are scored on +30 (preference), +5 (like), +10 (save), –10 (not-interested), with inverse deltas on undo.
- **Allergen/blacklist penalty** applied before ranking so flagged recipes are demoted or excluded.
- Feed re-ranks after every interaction, giving the user an immediately noticeable personalization signal.

### Ingredient Search
- **Custom Trie** (`data/Trie.java`, `data/TrieNode.java`) with O(L) autocomplete over the full ingredient catalog.
- **Chip-based multi-ingredient query** in `SearchFragment`, ranked server-side-adjacent by match count.
- **User search** with a dedicated adapter for discovering other creators.

### Recipe Upload
- Camera or gallery video picker, followed by a structured upload form with ingredient chips, prep/cook time pickers, and tag selection.
- Uploads go to **Supabase Storage**, and the recipe row is created atomically via PostgREST.

### Comments
- Bottom-sheet comments UI with real-time-style refresh on submit.
- Pagination-ready ordering via PostgREST.

### AI Features (Gemini 2.5 Flash)
- **Per-recipe chatbot** with conversation memory (20-message cap per recipe) and a context-aware system prompt that injects title, ingredients (with allergen flags), times, and tags.
- **On-demand recipe enrichment** ("Generate AI Insights" button) that returns allergens, generated instructions, calorie estimates, and suggested tags in a single JSON response.
- **Client-side enrichment cache** keyed by `recipeId` to avoid repeat quota spend.
- **Graceful 429 handling** with user-facing messaging and mock fallback if no API key is configured.

### Profile & Social
- Manage profile (avatar, username, preferences, allergens).
- Follow / unfollow other users with follower/following list views.
- View other users' profiles and their recipe grids.

### UX Polish
- Haptic feedback on primary interactions.
- Dark scrim gradient and safe-area handling for feed overlays.
- Keyboard-aware layout so the bottom nav isn't pushed off-screen.
- Consistent color tokens in `colors.xml` (no hardcoded hex in layouts).

---

## Architecture

```
┌──────────────────────────┐       Retrofit + OkHttp
│    Android (Java, XML)   │──────────┐
│  MVVM-lite: UI / Service │          │
│  / Model layers          │          ▼
└──────────────────────────┘   ┌──────────────────┐
            │                  │   Supabase       │
            │                  │  ┌────────────┐  │
            │                  │  │  GoTrue    │  │
            │                  │  │  (Auth)    │  │
            │                  │  └────────────┘  │
            │                  │  ┌────────────┐  │
            │                  │  │ PostgREST  │  │
            │                  │  │ (Postgres) │  │
            │                  │  └────────────┘  │
            │                  │  ┌────────────┐  │
            │                  │  │  Storage   │  │
            │                  │  │  (video)   │  │
            │                  │  └────────────┘  │      ┌────────────────┐
            │                  │  ┌────────────┐  │      │  Gemini REST   │
            │                  │  │ Edge Funcs │──┼─────►│  2.5 Flash     │
            │                  │  │ gemini-chat│  │      └────────────────┘
            │                  │  │ gemini-    │  │       key = Supabase
            │                  │  │   enrich   │  │       secret, never
            │                  │  └────────────┘  │       on the device
            │                  └──────────────────┘
            └─── ExoPlayer ◄─── HLS/MP4 (direct Storage URL)
```

The app is fully serverless on the backend — Supabase handles auth, Postgres with auto-generated REST, object storage, and the Edge Functions that broker Gemini. **The Gemini API key never ships in the APK**: it lives as a Supabase secret and is read server-side by `gemini-chat` and `gemini-enrich`. The release workflow enforces this — a build step greps the compiled DEX for the Gemini endpoint and for `AIza…` key patterns, and fails the release if either is present.

### Service Architecture

Every external capability is expressed as an **interface + real impl + mock impl + provider factory**, enabling offline development and deterministic unit tests:

```
IRecipeService ──► SupabaseRecipeService   (prod)
                └─ MockRecipeService       (tests / offline)

IChatService  ──► SupabaseChatService
                └─ MockChatService

IAuthService  ──► SupabaseAuthService
                └─ MockAuthService

...and so on for comments, interactions, enrichment.
```

Providers auto-select the real implementation when credentials are present and fall back to the mock otherwise — the rest of the app is unaware.

---

## Tech Stack

| Layer | Technology                                                   |
|---|--------------------------------------------------------------|
| Client | Android (Java), XML layouts, Material Components, ViewPager2 |
| Networking | Retrofit 2.11 + OkHttp 4.12 + Gson 2.11                      |
| Video | AndroidX Media3 ExoPlayer (pooled + cached)                  |
| Images | Glide 4.16                                                   |
| Auth | Supabase GoTrue (JWT)                                        |
| Database | Supabase Postgres + PostgREST + Row Level Security           |
| Storage | Supabase Storage                                             |
| AI | Google Gemini 2.5 Flash (REST)                               |
| Build | Gradle Kotlin DSL, Java 11, Min SDK 24 / Compile SDK 36      |
| Testing | JUnit 4, Espresso                                            |

---

## Data Structures & Algorithms

The SUTD 50.004 requirement mandates custom implementations — Java's built-in collections may back custom structures but cannot be the whole answer.

| Structure | Location | Purpose | Complexity |
|---|---|---|---|
| **Trie** | `data/Trie.java` | Ingredient autocomplete as the user types. | O(L) per lookup |
| **Max-Heap Priority Queue** | `data/RecipePriorityQueue.java` | Feed ranking against the interest profile. | O(log n) insert, O(1) peek |
| **HashMap Interest Profile** | `User.interestProfile` | Tag → score map updated on every interaction. | O(1) get/put |
| **HashSet-backed Allergen Check** | `Recipe.findBlacklistedIngredients()` | Reject flagged recipes before ranking. | O(1) per ingredient |

All custom structures ship with JUnit coverage (`TrieTest`, `RecipePriorityQueueTest`, `RecommendationServiceTest`).

---

## OOP Design

The codebase deliberately applies the principles graded by the course:

- **Encapsulation** — private fields, intentional getters/setters, `Collections.unmodifiableList()` on exposed collections, fully-immutable `Ingredient` and `ChatMessage`.
- **Tell, Don't Ask** — e.g. `Recipe.containsAllergen(user)` rather than exposing ingredients for callers to iterate.
- **Strategy Pattern** — every service is an interface with real + mock strategies selected by a provider at runtime.
- **Singleton / Factory** — `AuthManager`, `SessionManager`, `*ServiceProvider` factories.
- **Adapter Pattern** — RecyclerView adapters with view-type polymorphism (chat bubbles, grid vs. feed cells).
- **Observer / Callback** — async boundary expressed via typed callback interfaces (`AuthCallback`, `ChatCallback`, etc.).
- **Separation of Concerns** — fragments do not contain business logic; services have no Android View references; adapters only bind.

Code adheres to the **Google Java Style Guide** (2-space indent, 100-char lines, K&R braces, full brace usage, class-level Javadoc on public types).

---

## Project Structure

```
com.example.foodtok
├── FoodTokApplication.java          # App entry point; initializes providers
├── adapters/                        # RecyclerView + ViewPager2 adapters
├── auth/                            # IAuthService, Supabase + Mock impls, AuthManager
├── data/                            # Custom Trie, TrieNode, RecipePriorityQueue
├── models/                          # Domain models (Recipe, User, Ingredient, ...)
│   └── dto/                         # API request/response DTOs
├── services/                        # Service interfaces, Supabase + Mock impls, providers
│                                    # RecommendationService (feed ranking)
├── ui/                              # Activities + Fragments
└── util/                            # ApiClient, SessionManager, video pool/cache
```

---

## Database Schema

The app uses Supabase (Postgres + PostgREST + Auth + Storage). Supabase Auth owns accounts in `auth.users`; `profiles` extends them with app data.

| Table | Purpose |
|---|---|
| `profiles` | App-side user data: username, avatar, `interest_profile` (JSONB), `blacklisted_ingredients` (TEXT[]). |
| `recipes` | Recipe metadata: title, description, video URL, tags, prep/cook times, estimated calories. |
| `ingredients` | Canonical ingredient catalog with allergen flags and calorie density. |
| `recipe_ingredients` | M:N join with `quantity` and `is_optional`. |
| `interactions` | Like / save / not_interested / view events (unique per user × recipe × type). |
| `comments` | Per-recipe comments. |
| `cookbooks` / `cookbook_recipes` | User-curated recipe collections. |
| `follows` | Follower graph with proper FK constraints. |

Row-Level Security is enabled on all tables; users can only mutate their own rows.

---

## Getting Started

### Prerequisites
- Android Studio Koala or later
- JDK 11+
- A Supabase project (free tier is fine)
- A Google AI Studio API key for Gemini, set as a Supabase secret — **not** in
  `local.properties`. See [AI proxy](#ai-proxy) below.

### Configuration

Create `local.properties` in the project root (this file is gitignored):

```properties
SUPABASE_URL=https://<your-project-ref>.supabase.co
SUPABASE_ANON_KEY=<your-supabase-anon-key>
```

These values are injected into `BuildConfig` at build time via `app/build.gradle.kts` and surfaced through `util/Constants.java`.

<a name="ai-proxy"></a>
### AI proxy

Gemini is **not** called from the device. Both AI features go through Supabase Edge
Functions in `supabase/functions/`, which hold the API key as a server-side secret:

| Function | Used by |
|---|---|
| `gemini-chat` | Per-recipe cooking assistant |
| `gemini-enrich` | Allergen detection, instruction generation, calorie estimate |

Deploy them and set the key with:

```bash
supabase link --project-ref <your-project-ref>
supabase secrets set GEMINI_API_KEY=<your-key>
supabase functions deploy gemini-chat gemini-enrich
```

The app decides real-vs-mock on whether `SUPABASE_URL` and `SUPABASE_ANON_KEY` are
configured (`Constants.isAiProxyConfigured()`), so a clone without a Supabase project
falls back to the mock services rather than failing.


## Build & Test

### Offline UI preview

The debug build uses deterministic local data by default. It does not require
Supabase or Gemini credentials, and its HTTP client blocks outbound requests.

```bash
./gradlew assembleDebug
```

To intentionally build against configured live services instead:

```bash
./gradlew assembleDebug -Pfoodtok.stub=false
```

```bash
./gradlew assembleDebug         # Build debug APK
./gradlew assembleRelease       # Build release APK
./gradlew lint                  # Lint checks
./gradlew test                  # Local JVM unit tests (Trie, PriorityQueue, Recommendation)
./gradlew connectedAndroidTest  # Instrumented tests (device/emulator required)
```

---

## Next Steps

The current build is feature-complete for the course deliverable. To take FoodTok from a course project to a deployable consumer product, the following work is queued:

### 1. ~~Move Gemini behind a Supabase Edge Function~~ — done
The Gemini key no longer ships in the APK. Chat and enrichment are proxied through the
`gemini-chat` and `gemini-enrich` Edge Functions, which build the prompts server-side so
the client sends a typed recipe object rather than free-form model input. See
[AI proxy](#ai-proxy).

### 2. Upgrade Gemini API to a paid tier
The free tier's 15 RPM / 1,500 RPD ceiling is shared across the whole Google account and will not survive a public launch — a single user stress-testing the chat can exhaust the daily quota. Moving to a paid tier (or Vertex AI with committed throughput) gives per-project quotas, higher RPM, and SLAs suitable for a production feed. This pairs naturally with the Edge Function above, which becomes the single billed caller.

### 3. Haptic + zoom preview animation
Add a press-and-hold gesture on feed cards that triggers a scale-up preview of the video with synchronized haptic feedback (a short tick on press, a heavier tick on release). This matches the tactile feel of modern short-video apps and gives users a low-commitment way to peek at a recipe without committing a full swipe. Implementation will use `Vibrator.vibrate(VibrationEffect)` with predefined effects and a `ScaleAnimation` or `View.animate().scaleX/Y()` pipeline tied to `MotionEvent` state.

### 4. Deploy the Edge Function and marketing site via Vercel
Stand up a Vercel project for (a) the public marketing / landing page with APK download links and (b) any companion web endpoints that don't fit Supabase Edge Functions (OAuth callbacks, webhook receivers, admin dashboards). Vercel's zero-config deploy from `main` and preview deploys per pull request give the team a fast iteration loop for the consumer-facing web surface while Supabase continues to own the application backend.

### Additional polish before public launch
- Feed pagination via PostgREST `Range` headers instead of whole-table fetch
- Local Room cache for offline read of recent feed + saved recipes
- Crashlytics + analytics (Firebase or PostHog) for real-world telemetry
- Play Store asset kit: screenshots, store listing copy, privacy policy, data-safety form
- End-to-end Espresso smoke tests covering login → feed → like → comment → save

---

## Team

**Group 31 — SUTD 50.001 (Spring 2026)**

| Student ID | Name |
|---|---|
| 1009156 | Cliffton Owen Gunawan |
| 1009209 | Jeremy Leonard Purnomo |
| 1009180 | Brian Wong Wei Xiang |
| 1009147 | Giorgio Remiel Pohar |
| 1009185 | Vincent Alexander Yauvira |
| 1009143 | Jovyan |

---

## License

This project is submitted as academic coursework for SUTD 50.001 Information Systems & Programming. All third-party libraries retain their original licenses; Supabase and Google Gemini are used under their respective terms of service.
