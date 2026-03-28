package com.example.foodtok.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.foodtok.R;
import com.example.foodtok.ui.ChatFragment;
import com.example.foodtok.ui.CreateFragment;
import com.example.foodtok.ui.HomeFragment;
import com.example.foodtok.ui.ProfileGuestFragment;
import com.example.foodtok.ui.SearchFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

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
                selectedFragment = new ChatFragment();
            } else if (itemId == R.id.nav_profile) {
                // Later: check AuthManager.isLoggedIn()
                // If logged in → ProfileUserFragment
                // If guest → ProfileGuestFragment
                selectedFragment = new ProfileGuestFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }

            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        // This swaps the fragment inside fragmentContainer
        // Like React: setActiveTab(fragment)
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}