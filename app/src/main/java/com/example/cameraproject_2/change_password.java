package com.example.cameraproject_2;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class change_password extends AppCompatActivity {

    private TextInputEditText editTextOldPassword;
    private TextInputEditText editTextNewPassword;
    private TextInputEditText editTextConfirmPassword;
    private Button buttonConfirmPassword;
    private TextView textViewError;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editTextOldPassword = findViewById(R.id.edit_text_old_password);
        editTextNewPassword = findViewById(R.id.edit_text_new_password);
        editTextConfirmPassword = findViewById(R.id.edit_text_confirm_password);
        buttonConfirmPassword = findViewById(R.id.button_confirm_password);
        textViewError = findViewById(R.id.text_view_error);
        userId = getIntent().getStringExtra("userId");
        findViewById(R.id.backArrow).setOnClickListener(v -> finish());

        // 設置確認按鈕點擊事件
        buttonConfirmPassword.setOnClickListener(v -> {
            String oldPassword = editTextOldPassword.getText().toString().trim();
            String newPassword = editTextNewPassword.getText().toString().trim();
            String confirmPassword = editTextConfirmPassword.getText().toString().trim();

            if (oldPassword.isEmpty()) {
                editTextOldPassword.setError(getString(R.string.input_current_password));
                return;
            }

            if (newPassword.isEmpty()) {
                editTextNewPassword.setError(getString(R.string.input_new_password));
                return;
            }

            if (newPassword.length() < 6) {
                editTextNewPassword.setError(getString(R.string.new_password_length_error));
                return;
            }

            if (confirmPassword.isEmpty()) {
                editTextConfirmPassword.setError(getString(R.string.input_confirm_password));
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                textViewError.setText(getString(R.string.password_mismatch_error));
                return;
            }

            RegisterDatabaseHelper dbHelper = new RegisterDatabaseHelper(this);
            String currentPassword = dbHelper.getCurrentPassword(userId);
            if (currentPassword == null || !currentPassword.equals(oldPassword)) {
                editTextOldPassword.setError(getString(R.string.incorrect_current_password));
                return;
            }

            boolean success = dbHelper.updatePassword(userId, newPassword);
            if (success) {
                Toast.makeText(this, getString(R.string.password_updated), Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, getString(R.string.password_update_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }
}