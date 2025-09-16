package com.example.cameraproject_2;

import static androidx.activity.result.ActivityResultCallerKt.registerForActivityResult;
import static com.example.cameraproject_2.RegisterDatabaseHelper.COL_GROUP_NAME;
import static com.example.cameraproject_2.RegisterDatabaseHelper.COL_INVITATION_ID;
import static com.example.cameraproject_2.RegisterDatabaseHelper.REGISTER_DB_NAME;
import static com.example.cameraproject_2.RegisterDatabaseHelper.TABLE_INVITATIONS;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.cameraproject_2.ui.FareQueryActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.unity3d.player.UnityPlayerActivity;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int REQUEST_CAMERA_PERMISSION_CODE = 1;
    private static final int REQUEST_NOTIFICATION_PERMISSION_CODE = 1002;
    private static final String INVITATION_CHECK_URL = "http://54.252.159.1/android_studio/fetch_invitations.php";
    private static final String USER_ID_KEY = "userId";
    private static final String LOGGED_IN_USER_KEY = "loggedInUser";
    private static final long CHECK_INTERVAL = 30000;

    private Uri photoUri;
    private File photoFile;
    private Bitmap currentBitmap;
    private String currentBitmapPath;

    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    private RegisterDatabaseHelper registerDbHelper;
    private PictureDatabaseHelper pictureDbHelper;
    private SQLiteDatabase database;

    private Handler handler = new Handler();
    private Runnable invitationChecker;
    private SharedPreferences sharedPreferences;

    private String currentUserId;
    private BottomNavigationView bottomNavigationView;
    private Spinner imageSourceSpinner;
    private Button buttonUpload;
    private ActivityResultLauncher<Intent> orbActivityLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyApplication app = (MyApplication) getApplication();
        app.setLocale();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = sharedPreferences.getString(USER_ID_KEY, getString(R.string.guest));
        String loggedInUser = sharedPreferences.getString(LOGGED_IN_USER_KEY, getString(R.string.guest));


        bottomNavigationView = findViewById(R.id.bottom_navigation);
        buttonUpload = findViewById(R.id.buttonupload);
        imageSourceSpinner = findViewById(R.id.imageSourceSpinner);

        // 新增 buttonCorrectLocation 初始化和點擊事件
        Button buttonCorrectLocation = findViewById(R.id.buttonCorrectLocation);
        buttonCorrectLocation.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, UnityPlayerActivity.class);
            startActivity(intent);
        });



        buttonUpload.setEnabled(false);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.homefill) {
                Toast.makeText(this, R.string.home_page, Toast.LENGTH_SHORT).show();
            } else if (id == R.id.chat) {
                boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
                Log.d("MainActivity", "Navigating to Chatroom, isLoggedIn: " + isLoggedIn);
                if (!isLoggedIn) {
                    Intent intent = new Intent(MainActivity.this, PersonalAccount.class);
                    startActivity(intent);
                } else {
                    Set<String> groupNames = sharedPreferences.getStringSet("groupNames", new HashSet<>());
                    if (groupNames == null || groupNames.isEmpty()) {
                        Intent intent = new Intent(MainActivity.this, chatroom_main.class);
                        startActivity(intent);
                    } else {
                        String defaultGroup = groupNames.iterator().next(); // 取第一個群組
                        String membersString = sharedPreferences.getString(defaultGroup + "_members", "");
                        List<String> members = new ArrayList<>();
                        if (!membersString.isEmpty()) {
                            String[] membersArray = membersString.split(",");
                            for (String member : membersArray) {
                                members.add(member.trim());
                            }
                        }
                        Intent intent = new Intent(MainActivity.this, chatroom_main.class);
                        intent.putExtra("groupName", defaultGroup);
                        intent.putStringArrayListExtra("members", new ArrayList<>(members));
                        startActivity(intent);
                    }
                }
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            } else if (id == R.id.nav_member) {
                // 保持原有邏輯不變
                boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
                Intent intent = new Intent(MainActivity.this, isLoggedIn ? UserProfileActivity.class : PersonalAccount.class);
                intent.putExtra("isLoggedIn", isLoggedIn);
                intent.putExtra("userId", sharedPreferences.getString("userId", getString(R.string.guest)));
                intent.putExtra("loggedInUser", sharedPreferences.getString("loggedInUser", getString(R.string.guest)));
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            } else if (id == R.id.nav_info) {
                Intent intent = new Intent(MainActivity.this, FareQueryActivity.class);
                startActivity(intent);
                Toast.makeText(this, R.string.taipei_info, Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_settings) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
            return true;
        });

        bottomNavigationView.setSelectedItemId(R.id.homefill);

        registerDbHelper = new RegisterDatabaseHelper(this);
        pictureDbHelper = new PictureDatabaseHelper(this);
        try {
            pictureDbHelper.createDataBase();
            registerDbHelper.getRegisterDatabase();
            database = pictureDbHelper.getPictureDatabase();
        } catch (IOException e) {
            //Log.e("MainActivity", "創建資料庫錯誤: " + e.getMessage());
            //Toast.makeText(this, "資料庫初始化失敗", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        IntentFilter filter = new IntentFilter("com.example.cameraproject_2.INVITATION_UPDATED");

        if (getIntent().getBooleanExtra("UPDATE_MENU", false)) {
            updateNavigationMenu();
        }

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Log.d("MainActivity", "照片拍攝成功，處理圖片...");
                        if (photoUri != null) {
                            Intent intent = new Intent(MainActivity.this, UploadImage.class);
                            intent.putExtra("photoUri", photoUri.toString());
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, getString(R.string.unable_to_get_photo), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        //Log.d("MainActivity", "拍照取消或失敗, resultCode: " + result.getResultCode());
                        Toast.makeText(this, getString(R.string.photo_cancelled_or_failed), Toast.LENGTH_SHORT).show();
                    }
                });

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            Intent intent = new Intent(MainActivity.this, UploadImage.class);
                            intent.putExtra("photoUri", selectedImageUri.toString());
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, getString(R.string.no_image_selected), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.image_selection_cancelled_or_failed), Toast.LENGTH_SHORT).show();
                    }
                });

        createNotificationChannel();
        startInvitationChecking();
        requestNotificationPermission();

        setupSpinner();

        // 處理按鈕點擊事件
        buttonUpload.setOnClickListener(v -> {
            // 模擬點擊後顯示下拉選單
            imageSourceSpinner.performClick();
        });
    }





    public void onInvitationAccepted(String invitationId) {
        registerDbHelper.updateInvitationStatus(invitationId, "accepted");
        registerDbHelper.syncInvitations(this, new RegisterDatabaseHelper.SyncCallback() {
            @Override
            public void onSyncComplete(boolean success) {
                runOnUiThread(() -> {
                    if (success) {
                        String groupName = getGroupNameFromInvitation(invitationId);
                        if (groupName != null) {
                            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                            boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
                            if (isLoggedIn) {
                                Intent intent = new Intent(MainActivity.this, UserProfileActivity.class);
                                intent.putExtra("isLoggedIn", true);
                                intent.putExtra("loggedInUser", sharedPreferences.getString("loggedInUser", getString(R.string.guest)));
                                intent.putExtra("userId", sharedPreferences.getString("userId", getString(R.string.user_id_label)));
                                startActivity(intent);
                                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                            } else {
                                Intent intent = new Intent(MainActivity.this, PersonalAccount.class);
                                startActivity(intent);
                                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                            }

                            String membersString = sharedPreferences.getString(groupName + "_members", "");
                            List<String> members = new ArrayList<>();
                            if (!membersString.isEmpty()) {
                                String[] membersArray = membersString.split(",");
                                for (String member : membersArray) {
                                    members.add(member.trim());
                                }
                            }
                            Intent chatIntent = new Intent(MainActivity.this, Chatroom.class);
                            chatIntent.putExtra("groupName", groupName);
                            chatIntent.putStringArrayListExtra("members", new ArrayList<>(members));
                            startActivity(chatIntent);
                            overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                        } else {
                            Log.e("MainActivity", getString(R.string.cannot_get_invitation_group_name) + invitationId);
                            Toast.makeText(MainActivity.this, getString(R.string.cannot_get_group_name), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private String getGroupNameFromInvitation(String invitationId) {
        SQLiteDatabase db = registerDbHelper.getRegisterDatabase();
        Cursor cursor = db.query(TABLE_INVITATIONS,
                new String[]{COL_GROUP_NAME},
                COL_INVITATION_ID + " = ?",
                new String[]{invitationId},
                null, null, null);
        String groupName = null;
        if (cursor.moveToFirst()) {
            groupName = cursor.getString(cursor.getColumnIndexOrThrow(COL_GROUP_NAME));
        }
        cursor.close();
        return groupName;
    }





    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null || !storageDir.exists()) storageDir.mkdirs();
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void captureImage() {
        Log.d("MainActivity", "開始 captureImage()");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity", "相機權限未授予，請求權限");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION_CODE);
            return;
        }

        try {
            photoFile = createImageFile();
        } catch (IOException e) {
            Log.e("MainActivity", "創建圖片文件錯誤: " + e.getMessage());
            Toast.makeText(this, getString(R.string.unable_to_create_image_file), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            photoUri = FileProvider.getUriForFile(this, "com.example.cameraproject_2.fileprovider", photoFile);
        } catch (IllegalArgumentException e) {
            Log.e("MainActivity", "使用 FileProvider 生成 URI 錯誤: " + e.getMessage());
            //Toast.makeText(this, "無法生成圖片 URI，請檢查 FileProvider 配置", Toast.LENGTH_LONG).show();
            return;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            takePictureLauncher.launch(takePictureIntent);
        } else {
            Log.e("MainActivity", "無可用相機應用處理意圖");
            Toast.makeText(this, getString(R.string.camera_app_not_found), Toast.LENGTH_LONG).show();
        }
    }

    private void openGallery() {
        Log.d("MainActivity", "開啟圖庫...");
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                captureImage();
            } else {
                Log.w("MainActivity", "相機或儲存權限被拒絕");
                Toast.makeText(this, getString(R.string.camera_or_storage_permission_denied), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.notification_permission_granted), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.notification_permission_required), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void checkInvitations() {
        if (currentUserId.equals("訪客")) {
            Log.d("MainActivity", "用戶未登入，跳過邀請檢查");
            return;
        }

        List<Invitation> invitations = registerDbHelper.getPendingInvitations(currentUserId);
        Log.d("MainActivity", "找到 " + invitations.size() + " 個待處理邀請，userId: " + currentUserId);

        for (Invitation invitation : invitations) {
            String groupName = invitation.getGroupName();
            if (!isGroupInPreferences(groupName)) {
                Log.d("MainActivity", "新邀請，群組: " + groupName);
                runOnUiThread(() -> {
                    showInvitationDialog(groupName, invitation.getInvitationId());
                    showInvitationNotification(groupName);
                });
                updateGroupInPreferences(groupName);
                //updateNavigationMenu();
            }
        }
    }

    private void showInvitationDialog(String groupName, String invitationId) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.group_invitation_title)
                .setMessage(getString(R.string.group_invitation_message, groupName))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    onInvitationAccepted(invitationId);
                    Toast.makeText(this, getString(R.string.accept_invitation_success, groupName), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    registerDbHelper.updateInvitationStatus(invitationId, "rejected");
                    Toast.makeText(this, getString(R.string.reject_invitation_success, groupName), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    private boolean isGroupInPreferences(String groupName) {
        Set<String> groupNames = sharedPreferences.getStringSet("groupNames", new HashSet<>());
        return groupNames.contains(groupName);
    }

    private void updateGroupInPreferences(String groupName) {
        Set<String> groupNames = sharedPreferences.getStringSet("groupNames", new HashSet<>());
        groupNames.add(groupName);
        sharedPreferences.edit().putStringSet("groupNames", groupNames).apply();
    }

    private void showInvitationNotification(String groupName) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION_CODE);
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "invitation_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(getString(R.string.new_group_notification_title))
                .setContentText(getString(R.string.new_group_notification_message, groupName))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Invitation Channel";
            String description = "Channel for group invitation notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("invitation_channel", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void startInvitationChecking() {
        invitationChecker = new Runnable() {
            @Override
            public void run() {
                registerDbHelper.syncInvitations(MainActivity.this, new RegisterDatabaseHelper.SyncCallback() {
                    @Override
                    public void onSyncComplete(boolean success) {
                        if (!success) {
                            Log.e("MainActivity", "邀請同步失敗");
                        } else {
                            String lastSyncTime = sharedPreferences.getString("last_sync_time", "0");
                            registerDbHelper.fetchInvitationsFromServer(MainActivity.this, currentUserId, lastSyncTime, new RegisterDatabaseHelper.SyncCallback() {
                                @Override
                                public void onSyncComplete(boolean success) {
                                    if (success) {
                                        SharedPreferences.Editor editor = sharedPreferences.edit();
                                        editor.putString("last_sync_time", String.valueOf(System.currentTimeMillis()));
                                        editor.apply();
                                        checkInvitations();
                                    }
                                }
                            });
                        }
                    }
                });
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };
        handler.post(invitationChecker);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //updateNavigationMenu();
        //updateHeader();
        registerDbHelper.checkInvitationStatus(sharedPreferences.getString("userId", "1"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && invitationChecker != null) {
            handler.removeCallbacks(invitationChecker);
        }
        if (registerDbHelper != null) registerDbHelper.closeDatabase();
        if (pictureDbHelper != null) pictureDbHelper.closeDatabase();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 處理螢幕旋轉邏輯，避免重新初始化資料庫
    }

    private void setupSpinner() {
        List<String> options = new ArrayList<>();

        options.add(getString(R.string.select_image_source));
        options.add(getString(R.string.from_camera));
        options.add(getString(R.string.from_picture));

        // 創建自定義適配器
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spinner_item_with_icons, R.id.spinner_text, options) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(R.id.spinner_text);
                ImageView icon1 = view.findViewById(R.id.icon1);
                ImageView icon2 = view.findViewById(R.id.icon2);

                textView.setGravity(Gravity.RIGHT);
                textView.setTextSize(15);
                textView.setTextColor(Color.BLACK);
                textView.setTypeface(null, Typeface.BOLD);
                if (position == 0) textView.setText(getString(R.string.upload));

                // 設置選中項的圖標
                icon1.setVisibility(View.GONE);
                icon2.setVisibility(View.GONE);
                if (position == 0) {
                    icon1.setImageResource(R.drawable.ic_camera);
                    icon2.setImageResource(R.drawable.ic_picture);
                    icon1.setVisibility(View.VISIBLE);
                    icon2.setVisibility(View.VISIBLE);
                } else if (position == 1) {
                    icon1.setImageResource(R.drawable.ic_camera);
                    icon1.setVisibility(View.VISIBLE);
                } else if (position == 2) {
                    icon1.setImageResource(R.drawable.ic_picture);
                    icon1.setVisibility(View.VISIBLE);
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = view.findViewById(R.id.spinner_text);
                ImageView icon1 = view.findViewById(R.id.icon1);
                ImageView icon2 = view.findViewById(R.id.icon2);


                // 動態設置圖標靠右
                LinearLayout.LayoutParams params1 = (LinearLayout.LayoutParams) icon1.getLayoutParams();
                params1.gravity = Gravity.END;
                icon1.setLayoutParams(params1);

                LinearLayout.LayoutParams params2 = (LinearLayout.LayoutParams) icon2.getLayoutParams();
                params2.gravity = Gravity.END;
                icon2.setLayoutParams(params2);

                textView.setGravity(Gravity.CENTER);
                textView.setTextSize(20);
                textView.setTextColor(Color.BLACK);
                textView.setTypeface(null, Typeface.BOLD);

                // 設置下拉項的圖標
                icon1.setVisibility(View.GONE);
                icon2.setVisibility(View.GONE);
                if (position == 1) {
                    icon1.setImageResource(R.drawable.ic_camera);
                    icon1.setVisibility(View.VISIBLE);
                } else if (position == 2) {
                    icon1.setImageResource(R.drawable.ic_picture);
                    icon1.setVisibility(View.VISIBLE);
                }
                return view;
            }
        };

        // 設置下拉視圖資源
        adapter.setDropDownViewResource(R.layout.spinner_item_with_icons);
        imageSourceSpinner.setAdapter(adapter);

        imageSourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedOption = options.get(position);
                if (position == 0) {
                    return;
                } else if (selectedOption.equals(getString(R.string.from_camera))) {
                    captureImage();
                } else if (selectedOption.equals(getString(R.string.from_picture))) {
                    openGallery();
                }
                imageSourceSpinner.setSelection(0); // 選擇後恢復到默認項
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
}