package com.example.foodtok.ui;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.foodtok.R;
import com.example.foodtok.auth.AuthManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.FragmentManager;

/** Main app activity hosting the BottomNavigationView and fragment container. */
public class MainActivity extends AppCompatActivity {

  private static final String TAG_HOME = "tag_home";
  private static final String TAG_SEARCH = "tag_search";
  private static final String TAG_CREATE = "tag_create";
  private static final String TAG_CHAT = "tag_chat";
  private static final String TAG_PROFILE = "tag_profile";

  private Fragment activeFragment;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

    View root = findViewById(android.R.id.content);
    View fragmentContainer = findViewById(R.id.fragmentContainer);

    ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
      boolean isKeyboardOpen = insets.isVisible(WindowInsetsCompat.Type.ime());
      bottomNav.setVisibility(isKeyboardOpen ? View.GONE : View.VISIBLE);
      int keyboardHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
      fragmentContainer.setPadding(0, 0, 0, keyboardHeight);
      return insets;
    });

    if (savedInstanceState == null) {
      switchToFragment(TAG_HOME);
    } else {
      restoreActiveFragment();
    }

    bottomNav.setOnItemSelectedListener(item -> {
      int itemId = item.getItemId();

      if (itemId == R.id.nav_home) {
        switchToFragment(TAG_HOME);
      } else if (itemId == R.id.nav_search) {
        switchToFragment(TAG_SEARCH);
      } else if (itemId == R.id.nav_create) {
        switchToFragment(TAG_CREATE);
      } else if (itemId == R.id.nav_chat) {
        switchToFragment(TAG_CHAT);
      } else if (itemId == R.id.nav_profile) {
        switchToFragment(TAG_PROFILE);
      }

      return true;
    });

    bottomNav.setOnItemReselectedListener(item -> {
      if (item.getItemId() == R.id.nav_home) {
        Fragment home = getSupportFragmentManager().findFragmentByTag(TAG_HOME);
        if (home instanceof HomeFragment) {
          ((HomeFragment) home).refreshFeed();
        }
      }
    });
  }

  /**
   * Restores the active fragment reference after a configuration change
   * by finding the currently visible fragment among the known tags.
   */
  private void restoreActiveFragment() {
    String[] tags = {TAG_HOME, TAG_SEARCH, TAG_CREATE, TAG_CHAT, TAG_PROFILE};
    for (String tag : tags) {
      Fragment f = getSupportFragmentManager().findFragmentByTag(tag);
      if (f != null && !f.isHidden()) {
        activeFragment = f;
        return;
      }
    }
  }

  /**
   * Switches to the fragment identified by the given tag, using a
   * show/hide strategy so that fragments are retained across tab switches
   * instead of being recreated every time.
   *
   * <p>The profile tab is special-cased: it is always recreated to reflect
   * the current auth state (guest vs. logged-in user).
   */
  private void switchToFragment(String tag) {
    // Pop any overlay fragments (e.g. ProfileSavedFeedFragment,
    // OtherUserProfileFragment) so the base tab fragment is visible.
    FragmentManager fm = getSupportFragmentManager();
    if (fm.getBackStackEntryCount() > 0) {
      fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
      setBottomNavVisibility(true);
    }

    FragmentTransaction ft = fm.beginTransaction();
    ft.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);

    Fragment target;

    if (TAG_PROFILE.equals(tag)) {
      // Profile tab must always reflect current auth state, so recreate it.
      Fragment existing = getSupportFragmentManager().findFragmentByTag(TAG_PROFILE);
      if (existing != null) {
        ft.remove(existing);
      }
      target = createFragmentForTag(tag);
      ft.add(R.id.fragmentContainer, target, tag);
    } else {
      target = getSupportFragmentManager().findFragmentByTag(tag);
      if (target == null) {
        target = createFragmentForTag(tag);
        ft.add(R.id.fragmentContainer, target, tag);
      } else {
        ft.show(target);
      }
    }

    // Hide the previously active fragment.
    if (activeFragment != null && activeFragment != target) {
      ft.hide(activeFragment);
    }

    activeFragment = target;
    ft.commit();
  }

  private Fragment createFragmentForTag(String tag) {
    switch (tag) {
      case TAG_SEARCH:
        return new SearchFragment();
      case TAG_CREATE:
        return new CreateFragment();
      case TAG_CHAT:
        return new GridFragment();
      case TAG_PROFILE:
        if (AuthManager.getInstance().isLoggedIn()) {
          return new ProfileUserFragment();
        } else {
          return new ProfileGuestFragment();
        }
      case TAG_HOME:
      default:
        return new HomeFragment();
    }
  }

    public void setBottomNavVisibility(boolean visible) {
        findViewById(R.id.bottomNav).setVisibility(
                visible ? View.VISIBLE : View.GONE
        );
    }

  /**
   * Switches to the Search tab and runs an immediate search for the
   * given tag. Used by clickable hashtags in the feed.
   */
  public void navigateToSearchWithTag(String tag) {
    if (tag == null || tag.trim().isEmpty()) {
      return;
    }
    SearchFragment.setPendingTag(tag.trim());
    BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
    bottomNav.setSelectedItemId(R.id.nav_search);
  }
}