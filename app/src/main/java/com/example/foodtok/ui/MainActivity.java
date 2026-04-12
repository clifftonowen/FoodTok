package com.example.foodtok.ui;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.foodtok.R;
import com.example.foodtok.auth.AuthManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/** Main app activity hosting the BottomNavigationView and fragment container. */
public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

    View root = findViewById(android.R.id.content);
    View fragmentContainer = findViewById(R.id.fragmentContainer); // Ensure this ID is correct

    ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
      // 1. Check if keyboard is open
      boolean isKeyboardOpen = insets.isVisible(WindowInsetsCompat.Type.ime());

      // 2. Hide or Show the Bottom Navigation bar
      bottomNav.setVisibility(isKeyboardOpen ? View.GONE : View.VISIBLE);

      // 3. Get the exact pixel height of the system keyboard
      int keyboardHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;

      // 4. Apply that height as padding to the bottom of the Fragment Container
      // If keyboard is closed, keyboardHeight is 0, returning it to normal.
      fragmentContainer.setPadding(0, 0, 0, keyboardHeight);

      return insets;
    });

    // Load HomeFragment by default when app opens
    if (savedInstanceState == null) {
      loadFragment(new HomeFragment());
    }

    // Handle tab switching
    bottomNav.setOnItemSelectedListener(item -> {
      Fragment selectedFragment = null;
      int itemId = item.getItemId();

      if (itemId == R.id.nav_home) {
        selectedFragment = new HomeFragment();
      } else if (itemId == R.id.nav_search) {
        selectedFragment = new SearchFragment();
      } else if (itemId == R.id.nav_create) {
        selectedFragment = new CreateFragment();
      } else if (itemId == R.id.nav_chat) {
        selectedFragment = new GridFragment();
      } else if (itemId == R.id.nav_profile) {
        // Later: check AuthManager.isLoggedIn()
        // If logged in → ProfileUserFragment
        // If guest → ProfileGuestFragment
        if (AuthManager.getInstance().isLoggedIn()){
          selectedFragment = new ProfileUserFragment();
        } else{
          selectedFragment = new ProfileGuestFragment();
        }
      }

      if (selectedFragment != null) {
        loadFragment(selectedFragment);
      }

      return true;
    });
  }

  private void loadFragment(Fragment fragment) {
    getSupportFragmentManager()
        .beginTransaction()
        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
        .replace(R.id.fragmentContainer, fragment)
        .commit();
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