package com.example.cameraproject_2;

import static com.example.cameraproject_2.RegisterDatabaseHelper.COL_ID;
import static com.example.cameraproject_2.RegisterDatabaseHelper.COL_IS_SYNCED;
import static com.example.cameraproject_2.RegisterDatabaseHelper.COL_SYNC_ACTION;
import static com.example.cameraproject_2.RegisterDatabaseHelper.COL_USERNAME;
import static com.example.cameraproject_2.RegisterDatabaseHelper.TABLE_NAME;
import static org.opencv.android.NativeCameraView.TAG;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UserProfileActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private RegisterDatabaseHelper dbHelper;
    private ImageView profileImage;
    private TextView textViewNameLabel;
    private TextView textViewAccountLabel;
    private LinearLayout changeNicknameLayout;
    private LinearLayout changePasswordLayout;
    private LinearLayout logoutLayout;
    private LinearLayout deleteAccountLayout;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private String originalUsername;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        dbHelper = new RegisterDatabaseHelper(this);

        // 綁定 UI 元素
        profileImage = findViewById(R.id.profile_image);
        textViewNameLabel = findViewById(R.id.text_view_name_label);
        textViewAccountLabel = findViewById(R.id.text_view_account_label);
        changeNicknameLayout = findViewById(R.id.change_nickname_layout);
        changePasswordLayout = findViewById(R.id.change_password_layout);
        logoutLayout = findViewById(R.id.logout_layout);
        deleteAccountLayout = findViewById(R.id.delete_account_layout);
        ImageView backArrow = findViewById(R.id.backArrow); // 綁定返回箭頭

        // 設置圖片選擇啟動器
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            if (userId != null && !userId.equals(getString(R.string.guest))) {
                                uploadProfileImage(selectedImageUri, userId);
                            } else {
                                Toast.makeText(this, getString(R.string.please_login), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );

        // 從 Intent 或 SharedPreferences 獲取狀態
        Intent intent = getIntent(); // 初始 Intent
        boolean isLoggedIn = intent.getBooleanExtra("isLoggedIn", sharedPreferences.getBoolean("isLoggedIn", false));
        String loggedInUser = intent.getStringExtra("username");
        userId = intent.getStringExtra("userId");
        if (loggedInUser == null) loggedInUser = sharedPreferences.getString("loggedInUser", getString(R.string.unknown_user));
        if (userId == null) userId = sharedPreferences.getString("userId", getString(R.string.unknown_id));

        // 根據狀態顯示內容
        if (isLoggedIn && !userId.equals(getString(R.string.guest))) {
            originalUsername = loggedInUser;
            textViewNameLabel.setText(getString(R.string.label_name) + loggedInUser);
            textViewAccountLabel.setText("ID: " + userId);

            // 載入頭像
            String profileImageUrl = intent.getStringExtra("profileImageUrl");
            if (profileImageUrl == null) {
                profileImageUrl = dbHelper.getProfileImageUrl(userId);
            }
            Log.d(TAG, "onCreate: userId=" + userId + ", profileImageUrl=" + profileImageUrl);

            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                Picasso.get().load(profileImageUrl)
                        .error(R.drawable.user)
                        .placeholder(R.drawable.user)
                        .into(profileImage, new com.squareup.picasso.Callback() {
                            @Override
                            public void onSuccess() {
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e(TAG, "Failed to load profile image: " + e.getMessage());
                                profileImage.setImageResource(R.drawable.user);
                            }
                        });
            } else {
                profileImage.setImageResource(R.drawable.user);
                Log.d(TAG, "No profile image URL, using default");
            }

            // 設置點擊事件
            changeNicknameLayout.setOnClickListener(v -> {
                Intent intentChangeUsername = new Intent(UserProfileActivity.this, change_username.class);
                intentChangeUsername.putExtra("userId", userId);
                intentChangeUsername.putExtra("originalUsername", originalUsername);
                startActivityForResult(intentChangeUsername, 1); // 使用 startActivityForResult
            });

            changePasswordLayout.setOnClickListener(v -> {
                Intent intentChangePassword = new Intent(UserProfileActivity.this, change_password.class);
                intentChangePassword.putExtra("userId", userId);
                startActivity(intentChangePassword);
            });

            logoutLayout.setOnClickListener(v -> logout());

            deleteAccountLayout.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.confirm_delete_title))
                        .setMessage(getString(R.string.confirm_delete_message))
                        .setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
                            deleteAccount(userId);
                            dialog.dismiss();
                        })
                        .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                        .show();
            });

            // 設置返回箭頭點擊事件
            backArrow.setOnClickListener(v -> {
                Intent mainIntent = new Intent(UserProfileActivity.this, MainActivity.class); // 更改變量名
                startActivity(mainIntent);
                finish(); // 關閉當前活動
            });

            // 設置圖標點擊事件（更改頭像）
            findViewById(R.id.circle_icon).setOnClickListener(v -> changeProfileImage());
        } else {
            textViewNameLabel.setText("姓名: 未登入");
            textViewAccountLabel.setText("ID: 未登入");
            changeNicknameLayout.setVisibility(View.GONE);
            changePasswordLayout.setVisibility(View.GONE);
            logoutLayout.setVisibility(View.GONE);
            deleteAccountLayout.setVisibility(View.GONE);
            profileImage.setImageResource(R.drawable.user);
            Toast.makeText(this, getString(R.string.please_login), Toast.LENGTH_SHORT).show();
            Intent personalIntent = new Intent(this, PersonalAccount.class);
            startActivity(personalIntent);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            String newUsername = data.getStringExtra("newUsername");
            if (newUsername != null) {
                originalUsername = newUsername;
                textViewNameLabel.setText(getString(R.string.label_name) + newUsername);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("loggedInUser", newUsername);
                editor.apply();
            }
        }
    }

    private void deleteAccount(String userId) {
        boolean success = dbHelper.deleteUser(userId);
        if (success) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isLoggedIn", false);
            editor.putString("loggedInUser", getString(R.string.guest));
            editor.putString("userId", getString(R.string.guest));
            editor.putString("profileImageUrl", null);
            editor.apply();
            Toast.makeText(this, getString(R.string.account_deleted), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, PersonalAccount.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, getString(R.string.account_deleted_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadProfileImage(Uri imageUri, final String userId) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        String url = "http://13.210.14.98/android_studio/upload_profile_image.php";

        File file = new File(getRealPathFromURI(imageUri));
        Log.d(TAG, "Uploading image for userId: " + userId + ", file path: " + file.getAbsolutePath() + ", file exists: " + file.exists() + ", file size: " + file.length());

        if (!file.exists() || file.length() == 0) {
            //runOnUiThread(() -> Toast.makeText(UserProfileActivity.this, "無效的圖片文件", Toast.LENGTH_SHORT).show());
            Log.e(TAG, "Invalid image file: exists=" + file.exists() + ", size=" + file.length());
            return;
        }

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("userId", userId)
                .addFormDataPart("profile_image", file.getName(),
                        RequestBody.create(file, MediaType.parse("image/*")))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //runOnUiThread(() -> Toast.makeText(UserProfileActivity.this, "上傳失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                Log.e(TAG, "Upload failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d(TAG, "Upload response: " + responseData);

                if (!responseData.trim().startsWith("{")) {
                    //runOnUiThread(() -> Toast.makeText(UserProfileActivity.this, "伺服器錯誤: 無效的回應格式", Toast.LENGTH_SHORT).show());
                    Log.e(TAG, "Invalid JSON response: " + responseData);
                    return;
                }

                try {
                    JSONObject jsonResponse = new JSONObject(responseData);
                    if (jsonResponse.getBoolean("success")) {
                        String imageUrl = jsonResponse.getString("profile_image_url");
                        Log.d(TAG, "Image uploaded, URL: " + imageUrl);
                        dbHelper.updateProfileImageUrl(userId, imageUrl);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("profileImageUrl", imageUrl);
                        editor.apply();
                        runOnUiThread(() -> {
                            Picasso.get().load(imageUrl)
                                    .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                                    .error(R.drawable.user)
                                    .placeholder(R.drawable.user)
                                    .into(profileImage, new com.squareup.picasso.Callback() {
                                        @Override
                                        public void onSuccess() {
                                            Log.d(TAG, "Profile image loaded successfully: " + imageUrl);
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            Log.e(TAG, "Failed to load profile image: " + e.getMessage());
                                            profileImage.setImageResource(R.drawable.user);
                                        }
                                    });
                            //Toast.makeText(UserProfileActivity.this, "圖片上傳成功", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        String message = jsonResponse.optString("message", getString(R.string.unknown_error));
                        //runOnUiThread(() -> Toast.makeText(UserProfileActivity.this, "上傳失敗: " + message, Toast.LENGTH_SHORT).show());
                        //Log.e(TAG, "Upload failed: " + message);
                    }
                } catch (JSONException e) {
                    //runOnUiThread(() -> Toast.makeText(UserProfileActivity.this, "JSON 錯誤: 無效的伺服器回應", Toast.LENGTH_SHORT).show());
                    Log.e(TAG, "JSON error: " + e.getMessage() + ", response: " + responseData);
                }
            }
        });
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(column_index);
        cursor.close();
        return path;
    }

    private void changeProfileImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    public void logAllUsers() {
        SQLiteDatabase db = dbHelper.getRegisterDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, null);
        while (cursor.moveToNext()) {
            String id = cursor.getString(cursor.getColumnIndexOrThrow(COL_ID));
            String username = cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME));
            String syncAction = cursor.getString(cursor.getColumnIndexOrThrow(COL_SYNC_ACTION));
            int isSynced = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_SYNCED));
            Log.d(TAG, "User: id=" + id + ", username=" + username + ", syncAction=" + syncAction + ", isSynced=" + isSynced);
        }
        cursor.close();
    }

    private void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", false);
        editor.putString("loggedInUser", getString(R.string.guest));
        editor.putString("userId", getString(R.string.guest));
        editor.putString("profileImageUrl", null);
        editor.apply();
        Toast.makeText(this, getString(R.string.logged_out), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, PersonalAccount.class);
        startActivity(intent);
        finish();
    }
}