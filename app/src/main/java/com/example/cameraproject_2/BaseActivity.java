package com.example.cameraproject_2;

import static org.opencv.android.NativeCameraView.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class BaseActivity extends AppCompatActivity {

    protected SharedPreferences sharedPreferences;
    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected ActionBarDrawerToggle toggle;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        preferenceChangeListener = (sharedPrefs, key) -> {
            if (key.equals("isLoggedIn") || key.equals("loggedInUser") || key.equals("userId") || key.equals("groupNames")) {
                updateHeader();
                updateNavigationMenu();
                if (navigationView != null) {
                    navigationView.getMenu().clear();
                    updateNavigationMenu();
                    navigationView.requestLayout();
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        updateHeader();
        Log.d("BaseActivity", "onStart: Navigation menu updated");
    }

    @Override
    protected void onStop() {
        super.onStop();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    protected void updateNavigationMenu() {
        Menu menu = navigationView.getMenu();
        menu.clear();
        Log.d(TAG, "Menu cleared, inflating nav_menu");
        Set<String> groupNames = sharedPreferences.getStringSet("groupNames", new HashSet<>());
        Log.d(TAG, "Group names from SharedPreferences: " + groupNames);
        for (String groupName : groupNames) {
            menu.add(groupName).setOnMenuItemClickListener(item -> {
                startChatroomActivity(groupName);
                return true;
            });
            Log.d(TAG, "Added group: " + groupName);
        }
        navigationView.invalidate();
    }

    protected void startChatroomActivity(String groupName) {
        Intent intent = new Intent(this, Chatroom.class);
        intent.putExtra("groupName", groupName);
        Set<String> groupNames = sharedPreferences.getStringSet("groupNames", new HashSet<>());
        if (groupNames.contains(groupName)) {
            String membersString = sharedPreferences.getString(groupName + "_members", "");
            List<String> members = new ArrayList<>();
            if (!membersString.isEmpty()) {
                String[] membersArray = membersString.split(",");
                for (String member : membersArray) {
                    members.add(member.trim());
                }
            }
            intent.putStringArrayListExtra("members", new ArrayList<>(members));
        }
        startActivity(intent);
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    protected void updateHeader() {
        if (navigationView == null) {
            return;
        }

        View headerView = navigationView.getHeaderView(0);
        headerView.invalidate();
        headerView.requestLayout();
    }

    protected void setupDrawer() {
        if (drawerLayout == null || navigationView == null) {
            Log.e("BaseActivity", "drawerLayout or navigationView is null");
            return;
        }

        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }
    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}