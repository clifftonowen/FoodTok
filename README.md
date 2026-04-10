# FoodTok Recipe Discovery App 🍳
*50.001 1D Information Systems Design Project*

## Project Overview
Food Tok is an infinite scrolling app for cooks to share and discover various recipes from different parts of the world for everyone to make
## Architecture & Package Structure
To maintain Separation of Concerns, please place all new files in their designated packages:
* `models/` - Plain Java data entities (`User`, `Recipe`, `Ingredient`) and `dto/` sub-package for API request/response objects.
* `services/` - Business logic, API clients, and service interfaces with real + mock implementations.
* `ui/` - Android Activities and Fragments.
* `adapters/` - RecyclerView adapters.
* `data/` - Custom data structures (50.004 requirement: Trie, PriorityQueue).
* `auth/` - Authentication service interface, implementations, and session management.
* `util/` - Helpers (API client, session manager, constants).

```text
com.example.foodtok
├── adapters/
│   ├── FeedAdapter.java
│   ├── RecipePageAdapter.java
│   ├── CommentAdapter.java
│   ├── ChatMessageAdapter.java
│   ├── IngredientSuggestionAdapter.java
│   ├── SearchResultAdapter.java
│   └── OnRecipeInteractionListener.java
├── auth/
│   ├── IAuthService.java
│   ├── SupabaseAuthService.java
│   ├── MockAuthService.java
│   ├── AuthServiceProvider.java
│   ├── AuthManager.java
│   └── AuthCallback.java
├── data/
│   ├── Trie.java
│   └── TrieNode.java
├── models/
│   ├── Ingredient.java
│   ├── Recipe.java
│   ├── User.java
│   ├── Comment.java
│   ├── ChatMessage.java
│   ├── RecipeEnrichment.java
│   └── dto/ (API request/response DTOs)
├── services/
│   ├── SupabaseApi.java
│   ├── SupabaseAuthApi.java
│   ├── SupabaseStorageApi.java
│   ├── SupabaseRecipeService.java
│   ├── SupabaseCommentService.java
│   ├── SupabaseInteractionService.java
│   ├── I*Service.java (interfaces)
│   ├── Mock*Service.java (mock impls)
│   └── *ServiceProvider.java (factories)
├── ui/
│   ├── MainActivity.java
│   ├── HomeFragment.java
│   ├── SearchFragment.java
│   ├── OnboardingActivity.java
│   ├── LoginActivity.java
│   ├── SignupActivity.java
│   └── ...
└── util/
    ├── ApiClient.java
    ├── SessionManager.java
    └── Constants.java
```
## Database Schema (Supabase / PostgreSQL)

The app uses Supabase (PostgreSQL + PostgREST + Auth + Storage). There is no standalone `users` table — Supabase Auth manages accounts in `auth.users`, and the `profiles` table extends them with app-specific data.

### Actual Tables (as deployed)

| Table | Key Columns | Notes |
|-------|-------------|-------|
| **profiles** | `id` (FK → auth.users), `username`, `avatar_url`, `interest_profile` (JSONB), `blacklisted_ingredients` (TEXT[]), `created_at` | No `email`/`display_name` — email is in auth.users. |
| **recipes** | `id`, `author_id` (FK → profiles), `title`, `description`, `video_url` (nullable), `thumbnail_url`, `tags` (TEXT[]), `prep_time_minutes`, `cook_time_minutes`, `estimated_calories` (DOUBLE PRECISION), `created_at` | |
| **ingredients** | `id`, `name` (UNIQUE), `calories_per_100g`, `is_common_allergen`, `category` | |
| **recipe_ingredients** | `recipe_id` (FK), `ingredient_id` (FK), `quantity` (NOT NULL), `is_optional` | Composite PK on (recipe_id, ingredient_id) |
| **interactions** | `id`, `user_id` (FK), `recipe_id` (FK), `type` (CHECK: 'like','save','not_interested','view'), `created_at` | UNIQUE(user_id, recipe_id, type). |
| **comments** | `id`, `user_id` (FK), `recipe_id` (FK), `content`, `created_at` | |
| **cookbooks** | `id`, `user_id` (FK → profiles), `name`, `created_at` | |
| **cookbook_recipes** | `cookbook_id` (FK), `recipe_id` (FK), `added_at` | Composite PK |
| **followers** | `id`, `follower_id`, `following_id`, `created_at` | Missing FKs to profiles. Legacy table. |
| **follows** | `id`, `follower_id` (FK → profiles), `following_id` (FK → profiles), `created_at` | Has proper FKs. API endpoints wired. |

### Pending Migrations

```sql
-- No pending migrations — all applied.
```

## Git Workflow
* **DO NOT commit directly to the `main` branch.**
* Create a new branch for every feature you work on.
* Pull from `main` frequently to avoid merge conflicts.