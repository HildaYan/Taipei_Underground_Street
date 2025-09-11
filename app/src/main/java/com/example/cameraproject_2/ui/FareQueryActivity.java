package com.example.cameraproject_2.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.cameraproject_2.ExitMapActivity;
import com.example.cameraproject_2.MainActivity;
import com.example.cameraproject_2.PersonalAccount;
import com.example.cameraproject_2.R;
import com.example.cameraproject_2.SettingsActivity;
import com.example.cameraproject_2.UserProfileActivity;
import com.example.cameraproject_2.chatroom_main;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FareQueryActivity extends AppCompatActivity {

    private AutoCompleteTextView autoCompleteStartStation;
    private AutoCompleteTextView autoCompleteEndStation;
    private Button buttonQueryFare;
    private TextView textViewFullFareResult;
    private TextView textViewConcessionFareResult;
    private TextView textViewTaipeiChildFareResult;
    private TextView textViewDistanceResult;
    private ProgressBar progressBar;
    private ImageView backArrow;
    private FrameLayout fabContainer;
    private BottomNavigationView bottomNavigationView;
    private SharedPreferences sharedPreferences;

    private FareQueryViewModel viewModel;
    private String selectedStartStation = null;
    private String selectedEndStation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fare_query);

        // 初始化 SharedPreferences
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        // 初始化視圖
        autoCompleteStartStation = findViewById(R.id.autoCompleteStartStation);
        autoCompleteEndStation = findViewById(R.id.autoCompleteEndStation);
        buttonQueryFare = findViewById(R.id.buttonQueryFare);
        textViewFullFareResult = findViewById(R.id.textViewFullFareResult);
        textViewConcessionFareResult = findViewById(R.id.textViewConcessionFareResult);
        textViewTaipeiChildFareResult = findViewById(R.id.textViewTaipeiChildFareResult);
        textViewDistanceResult = findViewById(R.id.textViewDistanceResult);
        progressBar = findViewById(R.id.progressBar);
        backArrow = findViewById(R.id.backArrow);
        fabContainer = findViewById(R.id.fab_container);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        viewModel = new ViewModelProvider(this).get(FareQueryViewModel.class);

        // 設置返回箭頭點擊事件
        backArrow.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
        });

        // 設置圓形圖標點擊事件
        fabContainer.setOnClickListener(v -> {
            Intent intent = new Intent(FareQueryActivity.this, ExitMapActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
        });

        // 設置底部導航欄
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.homefill) {
                Intent intent = new Intent(FareQueryActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.chat) {
                boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
                if (!isLoggedIn) {
                    Intent intent = new Intent(FareQueryActivity.this, PersonalAccount.class);
                    startActivity(intent);
                } else {
                    Set<String> groupNames = sharedPreferences.getStringSet("groupNames", new HashSet<>());
                    if (groupNames == null || groupNames.isEmpty()) {
                        Intent intent = new Intent(FareQueryActivity.this, chatroom_main.class);
                        startActivity(intent);
                    } else {
                        String defaultGroup = groupNames.iterator().next();
                        String membersString = sharedPreferences.getString(defaultGroup + "_members", "");
                        List<String> members = new ArrayList<>();
                        if (!membersString.isEmpty()) {
                            String[] membersArray = membersString.split(",");
                            for (String member : membersArray) {
                                members.add(member.trim());
                            }
                        }
                        Intent intent = new Intent(FareQueryActivity.this, chatroom_main.class);
                        intent.putExtra("groupName", defaultGroup);
                        intent.putStringArrayListExtra("members", new ArrayList<>(members));
                        startActivity(intent);
                    }
                }
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                return true;
            } else if (id == R.id.nav_member) {
                boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
                Intent intent = new Intent(FareQueryActivity.this, isLoggedIn ? UserProfileActivity.class : PersonalAccount.class);
                intent.putExtra("isLoggedIn", isLoggedIn);
                intent.putExtra("userId", sharedPreferences.getString("userId", getString(R.string.guest)));
                intent.putExtra("loggedInUser", sharedPreferences.getString("loggedInUser", getString(R.string.guest)));
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                return true;
            } else if (id == R.id.nav_info) {
                Intent intent = new Intent(FareQueryActivity.this, FareQueryActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_settings) {
                Intent intent = new Intent(FareQueryActivity.this, SettingsActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                return true;
            }
            return false;
        });

        setupStationDropdowns();
        setupButton();
        observeViewModel();
    }

    private void setupStationDropdowns() {
        ArrayAdapter<String> emptyAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        autoCompleteStartStation.setAdapter(emptyAdapter);
        autoCompleteEndStation.setAdapter(emptyAdapter);

        autoCompleteStartStation.setOnItemClickListener((parent, view, position, id) ->
                selectedStartStation = (String) parent.getItemAtPosition(position));
        autoCompleteEndStation.setOnItemClickListener((parent, view, position, id) ->
                selectedEndStation = (String) parent.getItemAtPosition(position));
    }

    private void setupButton() {
        buttonQueryFare.setOnClickListener(v -> {
            if (selectedStartStation != null && !selectedStartStation.isEmpty() &&
                    selectedEndStation != null && !selectedEndStation.isEmpty()) {
                clearResultTextViews();
                viewModel.queryFare(selectedStartStation, selectedEndStation);
            } else {
                Toast.makeText(this, "請選擇起點和終點站", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void observeViewModel() {
        viewModel.stationList.observe(this, stations -> {
            if (stations != null) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_dropdown_item_1line, stations);
                autoCompleteStartStation.setAdapter(adapter);
                autoCompleteEndStation.setAdapter(adapter);
            }
        });

        viewModel.fullFareResult.observe(this, fare -> {
            updateTextView(textViewFullFareResult, fare);
        });

        viewModel.concessionFareResult.observe(this, fare -> {
            updateTextView(textViewConcessionFareResult, fare);
        });

        viewModel.taipeiChildFareResult.observe(this, fare -> {
            updateTextView(textViewTaipeiChildFareResult, fare);
        });

        viewModel.distanceResult.observe(this, distance -> {
            updateTextView(textViewDistanceResult, distance);
        });

        viewModel.isLoading.observe(this, isLoading -> {
            if (isLoading != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                buttonQueryFare.setEnabled(!isLoading);
            }
        });

        viewModel.errorMessage.observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                clearResultTextViews();
            }
        });
    }

    private void updateTextView(TextView textView, String text) {
        if (text != null && !text.isEmpty()) {
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setText("");
            textView.setVisibility(View.GONE);
        }
    }

    private void clearResultTextViews() {
        updateTextView(textViewFullFareResult, null);
        updateTextView(textViewConcessionFareResult, null);
        updateTextView(textViewTaipeiChildFareResult, null);
        updateTextView(textViewDistanceResult, null);
    }
}