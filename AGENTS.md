# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew build                  # Full build (all variants)
./gradlew lint                   # Run lint checks
./gradlew test                   # Run local unit tests (JVM)
./gradlew connectedAndroidTest   # Run instrumented tests (requires device/emulator)
```

For the Spring Boot backend (once created):
```bash
cd backend/
./mvnw spring-boot:run           # Run backend server
./mvnw test                      # Run backend tests
./mvnw package                   # Build JAR
```

---

## Project Overview

**Food Tok** is a TikTok-style infinite-scrolling recipe discovery Android app, built as a new feature for the fictional "YouTwitFace" mega-app (50.001 Information Systems & Programming, SUTD Spring 2026).

Users scroll through short cooking videos, get personalized recipe recommendations, filter by ingredients on hand, and receive smart allergen alerts. The project must demonstrate strong OOP principles, good software design, and use data structures/algorithms from 50.004.

---

## Current State of the Codebase

The app has a **working UI skeleton with mock data**. No real backend, no persistence, no network calls yet.

### What exists now
- `MainActivity` hosts a `BottomNavigationView` with 5 tabs
- Navigation is manual fragment replacement (no Jetpack Navigation component)
- Profile tab swaps between `ProfileGuestFragment` and `ProfileUserFragment` depending on auth state
- Feed with infinite-scroll RecyclerView using mock data
- Comments as `BottomSheetDialogFragment`
- Login/Signup activities with basic input validation (placeholder mock logic)
- `services/` classes are **empty stubs** intended for future backend integration
- All data is **in-memory mock data**

### What does NOT exist yet
- Backend (Spring Boot)
- Database connection (Supabase)
- Real authentication
- Image/video loading (Glide is a TODO)
- Network layer (Retrofit/OkHttp)
- Local persistence (Room)
- Dependency injection (Hilt)
- Data structure implementations (Trie, Priority Queue, etc.)
- OpenAI integration

### Current dependencies
- Min SDK 24 / Compile SDK 36, Java 11, Gradle Kotlin DSL
- AndroidX AppCompat, ConstraintLayout, Material Components
- JUnit 4 + Espresso (boilerplate tests only)

---

## Architecture

### Target Architecture

```
┌─────────────┐     HTTP/REST     ┌──────────────────┐     JDBC/REST    ┌───────────┐
│  Android App │ ◄──────────────► │  Spring Boot API  │ ◄─────────────► │ Supabase  │
│  (Java/XML)  │                  │  (Java)           │                  │ PostgreSQL│
└─────────────┘                  └────────┬─────────┘                  │ Auth      │
                                          │                             │ Storage   │
                                          │ HTTP                        └───────────┘
                                          ▼
                                   ┌──────────────┐
                                   │  OpenAI API   │
                                   │  (calories,   │
                                   │  alt. recs)   │
                                   └──────────────┘
```

### Tech Stack

| Layer | Technology | Status |
|-------|-----------|--------|
| **Frontend** | Android (Java, XML layouts) | ✅ Skeleton exists |
| **Backend** | Java + Spring Boot | ❌ Not started |
| **Database** | Supabase (PostgreSQL) | ❌ Not started |
| **AI/API** | OpenAI API | ❌ Not started |
| **Build** | Gradle Kotlin DSL (Android) / Maven (Spring Boot) | ✅ Android only |

**Constraints:** App must be programmed mostly in Java. No Kotlin/Compose for main UI. Non-Java frontend frameworks are banned. Minor Kotlin usage must be justified in the report.

---

## Navigation & Key Flows

### Current Navigation
`MainActivity` hosts a `BottomNavigationView` with 5 tabs. Navigation is manual fragment replacement (no Jetpack Navigation component). The Profile tab swaps between `ProfileGuestFragment` and `ProfileUserFragment` depending on auth state.

### Key Flows

- **Feed (Home tab):** `HomeFragment` → `FeedAdapter` → `Recipe` model. Infinite-scroll RecyclerView with mock data. Comments open as a `BottomSheetDialogFragment` (`CommentsFragment`) launched from `FeedAdapter`.
- **Auth:** `LoginActivity` and `SignupActivity` are standalone activities with basic input validation. Both have placeholder mock logic with TODOs for real backend integration.
- **Comments:** `CommentsFragment.newInstance(recipeId)` factory pattern. Uses its own `CommentAdapter` and mock `Comment` objects.
- **Search (planned):** Ingredient-based filtering with Trie autocomplete.
- **Recipe Detail (planned):** Full recipe view with ingredients, instructions, allergen alert banner.
- **Upload (planned):** Pick video, enter recipe metadata, upload to Supabase Storage.
- **Profile:** Saved recipes grid, cookbook management.

---

## Package Layout

### Android App (current)

```
com.example.foodtok/
├── adapters/          FeedAdapter, CommentAdapter
├── models/            Recipe, Comment (business logic), User (stub), Ingredient (stub)
├── services/          InteractionManager (stub), RecommendationService (stub)
└── ui/                MainActivity, 5 tab fragments, LoginActivity, SignupActivity, CommentsFragment
```

### Android App (target — expand into this structure as features are built)

```
com.example.foodtok/
├── adapters/                   # RecyclerView adapters
│   ├── FeedAdapter.java              ✅ exists
│   └── CommentAdapter.java           ✅ exists
│
├── models/                     # POJOs / data classes
│   ├── Recipe.java                   ✅ exists (expand with tags, videoUrl, etc.)
│   ├── Comment.java                  ✅ exists
│   ├── User.java                     ✅ stub (expand with interestProfile, blacklist)
│   └── Ingredient.java               ✅ stub (expand with calories, is_allergen)
│
├── data/                       # Data structures (2D aspect) — NEW
│   ├── Trie.java                     ❌ implement
│   ├── TrieNode.java                 ❌ implement
│   └── RecipePriorityQueue.java      ❌ implement (max-heap)
│
├── services/                   # Business logic
│   ├── InteractionManager.java       ✅ stub (wire to backend)
│   ├── RecommendationService.java    ✅ stub (implement scoring + priority queue)
│   ├── AllergenService.java          ❌ implement (HashSet-based blacklist checking)
│   └── ApiClient.java                ❌ implement (Retrofit HTTP client)
│
├── ui/                         # Activities & Fragments
│   ├── MainActivity.java             ✅ exists
│   ├── HomeFragment.java             ✅ exists (feed)
│   ├── SearchFragment.java           ✅ exists (wire Trie autocomplete)
│   ├── ProfileGuestFragment.java     ✅ exists
│   ├── ProfileUserFragment.java      ✅ exists
│   ├── LoginActivity.java            ✅ exists (wire to real auth)
│   ├── SignupActivity.java           ✅ exists (wire to real auth)
│   ├── CommentsFragment.java         ✅ exists
│   ├── RecipeDetailActivity.java     ❌ implement
│   └── UploadActivity.java           ❌ implement
│
├── util/                       # Helpers — NEW
│   ├── Constants.java                ❌ implement (BASE_URL, etc.)
│   └── SessionManager.java          ❌ implement (JWT token storage)
│
└── FoodTokApplication.java          ❌ implement (Application entry point)
```

### Spring Boot Backend (to be created at `backend/`)

```
backend/
├── src/main/java/com/foodtok/api/
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── RecipeController.java
│   │   ├── UserController.java
│   │   ├── FeedController.java
│   │   └── UploadController.java
│   ├── service/
│   │   ├── RecipeService.java
│   │   ├── UserService.java
│   │   ├── FeedService.java
│   │   ├── OpenAIService.java
│   │   └── SupabaseStorageService.java
│   ├── model/
│   │   ├── User.java
│   │   ├── Recipe.java
│   │   ├── Ingredient.java
│   │   └── Interaction.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── RecipeRepository.java
│   │   └── InteractionRepository.java
│   ├── dto/
│   │   ├── RecipeDTO.java
│   │   ├── FeedRequestDTO.java
│   │   └── FeedResponseDTO.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── SupabaseConfig.java
│   │   └── OpenAIConfig.java
│   └── FoodTokApiApplication.java
├── src/main/resources/
│   └── application.yml
└── pom.xml
```

---

## Core Data Structures & Algorithms (2D Aspect)

These MUST be implemented in `data/` package (not just imported from Java collections). The logic must be custom to satisfy the 50.004 requirement. Java built-in collections can be used as backing stores.

### 1. HashMap — User Interest Profile
- **Where:** `User.interestProfile` field (`Map<String, Integer>`)
- **What:** Maps recipe tags (e.g., "Italian", "Vegan", "Quick") to preference scores
- **Scoring:** Initial preference +30, like +10, save +15, skip -5, not-interested -30
- **Complexity:** O(1) average for get/put
- **Integration:** Used by `RecommendationService` to score recipes for the feed

### 2. Priority Queue (Max-Heap) — Feed Ranking
- **Where:** `data/RecipePriorityQueue.java`
- **What:** Scores each candidate recipe against user's interest profile, inserts into max-heap, pops top-N for the feed page
- **Complexity:** O(log n) insert, O(1) peek
- **Integration:** Used by `RecommendationService.generateFeed()` to order the infinite scroll

### 3. HashSet — Allergen/Blacklist Filtering
- **Where:** `User.blacklistedIngredients` stored as `HashSet<String>`
- **What:** Before scoring a recipe, check all its ingredients against the blacklist. If any match, exclude or penalize (-30 points)
- **Complexity:** O(1) lookup per ingredient
- **Integration:** Used by `AllergenService` before recipes enter the priority queue

### 4. Trie (Prefix Tree) — Ingredient Search Autocomplete
- **Where:** `data/Trie.java` + `data/TrieNode.java`
- **What:** All known ingredients stored in a trie. As the user types, the trie returns all matching suggestions
- **Complexity:** O(L) where L = length of typed prefix
- **Integration:** Powers autocomplete in `SearchFragment` ingredient input field

---

## OOP Principles to Demonstrate

The course grades heavily on these. Apply them deliberately and be ready to explain in checkoff interviews.

### Must-Have
- **Encapsulation / Information Hiding:** Private fields, purposeful getters/setters only (not auto-generated for everything). Use "Tell, Don't Ask" principle.
- **Separation of Concerns / SRP:** Each class has one job. UI code must not contain business logic. Services don't know about Android Views.
- **DRY:** Extract repeated logic into helper methods or base classes.
- **Meaningful naming:** Follow Google Java Style Guide (https://google.github.io/styleguide/javaguide.html).

### Should-Have
- **Inheritance:** e.g., `BaseFragment` with shared logic for all tab fragments.
- **Interfaces:** Define contracts like `OnRecipeInteractionListener`, `FeedDataSource`. Use for loose coupling between adapters and fragments.
- **Abstract classes:** e.g., `AbstractRecipeFilter` with concrete subclasses for allergen filter, cuisine filter, etc.
- **Polymorphism:** e.g., different `InteractionHandler` implementations for like, save, comment.

### Nice-to-Have (Design Patterns)
- **Observer Pattern:** LiveData/ViewModel already uses this — mention it in checkoff.
- **Strategy Pattern:** Swappable feed ranking strategies.
- **Adapter Design Pattern:** e.g. FeedAdapter for RecyclerView.
- **Repository Pattern:** Abstract data source (local cache vs. network).
- **Singleton:** `ApiClient`, `SessionManager`.
- **Builder Pattern:** For constructing complex `Recipe` or feed request objects.
- **Factory Pattern:** `CommentsFragment.newInstance(recipeId)` already uses this — mention it.
- **Concurrency:** Implement executor and handler using MainThread and BackgroundThread when calling any API Keys (To be Implemented ).


---

## Database Schema (Supabase / PostgreSQL)

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT UNIQUE NOT NULL,
    username TEXT UNIQUE NOT NULL,
    display_name TEXT,
    avatar_url TEXT,
    interest_profile JSONB DEFAULT '{}',
    blacklisted_ingredients TEXT[] DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE recipes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id UUID REFERENCES users(id),
    title TEXT NOT NULL,
    description TEXT,
    video_url TEXT NOT NULL,
    thumbnail_url TEXT,
    tags TEXT[] DEFAULT '{}',
    prep_time_minutes INT,
    cook_time_minutes INT,
    estimated_calories DOUBLE PRECISION,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE ingredients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT UNIQUE NOT NULL,
    calories_per_100g DOUBLE PRECISION,
    is_common_allergen BOOLEAN DEFAULT false,
    category TEXT
);

CREATE TABLE recipe_ingredients (
    recipe_id UUID REFERENCES recipes(id) ON DELETE CASCADE,
    ingredient_id UUID REFERENCES ingredients(id),
    quantity TEXT,
    is_optional BOOLEAN DEFAULT false,
    PRIMARY KEY (recipe_id, ingredient_id)
);

CREATE TABLE interactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    recipe_id UUID REFERENCES recipes(id),
    type TEXT NOT NULL,  -- 'like', 'save', 'not_interested', 'view'
    created_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE(user_id, recipe_id, type)
);

CREATE TABLE comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    recipe_id UUID REFERENCES recipes(id),
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE cookbooks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    name TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE cookbook_recipes (
    cookbook_id UUID REFERENCES cookbooks(id) ON DELETE CASCADE,
    recipe_id UUID REFERENCES recipes(id),
    added_at TIMESTAMPTZ DEFAULT now(),
    PRIMARY KEY (cookbook_id, recipe_id)
);
```

---

## API Endpoints (Spring Boot Backend)

### Auth
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/register` | Create new user |
| POST | `/api/auth/login` | Login, returns JWT |

### Feed
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/feed` | Get personalized feed (paginated) |
| GET | `/api/feed/following` | Get feed from followed users |

### Recipes
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/recipes/{id}` | Get recipe details |
| POST | `/api/recipes` | Create/upload a recipe |
| GET | `/api/recipes/search?ingredients=egg,tomato` | Search by ingredients |
| GET | `/api/recipes/{id}/alternatives` | AI-generated ingredient alternatives |

### Interactions
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/interactions/like/{recipeId}` | Like a recipe |
| POST | `/api/interactions/save/{recipeId}` | Save a recipe |
| POST | `/api/interactions/not-interested/{recipeId}` | Mark not interested |
| POST | `/api/comments/{recipeId}` | Add comment |

### User / Profile
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/users/me` | Get current user profile |
| PUT | `/api/users/me/preferences` | Update interest profile |
| PUT | `/api/users/me/blacklist` | Update allergen blacklist |
| GET | `/api/users/me/saved` | Get saved recipes |
| GET | `/api/users/me/cookbooks` | Get cookbooks |

---

## Build Order (Phase-by-Phase)

Follow this order. Each phase builds on the previous one. Phases map roughly to the weekly schedule.

### Phase 1: Backend Foundation (Week 5-6)
> The Android skeleton already exists. Focus on standing up the backend.

1. Create Spring Boot project at `backend/` (Spring Web, Spring Data JPA, PostgreSQL driver)
2. Set up Supabase project — create tables per schema above
3. Configure `application.yml` with Supabase connection string
4. Implement backend model classes (User, Recipe, Ingredient)
5. Implement repositories with Spring Data JPA
6. Implement `AuthController` with JWT-based register/login
7. Implement `RecipeController` with basic CRUD

### Phase 2: Connect Android to Backend (Week 6-7)
> Wire the existing UI skeleton to real data.

1. Add Retrofit + OkHttp + Gson dependencies to Android `build.gradle`
2. Implement `ApiClient.java` (Retrofit singleton) and `Constants.java`
3. Implement `SessionManager.java` (JWT storage in SharedPreferences)
4. Wire `LoginActivity` and `SignupActivity` to real auth endpoints
5. Wire `FeedAdapter` to fetch recipes from backend instead of mock data
6. Wire `CommentsFragment` to real comment endpoints

### Phase 3: Data Structures (Week 7-8)
> The 2D aspect — implement custom data structures.

1. Implement `Trie.java` and `TrieNode.java` with insert, search, autocomplete
2. Implement `RecipePriorityQueue.java` — custom max-heap backed by ArrayList
3. Implement user interest profile scoring logic using HashMap in `RecommendationService`
4. Implement allergen filtering using HashSet in `AllergenService`
5. Write unit tests for all four data structures

### Phase 4: Feed Algorithm & Search (Week 8-9)
> Make the feed smart and search functional.

1. Implement `FeedService` in backend — scoring recipes against user profile + priority queue
2. Implement `GET /api/feed` endpoint with pagination
3. Connect `RecommendationService` on Android to update scores on like/save/skip
4. Wire Trie into `SearchFragment` for ingredient autocomplete
5. Implement `GET /api/recipes/search` endpoint for ingredient-based filtering

### Phase 5: Recipe Detail, Upload & AI (Week 9-10)
> Build out remaining screens and AI integration.

1. Implement `RecipeDetailActivity` — ingredients checklist, instructions, allergen alert banner
2. Implement allergen alert: check recipe ingredients against user's HashSet blacklist
3. Implement `UploadActivity` — pick video, enter metadata, upload to Supabase Storage
4. Integrate OpenAI in Spring Boot — `OpenAIService` for calorie estimation
5. Implement alternative ingredient recommendations via OpenAI
6. Display nutritional info on recipe detail page

### Phase 6: Polish & Demo Prep (Week 10-11)
> Get ready for Checkoff 2 and Checkoff 3.

1. Add Glide for image/thumbnail loading (resolve TODO in `CommentAdapter`)
2. Handle edge cases: empty feed, network errors, loading states
3. UI polish: consistent styling, transitions, error messages
4. Seed database with 15-20 sample recipes with videos/thumbnails
5. Prepare demo flow for Checkoff 2 presentation
6. Record backup demo video
7. Add Room for local caching (optional, nice-to-have)

---

## Environment Variables / Secrets

### Backend (`backend/application-local.yml` — gitignored)
```yaml
supabase:
  url: https://YOUR_PROJECT.supabase.co
  key: YOUR_ANON_KEY
  db-url: jdbc:postgresql://db.YOUR_PROJECT.supabase.co:5432/postgres
  db-user: postgres
  db-password: YOUR_DB_PASSWORD

openai:
  api-key: YOUR_OPENAI_KEY

jwt:
  secret: YOUR_JWT_SECRET
```

### Android (`util/Constants.java`)
```java
public class Constants {
    public static final String BASE_URL = "http://10.0.2.2:8080/api/"; // emulator → host localhost
    // public static final String BASE_URL = "https://your-deployed-url.com/api/"; // production
}
```

**Never commit API keys to Git.** Use `.gitignore` for `application-local.yml`, `.env`, `local.properties`.

---

## Testing Checklist

Before each checkoff, verify:

- [ ] App launches without crashes
- [ ] User can register and log in
- [ ] Feed loads and scrolls infinitely (videos or thumbnails play)
- [ ] Tapping a recipe opens the detail view with ingredients and instructions
- [ ] Allergen alert banner shows when recipe contains blacklisted ingredients
- [ ] Like, save, and not-interested buttons work and update the feed
- [ ] Ingredient search with autocomplete works (Trie)
- [ ] Feed is personalized (liked tags appear more often)
- [ ] Profile shows saved recipes
- [ ] Video upload flow works end-to-end
- [ ] OpenAI calorie estimation returns results
- [ ] App handles no-network gracefully (error messages, not crashes)

---

## Git Workflow

- **Repository:** Private GitHub repo, add instructor handles as collaborators
- **Branching:** `main` (stable), `dev` (integration), feature branches (`feature/feed-ui`, `feature/trie-search`, etc.)
- **Commits:** Descriptive messages — `feat: implement Trie autocomplete for ingredient search`
- **No API keys in repo**

---

## External Resources

- Spring Boot: https://spring.io/projects/spring-boot
- Supabase Docs: https://supabase.com/docs
- OpenAI API: https://platform.openai.com/docs
- ExoPlayer: https://github.com/google/ExoPlayer
- Retrofit: https://square.github.io/retrofit/
- Glide: https://github.com/bumptech/glide
- Google Java Style Guide: https://google.github.io/styleguide/javaguide.html
- Android Developer Docs: https://developer.android.com

---

## Team (Group 31)

| ID | Name |
|----|------|
| 1009156 | Cliffton Owen Gunawan |
| 1009209 | Jeremy Leonard Purnomo |
| 1009180 | Brian Wong Wei Xiang |
| 1009147 | Giorgio Remiel Pohar |
| 1009185 | Vincent Alexander Yauvira |
| 1009143 | Jovyan |

---

## Working with Claude Code

1. **Always specify the module** — "in the Android app" or "in the Spring Boot backend"
2. **Reference phases** — e.g., "Implement Phase 3 step 1: Trie data structure"
3. **Existing code matters** — check the ✅/❌ markers in the package layout above before creating new files; some files already exist and should be modified, not recreated
4. **OOP compliance** — when implementing any class, follow the OOP principles section. Use private fields, meaningful names, SRP, and justify getters/setters
5. **When debugging** — share the full error stack trace and the relevant file
6. **Before checkoffs** — ask for an OOP review of the codebase to catch violations
