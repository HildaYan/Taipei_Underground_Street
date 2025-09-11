package com.example.cameraproject_2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;

public class CreatAccount extends AppCompatActivity {

    private EditText editTextNewUsername;
    private EditText editTextEmail;
    private EditText editTextNewPassword;
    private EditText editTextConfirmPassword;
    private Button buttonConfirmRegister;
    private RegisterDatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;
    private ImageView backArrow; // 添加返回箭頭的 ImageView 變量
    private TextInputLayout textInputLayoutNewPassword;
    private TextInputLayout textInputLayoutConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_creat_account);

        // 初始化 SharedPreferences
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        // 初始化資料庫
        dbHelper = new RegisterDatabaseHelper(this);

        // 初始化 UI 元素
        editTextNewUsername = findViewById(R.id.editTextNewUsername);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextNewPassword = findViewById(R.id.editTextNewPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonConfirmRegister = findViewById(R.id.buttonConfirmRegister);
        backArrow = findViewById(R.id.backArrow);
        textInputLayoutNewPassword = findViewById(R.id.textInputLayoutNewPassword);
        textInputLayoutConfirmPassword = findViewById(R.id.textInputLayoutConfirmPassword);

        // 檢查 backArrow 是否成功初始化
        if (backArrow == null) {
            Log.e("CreatAccount", "backArrow not found in layout");
        } else {
            Log.d("CreatAccount", "backArrow found successfully");
        }

        // 設置返回箭頭的點擊事件
        backArrow.setOnClickListener(v -> {
            if (backArrow != null) {
                Intent intent = new Intent(CreatAccount.this, PersonalAccount.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
                finish();
            } else {
                Log.e("CreatAccount", "backArrow is null, cannot set click listener");
            }
        });

        // 接收從 PersonalAccount 傳遞的帳號和密碼（如果有）
        Intent intent = getIntent();
        String username = intent.getStringExtra("username");
        String password = intent.getStringExtra("password");
        if (username != null) editTextNewUsername.setText(username);
        if (password != null) {
            editTextNewPassword.setText(password);
            editTextConfirmPassword.setText(password);
        }

        // 設置確認註冊按鈕的點擊事件
        buttonConfirmRegister.setOnClickListener(v -> {
            String newUsername = editTextNewUsername.getText().toString().trim();
            String email = editTextEmail.getText().toString().trim();
            String newPassword = editTextNewPassword.getText().toString().trim();
            String confirmPassword = editTextConfirmPassword.getText().toString().trim();

            // 檢查所有欄位是否為空
            if (newUsername.isEmpty()) {
                editTextNewUsername.setError(getString(R.string.username_cannot_be_blank));
                editTextNewUsername.requestFocus();
                return;
            }

            if (email.isEmpty()) {
                editTextEmail.setError(getString(R.string.email_cannot_be_blank));
                editTextEmail.requestFocus();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                editTextEmail.setError(getString(R.string.invalid_email_address));
                editTextEmail.requestFocus();
                return;
            }

            if (newPassword.isEmpty()) {
                textInputLayoutNewPassword.setError(getString(R.string.password_cannot_be_blank));
                editTextNewPassword.requestFocus();
                return;
            }

            if (newPassword.length() < 6) {
                textInputLayoutNewPassword.setError(getString(R.string.password_min_length));
                editTextNewPassword.requestFocus();
                return;
            }

            if (confirmPassword.isEmpty()) {
                textInputLayoutConfirmPassword.setError(getString(R.string.confirm_password_cannot_be_blank));
                editTextConfirmPassword.requestFocus();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                textInputLayoutConfirmPassword.setError(getString(R.string.passwords_do_not_match));
                editTextConfirmPassword.requestFocus();
                return;
            }

            RegisterDatabaseHelper.RegistrationResult result = dbHelper.registerUser(newUsername, email, newPassword);
            if (result.success) {
                Toast.makeText(CreatAccount.this, "Registration successful! Your ID: " + result.id, Toast.LENGTH_LONG).show();
                Log.d("CreatAccount", "Registration successful for " + newUsername + ", ID: " + result.id);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("loggedInUser", newUsername);
                editor.putString("userId", result.id);
                editor.putBoolean("isLoggedIn", true);
                editor.apply();

                Intent mainIntent = new Intent(CreatAccount.this, MainActivity.class);
                startActivity(mainIntent);
                finish();
            } else {
                Toast.makeText(CreatAccount.this, "Registration failed: Username already exists", Toast.LENGTH_SHORT).show();
                Log.d("CreatAccount", "Registration failed for " + newUsername + ": Username already exists");
            }

            dbHelper.syncDatabase(null);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.closeDatabase();
        }
    }
}