# FoodTok Fragment Implementation Context

This document is a complete technical reference for the following fragments and their supporting classes in the FoodTok Android app. It is intended to be pasted into a fresh Claude conversation as context for explaining the implementation.

---

## Project Overview

- **Language:** Java (Android)
- **Single Activity pattern:** `MainActivity` hosts all fragments in `R.id.fragmentContainer` (a `FrameLayout`)
- **Navigation:** All fragment transitions use `getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment).addToBackStack(null).commit()`
- **Backend:** Supabase PostgREST (REST API with auto-generated endpoints from PostgreSQL schema)
- **HTTP client:** Retrofit2 with Gson for JSON parsing
- **Video playback:** ExoPlayer via `FeedVideoPlayerPool`
- **Image loading:** Glide
- **Auth:** `AuthManager` (in-memory singleton holding the current `User` domain object) + `SessionManager` (SharedPreferences for JWT persistence)

---

## App Navigation Map

```
MainActivity (BottomNavigationView with 5 tabs)
├── nav_home     → HomeFragment (infinite vertical video feed)
├── nav_search   → SearchFragment (Trie autocomplete search)
├── nav_create   → CreateFragment → UploadRecipeFragment
├── nav_chat     → GridFragment (explore grid) → GridFeedFragment (full-screen feed)
└── nav_profile  → ProfileUserFragment (logged in) / ProfileGuestFragment (guest)
                       ├── tap recipe → ProfileSavedFeedFragment
                       ├── tap Followers → FollowListFragment(MODE_FOLLOWERS)
                       └── tap Following → FollowListFragment(MODE_FOLLOWING)
                                               └── tap user → OtherUserProfileFragment
                                                               ├── tap recipe → ProfileSavedFeedFragment
                                                               ├── tap Followers → FollowListFragment(MODE_FOLLOWERS)
                                                               └── tap Following → FollowListFragment(MODE_FOLLOWING)
```

---

## Supabase Table Schema (relevant tables)

### `profiles`
| Column | Type | Notes |
|---|---|---|
| `id` | UUID | FK to auth.users |
| `username` | text | |
| `avatar_url` | text | nullable |

### `recipes`
| Column | Type |
|---|---|
| `id` | UUID |
| `author_id` | UUID (FK → profiles.id) |
| `title` | text |
| `video_url` | text |
| `thumbnail_url` | text |
| `tags` | text[] |
| `prep_time_minutes` | int |
| `cook_time_minutes` | int |
| `estimated_calories` | float |
| `created_at` | timestamptz |

### `follows`
| Column | Type | Meaning |
|---|---|---|
| `follower_id` | UUID | the user WHO follows |
| `following_id` | UUID | the user BEING followed |
| `created_at` | timestamptz | |

- **followers of user X** = rows where `following_id = X` → query `getFollowers("eq.X", ...)`
- **following of user X** = rows where `follower_id = X` → query `getFollowing("eq.X", ...)`

### `saved_recipes`
| Column | Type |
|---|---|
| `user_id` | UUID |
| `recipe_id` | UUID |
| `created_at` | timestamptz |

### `interactions`
| Column | Type | Notes |
|---|---|---|
| `user_id` | UUID | |
| `recipe_id` | UUID | |
| `type` | text | "like", "save", "not_interested", "view" |

---

## DTO Classes

### `UserDto.java`
```java
@SerializedName("id")         public String id;
@SerializedName("username")   public String username;
@SerializedName("avatar_url") public String avatarUrl;
```

### `FollowDto.java`
```java
@SerializedName("follower_id")  private String followerId;   // getter: getFollowerId()
@SerializedName("following_id") private String followingId;  // getter: getFollowingId()
@SerializedName("created_at")   private String createdAt;
```

### `RecipeDto.java` (key fields)
```java
@SerializedName("id")                    public String id;
@SerializedName("author_id")             public String authorId;
@SerializedName("title")                 public String title;
@SerializedName("video_url")             public String videoUrl;
@SerializedName("thumbnail_url")         public String thumbnailUrl;
@SerializedName("tags")                  public String[] tags;
@SerializedName("prep_time_minutes")     public int prepTimeMinutes;
@SerializedName("cook_time_minutes")     public int cookTimeMinutes;
@SerializedName("estimated_calories")    public double estimatedCalories;
@SerializedName("created_at")            public String createdAt;
// Method:
public Recipe toDomain() { ... }  // converts to domain model for use in FeedAdapter/PlayerPool
```

### `SavedRecipeDto.java`
```java
@SerializedName("user_id")   public String userId;
@SerializedName("recipe_id") public String recipeId;
@SerializedName("created_at") public String createdAt;
```

### `CreateFollowRequest.java`
```java
@SerializedName("follower_id")  private final String followerId;   // NOTE: was bugged as "followers_id" — fixed
@SerializedName("following_id") private final String followingId;
// Constructor: CreateFollowRequest(String followerId, String followingId)
```

---

## SupabaseApi.java — Retrofit Interface (relevant methods)

```java
// Recipes
@GET("recipes") Call<List<RecipeDto>> getRecipes(@Query("select") String, @Query("order") String, @Header("Range") String);
@GET("recipes") Call<List<RecipeDto>> getRecipesByAuthor(@Query("author_id") String authorIdFilter, @Query("select") String);
@GET("recipes") Call<List<RecipeDto>> getRecipesByIds(@Query("id") String idFilter, @Query("select") String);

// Follows
@GET("follows")  Call<List<FollowDto>> getFollowers(@Query("following_id") String, @Query("select") String);
@GET("follows")  Call<List<FollowDto>> getFollowing(@Query("follower_id") String, @Query("select") String);
@GET("follows")  Call<List<FollowDto>> checkFollow(@Query("follower_id") String, @Query("following_id") String, @Query("select") String);
@POST("follows") Call<List<FollowDto>> followUser(@Body CreateFollowRequest request);
@DELETE("follows") Call<Void> unfollowUser(@Query("follower_id") String, @Query("following_id") String);

// Profiles (read)
@GET("profiles") Call<List<UserDto>> getProfiles(@Query("id") String idFilter, @Query("select") String);

// Saved recipes
@GET("saved_recipes") Call<List<SavedRecipeDto>> getSavedRecipes(@Query("user_id") String, @Query("select") String, @Query("order") String);
```

**PostgREST filter syntax used in this app:**
- Exact match: `"eq." + userId` → e.g. `"eq.abc-123"`
- Multiple IDs: `"in.(id1,id2,id3)"` — built dynamically from a list
- Select fields: comma-separated column names e.g. `"id,username,avatar_url"`
- Order: `"created_at.desc"`

**All API calls are authenticated** — `ApiClient` attaches the JWT from `SessionManager` automatically via an OkHttp interceptor.

---

## 1. ProfileUserFragment

**File:** `app/src/main/java/com/example/foodtok/ui/ProfileUserFragment.java`
**Layout:** `app/src/main/res/layout/fragment_profile_user.xml`

### Purpose
The logged-in user's own profile screen. Shows their username, follower/following/recipe counts, and a tabbed recipe grid (My Recipes | Saved).

### Layout Structure
```
LinearLayout (vertical, foodtok_cream background)
├── RelativeLayout (header)
│   ├── tvUsername (TextView, top-left, bold 22sp)
│   └── btnLogout (TextView styled as button, top-right)
├── Divider
├── LinearLayout (profile info, centered vertical)
│   ├── ivProfilePic (ImageView, 80dp circle)
│   ├── tvDisplayName (TextView, bold 18sp)
│   ├── tvBio (TextView, secondary color)
│   └── LinearLayout (stats row, horizontal)
│       ├── LinearLayout (Recipes column)
│       │   ├── tvRecipeCount
│       │   └── "Recipes" label
│       ├── LinearLayout id=llFollowers (CLICKABLE — opens FollowListFragment)
│       │   ├── tvFollowerCount
│       │   └── "Followers" label
│       └── LinearLayout id=llFollowing (CLICKABLE — opens FollowListFragment)
│           ├── tvFollowingCount
│           └── "Following" label
├── Divider
├── LinearLayout (tabs)
│   ├── tabMyRecipes (TextView, bold when active)
│   └── tabSaved (TextView, bold when active)
├── Divider
└── RecyclerView id=rvProfileRecipes (3-column GridLayoutManager, weight=1)
```

### Fields
```java
RecyclerView rvProfileRecipes;
TextView tabMyRecipes, tabSaved;
TextView tvFollowerCount, tvFollowingCount, tvRecipeCount;
TextView userName, btnLogout, tvDisplayName;
View llFollowers, llFollowing;          // the clickable stat containers
boolean isMyRecipesTab = true;          // tracks which tab is active
List<RecipeDto> myRecipes;              // backing list for My Recipes tab
List<RecipeDto> savedRecipes;           // backing list for Saved tab
ProfileRecipeAdapter adapter;           // single adapter — data swapped on tab switch
```

### Key Methods

#### `onCreateView`
1. Inflates layout, binds all views
2. Sets `tvUsername` and `tvDisplayName` from `AuthManager.getInstance().getCurrentUser().getUsername()`
3. Logout button → calls `AuthServiceProvider.getAuthService().logout()` then restarts `MainActivity` with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`
4. **`llFollowers` click** → `FollowListFragment.newInstance(userId, FollowListFragment.MODE_FOLLOWERS)` → `replace + addToBackStack`
5. **`llFollowing` click** → `FollowListFragment.newInstance(userId, FollowListFragment.MODE_FOLLOWING)` → `replace + addToBackStack`
6. Sets up `GridLayoutManager(context, 3)` + `ProfileRecipeAdapter`
7. Recipe click listener → `ProfileSavedFeedFragment.newInstance(position, MODE_MY_RECIPES or MODE_SAVED, userId)`
8. Calls `fetchProfileStats()`, `fetchMyRecipes()`, `fetchSavedRecipes()`, `switchTab(true)` on load

#### `switchTab(boolean showMyRecipes)`
- Sets `isMyRecipesTab`
- Updates active tab text color (`foodtok_text_primary`) and typeface (`BOLD`)
- Inactive tab gets `foodtok_text_secondary` + `NORMAL`
- Calls `adapter.updateData(showMyRecipes ? myRecipes : savedRecipes)` — just swaps the reference and calls `notifyDataSetChanged()`

#### `fetchMyRecipes()`
- Calls `api.getRecipesByAuthor("eq." + userId, selectFields)`
- On success: clears + repopulates `myRecipes`, calls `adapter.updateData(myRecipes)` only if `isMyRecipesTab == true`

#### `fetchSavedRecipes()` — Two-step hydration
1. `api.getSavedRecipes("eq." + userId, "user_id,recipe_id,created_at", "created_at.desc")` → `List<SavedRecipeDto>`
2. From result, builds `"in.(id1,id2,...)"` string
3. `api.getRecipesByIds(inFilter, selectFields)` → `List<RecipeDto>`
4. Uses a `Map<String, RecipeDto>` keyed by `recipe.id` to re-order results to match the `saved_recipes` order (newest first)
5. Calls `adapter.updateData(savedRecipes)` only if `!isMyRecipesTab`

#### `fetchProfileStats()`
Three independent API calls fired in parallel (no `await`—all `.enqueue()`):
- `api.getFollowers("eq." + userId, "follower_id")` → `.body().size()` → `tvFollowerCount`
- `api.getFollowing("eq." + userId, "following_id")` → `.body().size()` → `tvFollowingCount`
- `api.getRecipesByAuthor("eq." + userId, "id")` → `.body().size()` → `tvRecipeCount`

---

## 2. ProfileSavedFeedFragment

**File:** `app/src/main/java/com/example/foodtok/ui/ProfileSavedFeedFragment.java`
**Layout:** `app/src/main/res/layout/fragment_profile_feed.xml`

### Purpose
A full-screen vertical swipeable video feed launched when a user taps a recipe thumbnail from a profile grid. Works for both own profile (My Recipes + Saved) and another user's profile (My Recipes mode). Hides the bottom navigation bar while open.

### Bundle Arguments
```java
static final String ARG_START_POSITION = "startPosition";  // int — which recipe to start on
static final String ARG_USER_ID        = "userId";          // String — whose recipes to load
static final String ARG_FEED_MODE      = "feedMode";        // String — "my_recipes" or "saved"
public static final String MODE_MY_RECIPES = "my_recipes";
public static final String MODE_SAVED      = "saved";
```

**Factory method:**
```java
ProfileSavedFeedFragment.newInstance(int startPosition, String feedMode, String userId)
```

### Layout Structure
```
ConstraintLayout
└── FrameLayout (black background, fills screen)
    ├── ViewPager2 id=vpProfileFeed (vertical orientation, fills screen)
    └── ImageButton id=btnBack (top-left, white arrow icon, 48dp, margin top=40dp)
```

### Fields
```java
ViewPager2 viewPager;
FeedAdapter feedAdapter;
FeedVideoPlayerPool playerPool;
List<Recipe> recipes;       // domain model list (not DTOs)
```

### Lifecycle Hooks
- **`onCreate`** → hides bottom nav: `((MainActivity) getActivity()).setBottomNavVisibility(false)`
- **`onDestroy`** → restores bottom nav: `setBottomNavVisibility(true)`
- **`onPause`** → `playerPool.pauseCurrent()`
- **`onResume`** → `playerPool.resumeCurrent()`
- **`onDestroyView`** → `playerPool.release(); playerPool = null;`

### Data Loading

**`loadMyRecipes()`:**
- Reads `ARG_USER_ID` and `ARG_START_POSITION` from args
- `api.getRecipesByAuthor("eq." + userId, selectFields)` → converts each `RecipeDto` to `Recipe` via `dto.toDomain()`
- → `initFeedAdapter(loadedRecipes, startPosition)`

**`loadSavedRecipes()`:**
- Same two-step hydration as `ProfileUserFragment.fetchSavedRecipes()` — fetch `saved_recipes` rows, then `getRecipesByIds(in.(...))`
- Preserves `created_at.desc` ordering using a HashMap reordering step
- → `initFeedAdapter(orderedRecipes, startPosition)`

### `initFeedAdapter(List<Recipe> recipes, int startPosition)`
The core setup method. Called after data loads:
1. Creates `FeedVideoPlayerPool(requireContext())` — manages ExoPlayer instances
2. `playerPool.setRecipes(recipes)` — gives pool the full list for pre-buffering
3. Creates `FeedAdapter(recipes, onRecipeInteractionListener, playerPool)`
4. `feedAdapter.setParentVerticalPager(viewPager)` — lets FeedAdapter coordinate with outer pager
5. Sets `OnHorizontalPageChangedListener` — when user swipes to video page (page=1), resumes player; swipes away (page≠1), pauses player
6. Registers `ViewPager2.OnPageChangeCallback` → `playerPool.setCurrentPosition(newPosition)` on page selection
7. Sets `viewPager.setOrientation(VERTICAL)`, `offscreenPageLimit(1)`
8. Sets adapter
9. If `startPosition > 0`, calls `viewPager.setCurrentItem(startPosition, false)` (no animation)
10. `viewPager.post(() -> playerPool.setCurrentPosition(currentItem))` — fires after first layout pass to start the right video

### Back Navigation
`btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack())`

---

## 3. FollowListFragment *(new)*

**File:** `app/src/main/java/com/example/foodtok/ui/FollowListFragment.java`
**Layout:** `app/src/main/res/layout/fragment_follow_list.xml`

### Purpose
A reusable scrollable list that shows either the followers or following of any given user. Used both from `ProfileUserFragment` (logged-in user) and `OtherUserProfileFragment` (another user). Tapping a row opens that user's `OtherUserProfileFragment`.

### Bundle Arguments
```java
private static final String ARG_USER_ID = "user_id";   // String UUID — whose list to show
private static final String ARG_MODE    = "mode";       // String — "followers" or "following"
public static final String MODE_FOLLOWERS = "followers";
public static final String MODE_FOLLOWING = "following";
```

**Factory method:**
```java
FollowListFragment.newInstance(String userId, String mode)
```

### Layout Structure
```
LinearLayout (vertical, foodtok_cream background)
├── RelativeLayout (header)
│   ├── btnBack (TextView styled as back button, left)
│   └── tvFollowListTitle (TextView, centered — "Followers" or "Following")
├── Divider
├── ProgressBar id=pbLoading (centered, GONE initially)
├── TextView id=tvEmpty (centered, "No users yet", GONE initially)
└── RecyclerView id=rvFollowList (LinearLayoutManager vertical, weight=1)
```

### Fields
```java
String userId;        // from args — whose follow list
String mode;          // from args — MODE_FOLLOWERS or MODE_FOLLOWING
RecyclerView rvFollowList;
ProgressBar pbLoading;
TextView tvEmpty, tvTitle;
FollowUserAdapter adapter;
List<UserDto> userList;
```

### `fetchFollowList()` — Two-step fetch
Shows spinner, then:

**If MODE_FOLLOWERS** (who follows this user):
1. `api.getFollowers("eq." + userId, "follower_id")` → `List<FollowDto>`
2. Extracts `dto.getFollowerId()` from each row → `List<String> ids`
3. → `fetchProfiles(ids)`

**If MODE_FOLLOWING** (who this user follows):
1. `api.getFollowing("eq." + userId, "following_id")` → `List<FollowDto>`
2. Extracts `dto.getFollowingId()` from each row → `List<String> ids`
3. → `fetchProfiles(ids)`

### `fetchProfiles(List<String> ids)`
- If `ids.isEmpty()` → `showEmpty()`
- Builds `"in.(id1,id2,...)"` string
- `api.getProfiles(inFilter, "id,username,avatar_url")` → `List<UserDto>`
- Hides spinner, passes list to `adapter.updateData(users)`, shows RecyclerView
- If empty response → `showEmpty()`

### `showEmpty()`
Hides spinner + RecyclerView, shows `tvEmpty` ("No users yet")

### User Click
```java
adapter.setOnUserClickListener(user -> {
    requireActivity().getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.fragmentContainer, OtherUserProfileFragment.newInstance(user.id))
        .addToBackStack(null)
        .commit();
});
```

### Back Navigation
`btnBack → requireActivity().getSupportFragmentManager().popBackStack()`

---

## 4. OtherUserProfileFragment *(new)*

**File:** `app/src/main/java/com/example/foodtok/ui/OtherUserProfileFragment.java`
**Layout:** `app/src/main/res/layout/fragment_other_user_profile.xml`

### Purpose
Displays another user's public profile. Similar layout to `ProfileUserFragment` but:
- No logout button
- No Saved tab (only shows that user's own recipes)
- Has a Follow / Unfollow button that reflects and changes the logged-in user's follow state
- Follower and following counts are clickable → open `FollowListFragment` for that user

### Bundle Arguments
```java
private static final String ARG_USER_ID = "user_id";  // String UUID — profile to display
```

**Factory method:**
```java
OtherUserProfileFragment.newInstance(String userId)
```

### Layout Structure
```
LinearLayout (vertical, foodtok_cream background)
├── RelativeLayout (header)
│   ├── btnBack (TextView, left — popBackStack)
│   ├── tvUsername (TextView, centered, bold 18sp)
│   └── btnFollowUnfollow (TextView styled as button, right — "Follow" or "Unfollow")
├── Divider
├── LinearLayout (profile info, centered vertical)
│   ├── ivProfilePic (ImageView, 80dp circle)
│   ├── tvDisplayName (TextView, bold 18sp)
│   └── LinearLayout (stats row)
│       ├── LinearLayout (Recipes column)
│       │   ├── tvRecipeCount
│       │   └── "Recipes" label
│       ├── LinearLayout id=llFollowers (CLICKABLE)
│       │   ├── tvFollowerCount
│       │   └── "Followers" label
│       └── LinearLayout id=llFollowing (CLICKABLE)
│           ├── tvFollowingCount
│           └── "Following" label
├── Divider
├── "Recipes" label (centered)
├── Divider
└── RecyclerView id=rvProfileRecipes (3-column GridLayoutManager, weight=1)
```

### Fields
```java
String profileUserId;         // from args — the user whose profile we're viewing
boolean isFollowing = false;  // current follow state, set by checkFollowState()
TextView tvUsername, tvDisplayName, tvRecipeCount, tvFollowerCount, tvFollowingCount;
TextView btnFollowUnfollow;
View llFollowers, llFollowing;
RecyclerView rvProfileRecipes;
List<RecipeDto> recipes;
ProfileRecipeAdapter adapter;  // reuses same adapter as ProfileUserFragment
```

### Data Loading (all fired on `onCreateView`)
Four independent async calls:

#### `fetchProfile()`
- `api.getProfiles("eq." + profileUserId, "id,username,avatar_url")`
- Takes first element of response list
- Sets `tvUsername.setText(user.username)` and `tvDisplayName.setText(user.username)`

#### `fetchRecipes()`
- `api.getRecipesByAuthor("eq." + profileUserId, selectFields)`
- On success: clears `recipes`, adds all, sets `tvRecipeCount`, calls `adapter.updateData(recipes)`

#### `fetchStats()`
- `api.getFollowers("eq." + profileUserId, "follower_id")` → `tvFollowerCount`
- `api.getFollowing("eq." + profileUserId, "following_id")` → `tvFollowingCount`

#### `checkFollowState()`
- Gets logged-in user ID from `AuthManager.getInstance().getCurrentUser().getId()`
- `api.checkFollow("eq." + myUserId, "eq." + profileUserId, "follower_id")`
- `isFollowing = !response.body().isEmpty()`
- Sets `btnFollowUnfollow.setText(isFollowing ? "Unfollow" : "Follow")`

### `toggleFollow()` — Follow/Unfollow Button
```
if (isFollowing):
    api.unfollowUser("eq." + myId, "eq." + profileUserId)
    → on success: isFollowing=false, setText("Follow"), refreshFollowerCount()

else:
    api.followUser(new CreateFollowRequest(myId, profileUserId))
    → on success: isFollowing=true, setText("Unfollow"), refreshFollowerCount()
```

### `refreshFollowerCount()`
Re-fetches `getFollowers("eq." + profileUserId, "follower_id")` and updates `tvFollowerCount`. Called immediately after a follow/unfollow completes so the displayed count stays accurate.

### Navigation
- **Back button** → `popBackStack()`
- **llFollowers click** → `FollowListFragment.newInstance(profileUserId, MODE_FOLLOWERS)` → `replace + addToBackStack`
- **llFollowing click** → `FollowListFragment.newInstance(profileUserId, MODE_FOLLOWING)` → `replace + addToBackStack`
- **Recipe grid click** → `ProfileSavedFeedFragment.newInstance(position, MODE_MY_RECIPES, profileUserId)` → `replace + addToBackStack`

---

## 5. GridFragment

**File:** `app/src/main/java/com/example/foodtok/ui/GridFragment.java`
**Layout:** `app/src/main/res/layout/fragment_grid.xml`

### Purpose
The "Explore" tab (nav_chat icon in bottom nav). Shows a 2-column thumbnail grid of recipes fetched from the feed endpoint. Tapping a recipe thumbnail opens `GridFeedFragment` full-screen at that position.

### Layout Structure
```
FrameLayout
├── RecyclerView id=rvGrid (2-column GridLayoutManager, fills screen)
└── ProgressBar id=gridLoadingSpinner (centered, visible initially)
```

### Data Loading
```java
RecipeServiceProvider.getRecipeService().getFeedRecipes(0, GRID_PAGE_SIZE=20, callback)
```
- Uses `RecipeServiceProvider` abstraction (not direct Retrofit call — goes through a service layer)
- On success: hides spinner, creates `GridAdapter(recipes, position -> openFeedAt(position))`, sets adapter
- On error: hides spinner, shows Toast

### `openFeedAt(int position)`
```java
Bundle args = new Bundle();
args.putInt("startPosition", position);
GridFeedFragment feedFragment = new GridFeedFragment();
feedFragment.setArguments(args);

requireActivity().getSupportFragmentManager()
    .beginTransaction()
    .setCustomAnimations(
        R.anim.feed_enter, R.anim.feed_exit,   // enter/exit for new fragment
        R.anim.feed_enter, R.anim.feed_exit    // enter/exit on back press
    )
    .replace(R.id.fragmentContainer, feedFragment)
    .addToBackStack(null)
    .commit();
```
Note: `GridFragment` is the only fragment that uses custom enter/exit animations (`feed_enter` / `feed_exit`).

---

## 6. GridFeedFragment

**File:** `app/src/main/java/com/example/foodtok/ui/GridFeedFragment.java`
**Layout:** `app/src/main/res/layout/fragment_grid_feed.xml`

### Purpose
A full-screen vertical swipeable video feed launched from `GridFragment`. Uses the same `FeedAdapter` + `FeedVideoPlayerPool` architecture as `ProfileSavedFeedFragment` but fetches its own copy of the recipe list (independent of the grid). Hides the bottom navigation bar.

### Layout Structure
```
FrameLayout (black background, fills screen)
├── ViewPager2 id=gridFeedViewPager (vertical, fills screen)
├── ProgressBar id=gridFeedLoadingSpinner (centered, visible initially)
└── ImageButton id=btnGridFeedBack (top-left, 48dp, margin top=48dp, start=8dp)
```

### Bundle Arguments
Passed manually (not via `newInstance` factory):
```java
args.putInt("startPosition", position);  // set by GridFragment
```
Read as: `getArguments().getInt("startPosition", 0)`

### Lifecycle Hooks
- **`onCreate`** → `((MainActivity) getActivity()).setBottomNavVisibility(false)`
- **`onDestroy`** → `setBottomNavVisibility(true)`
- **`onPause`** → `playerPool.pauseCurrent()`
- **`onResume`** → `playerPool.resumeCurrent()`
- **`onDestroyView`** → `playerPool.release(); playerPool = null;`

### Data Loading
```java
RecipeServiceProvider.getRecipeService().getFeedRecipes(0, FEED_PAGE_SIZE=20, callback)
```
- On success (on main thread via `getActivity().runOnUiThread()`): hides spinner, calls `initFeedAdapter(recipes)`

### `initFeedAdapter(List<Recipe> recipes)`
Nearly identical to `ProfileSavedFeedFragment.initFeedAdapter()`:
1. Creates `FeedVideoPlayerPool`, calls `setRecipes`
2. Creates `FeedAdapter` with `OnRecipeInteractionListener` (like, comment, save, not_interested)
3. `feedAdapter.setParentVerticalPager(gridFeedViewPager)`
4. Sets `OnHorizontalPageChangedListener` — pause/resume on horizontal swipe away from/to video page
5. Registers `OnPageChangeCallback` → `playerPool.setCurrentPosition(position)`
6. Sets adapter (no explicit `setOrientation` — ViewPager2 XML already has `orientation=vertical`)
7. Jumps to `startPosition` if > 0 (no animation)
8. `post(() -> playerPool.setCurrentPosition(currentItem))`

**Key difference from ProfileSavedFeedFragment:** `GridFeedFragment` does NOT call `viewPager.setOrientation(VERTICAL)` in Java (already set in XML). `ProfileSavedFeedFragment` DOES call it in Java.

### Back Navigation
`btnGridFeedBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack())`

---

## Supporting Adapters

### ProfileRecipeAdapter

**File:** `app/src/main/java/com/example/foodtok/adapters/ProfileRecipeAdapter.java`
**Layout:** `app/src/main/res/layout/item_profile_recipe.xml`

Used by both `ProfileUserFragment` and `OtherUserProfileFragment` for the 3-column recipe grid.

```java
// Interface
interface OnRecipeClickListener { void onRecipeClick(int position); }

// Constructor
ProfileRecipeAdapter(List<RecipeDto> recipes)

// Methods
void setOnRecipeClickListener(OnRecipeClickListener)
void updateData(List<RecipeDto> newRecipes)   // replaces list ref + notifyDataSetChanged()
int getItemCount()
```

**`onBindViewHolder`:**
- If `recipe.thumbnailUrl` is non-empty → `Glide.load(thumbnailUrl).centerCrop().into(ivRecipeThumb)`
- Else → `VideoThumbnailLoader.load(recipe.videoUrl, ivRecipeThumb)` (extracts first frame from video URL)
- Click → safe position check (`getAdapterPosition() != NO_POSITION`) then fires listener

**ViewHolder:** single `ImageView ivRecipeThumb = view.findViewById(R.id.ivRecipeThumb)`

---

### FollowUserAdapter *(new)*

**File:** `app/src/main/java/com/example/foodtok/adapters/FollowUserAdapter.java`
**Layout:** `app/src/main/res/layout/item_follow_user.xml`

Used by `FollowListFragment` for the vertical user list.

```java
// Interface
interface OnUserClickListener { void onUserClick(UserDto user); }

// Constructor
FollowUserAdapter(List<UserDto> users)

// Methods
void setOnUserClickListener(OnUserClickListener)
void updateData(List<UserDto> newUsers)    // replaces list ref + notifyDataSetChanged()
int getItemCount()
```

**`onBindViewHolder`:**
- `holder.tvUsername.setText(user.username)`
- If `user.avatarUrl` non-empty → `Glide.load(avatarUrl).circleCrop().into(ivAvatar)`
- Else → `ivAvatar.setImageResource(R.drawable.ic_burger_foodtok)` (placeholder)
- Click → safe position check then `clickListener.onUserClick(users.get(adapterPosition))`

**item_follow_user.xml layout:**
```
LinearLayout (horizontal, clickable, padding=16dp, foodtok_cream background)
├── ImageView id=ivUserAvatar (48dp, bg_circle_profile, padding=8dp, burger placeholder)
└── TextView id=tvFollowUsername (weight=1, bold 16sp, foodtok_text_primary)
```

**ViewHolder:** `ImageView ivAvatar` + `TextView tvUsername`

---

### GridAdapter

**File:** `app/src/main/java/com/example/foodtok/adapters/GridAdapter.java`
**Layout:** `app/src/main/res/layout/item_grid_recipe.xml`

Used by `GridFragment` for the 2-column explore grid. Works with domain `Recipe` objects (not DTOs).

```java
// Interface
interface OnItemClickListener { void onItemClick(int position); }

// Constructor (takes final list and listener — immutable after creation)
GridAdapter(List<Recipe> recipes, OnItemClickListener listener)
```

**`onBindViewHolder`:**
- `recipe.getThumbnailUrl()` non-empty → Glide load
- Else → `Glide.with(holder).clear(holder.ivRecipeThumb)` then `VideoThumbnailLoader.load(videoUrl, view)`
- Click → `listener.onItemClick(position)` (direct position, no safe-check needed since list is immutable)

---

## FeedVideoPlayerPool

**File:** `app/src/main/java/com/example/foodtok/util/FeedVideoPlayerPool.java`

Manages a window of ExoPlayer instances (prev/current/next) so that scrolling to the next video starts instantly because it's already buffered.

### Constants
```java
WINDOW = 2          // keeps players for positions [current-2 .. current+2], max 5 live players
MIN_BUFFER_MS = 2000
MAX_BUFFER_MS = 10000
BUFFER_FOR_PLAYBACK_MS = 1000
BUFFER_FOR_REBUFFER_MS = 2000
```

### Key Methods
```java
FeedVideoPlayerPool(Context context)
void setRecipes(List<Recipe> recipes)
void setCurrentPosition(int position)  // rotates pool window, evicts stale, plays current, pauses neighbours
void attach(int position, PlayerView view)   // called by FeedAdapter to bind player to UI
void detach(int position, PlayerView view)   // called on recycle/unbind
void pauseCurrent()    // called on fragment onPause
void resumeCurrent()   // called on fragment onResume
void release()         // releases ALL players — call from onDestroyView
```

### Player Config
Each player is built with:
- `DefaultMediaSourceFactory` backed by `VideoCache.getCacheFactory(appContext)` (disk cache — shared across all pools)
- Custom `LoadControl` with the buffer durations above
- `REPEAT_MODE_ONE` (videos loop)
- Error self-heal listener: on `onPlayerError`, calls `seekToDefaultPosition()` + `prepare()` to recover from network blips

---

## Navigation Patterns Summary

### Fragment Transaction Pattern (used everywhere)
```java
requireActivity().getSupportFragmentManager()
    .beginTransaction()
    .replace(R.id.fragmentContainer, targetFragment)
    .addToBackStack(null)
    .commit();
```

### Back Navigation (two styles)
1. `getParentFragmentManager().popBackStack()` — used in `ProfileSavedFeedFragment`
2. `requireActivity().getSupportFragmentManager().popBackStack()` — used in `FollowListFragment`, `OtherUserProfileFragment`, `GridFeedFragment`

### Bottom Nav Visibility
Fragments that go full-screen (`ProfileSavedFeedFragment`, `GridFeedFragment`) hide the bottom nav:
```java
// In onCreate:
((MainActivity) getActivity()).setBottomNavVisibility(false);
// In onDestroy:
((MainActivity) getActivity()).setBottomNavVisibility(true);
```

### Factory Method Pattern
New fragments always use static `newInstance()` with Bundle args:
```java
// ProfileSavedFeedFragment
ProfileSavedFeedFragment.newInstance(int startPosition, String feedMode, String userId)

// FollowListFragment
FollowListFragment.newInstance(String userId, String mode)

// OtherUserProfileFragment
OtherUserProfileFragment.newInstance(String userId)
```

---

## Full Navigation Chain (Follow Feature)

```
ProfileUserFragment
  [tap llFollowers/llFollowing]
      → FollowListFragment(userId, MODE_FOLLOWERS|MODE_FOLLOWING)
          [tap user row]
              → OtherUserProfileFragment(targetUserId)
                  [tap llFollowers/llFollowing]
                      → FollowListFragment(targetUserId, MODE_FOLLOWERS|MODE_FOLLOWING)
                  [tap recipe thumbnail]
                      → ProfileSavedFeedFragment(position, MODE_MY_RECIPES, targetUserId)
                  [tap Follow/Unfollow button]
                      → toggleFollow() in-place, refreshFollowerCount()

Back stack:
OtherUserProfileFragment → popBackStack → FollowListFragment → popBackStack → ProfileUserFragment
```

---

## Pre-existing Bug Fixed During This Session

**`CreateFollowRequest.java`:**
- **Before:** `@SerializedName("followers_id")` — typo with extra `s`
- **After:** `@SerializedName("follower_id")` — matches the actual Supabase column name
- **Impact:** Before the fix, `api.followUser(request)` sent `{"followers_id": "...", "following_id": "..."}` — PostgREST would ignore the unrecognized `followers_id` key, causing the insert to fail silently (null `follower_id` would violate NOT NULL or FK constraint, or create a broken row)

---

## OOP Design Patterns Used

| Pattern | Where |
|---|---|
| **Singleton** | `AuthManager`, `SessionManager`, `ApiClient`, `VideoCache` |
| **Factory Method** | `ProfileSavedFeedFragment.newInstance()`, `FollowListFragment.newInstance()`, `OtherUserProfileFragment.newInstance()` |
| **Interface/Callback** | `OnRecipeClickListener`, `OnUserClickListener`, `OnRecipeInteractionListener`, `RecipeListCallback`, `InteractionCallback` |
| **Adapter (GoF)** | All RecyclerView adapters translate data models to UI |
| **Object Pool** | `FeedVideoPlayerPool` — 3-5 slot ExoPlayer pool for O(1) video start |
| **Observer** | `ViewPager2.OnPageChangeCallback`, `FeedAdapter.OnHorizontalPageChangedListener` |
| **Service Provider / Abstraction** | `RecipeServiceProvider`, `InteractionServiceProvider`, `AuthServiceProvider` — decouple consumers from concrete implementations |
