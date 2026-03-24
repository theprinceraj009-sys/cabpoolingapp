package com.princeraj.campustaxipooling;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.princeraj.campustaxipooling.ui.feed.RideFeedFragment;
import com.princeraj.campustaxipooling.ui.myrides.MyRidesFragment;
import com.princeraj.campustaxipooling.ui.chat.ChatListFragment;
import com.princeraj.campustaxipooling.ui.profile.ProfileFragment;

/**
 * Shell activity hosting the 4 bottom-navigation tabs.
 * Each tab is a Fragment — no Activity switching for tab changes.
 */
public class HomeActivity extends BaseActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        bottomNav = findViewById(R.id.bottomNav);

        // Load default tab
        if (savedInstanceState == null) {
            loadFragment(new RideFeedFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected;
            int id = item.getItemId();

            if (id == R.id.nav_feed) {
                selected = new RideFeedFragment();
            } else if (id == R.id.nav_my_rides) {
                selected = new MyRidesFragment();
            } else if (id == R.id.nav_chat) {
                selected = new ChatListFragment();
            } else if (id == R.id.nav_profile) {
                selected = new ProfileFragment();
            } else {
                return false;
            }

            loadFragment(selected);
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
