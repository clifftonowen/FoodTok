# FoodTok Recipe Discovery App 🍳
*50.001 1D Information Systems Design Project*

## Project Overview
Food Tok is an infinite scrolling app for cooks to share and discover various recipes from different parts of the world for everyone to make
## Architecture & Package Structure
To maintain Separation of Concerns, please place all new files in their designated packages:
* `models/` - Plain Java data entities (`User`, `Recipe`, `Ingredient`).
* `services/` - Stateless business logic and algorithms (`RecommendationService`, `InteractionManager`).
* `ui/` - Android Activities and Fragments (`MainActivity`).
* `adapters/` - RecyclerView adapters (`FeedAdapter`).

```text
com.example.foodtok
├── adapters/
│   └── FeedAdapter.java
├── models/
│   ├── Ingredient.java
│   ├── Recipe.java
│   └── User.java
├── services/
│   ├── InteractionManager.java
│   ├── RecommendationService.java
│   └── Service.java
└── ui/
    └── MainActivity.java
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
| **followers** | `id`, `follower_id`, `following_id`, `created_at` | Missing FKs to profiles. |
| **follows** | `id`, `follower_id` (FK → profiles), `following_id` (FK → profiles), `created_at` | Has proper FKs. Team is consolidating followers/follows — do not modify either for now. |

### Pending Migrations

```sql
-- No pending migrations — all applied.
```

## Git Workflow
* **DO NOT commit directly to the `main` branch.**
* Create a new branch for every feature you work on.
* Pull from `main` frequently to avoid merge conflicts.