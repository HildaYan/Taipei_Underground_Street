package com.example.cameraproject_2;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cameraproject_2.ui.FareQueryActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.unity3d.player.UnityPlayerActivity;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UploadImage extends AppCompatActivity {

    static {
        System.loadLibrary("opencv_java4");
    }

    private static final String KEY_PHOTO_URI = "photoUri";
    private static final String KEY_CURRENT_BITMAP_PATH = "currentBitmapPath";
    private static final String KEY_LAST_LOCATION = "lastLocation";

    private BottomNavigationView bottomNavigationView;
    private ImageView bigmap;
    private ImageView smallmap;
    private TextView currentLocationTextView;
    private Button buttonCorrectLocation;
    private Button buttonIncorrectLocation;
    private Button buttonUpload;
    private Button sendToChatButton;
    private Uri photoUri;
    private Bitmap currentBitmap;
    private String currentBitmapPath;
    private ArrayList<MatchResult> topMatches = new ArrayList<>();
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private SharedPreferences sharedPreferences;
    private SharedPreferences errorRecords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_upload_image);

        // Initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "無法載入 OpenCV");
            finish();
            return;
        }
        Log.d("OpenCV", "OpenCV 載入成功");

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        errorRecords = getSharedPreferences("ErrorRecords", MODE_PRIVATE);

        // Initialize UI components
        bigmap = findViewById(R.id.bigmap);
        smallmap = findViewById(R.id.smallmap);
        currentLocationTextView = findViewById(R.id.currentLocationTextView);
        buttonCorrectLocation = findViewById(R.id.buttonCorrectLocation);
        buttonIncorrectLocation = findViewById(R.id.buttonIncorrectLocation);
        buttonUpload = findViewById(R.id.buttonupload);
        sendToChatButton = findViewById(R.id.sendtochat);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (bigmap == null || smallmap == null || currentLocationTextView == null ||
                buttonCorrectLocation == null || buttonIncorrectLocation == null ||
                buttonUpload == null || sendToChatButton == null || bottomNavigationView == null) {
            Log.e("UploadImage", "UI components not found in layout, check activity_upload_image.xml");
            finish();
            return;
        }

        // Setup BottomNavigationView
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.homefill) {
                Toast.makeText(this, R.string.home_page, Toast.LENGTH_SHORT).show();
            } else if (id == R.id.chat) {
                boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
                Log.d("MainActivity", "Navigating to Chatroom, isLoggedIn: " + isLoggedIn);
                if (!isLoggedIn) {
                    Intent intent = new Intent(UploadImage.this, PersonalAccount.class);
                    startActivity(intent);
                } else {
                    Set<String> groupNames = sharedPreferences.getStringSet("groupNames", new HashSet<>());
                    if (groupNames == null || groupNames.isEmpty()) {
                        Intent intent = new Intent(UploadImage.this, chatroom_main.class);
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
                        Intent intent = new Intent(UploadImage.this, chatroom_main.class);
                        intent.putExtra("groupName", defaultGroup);
                        intent.putStringArrayListExtra("members", new ArrayList<>(members));
                        startActivity(intent);
                    }
                }
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            } else if (id == R.id.nav_member) {
                boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
                Intent intent = new Intent(UploadImage.this, isLoggedIn ? UserProfileActivity.class : PersonalAccount.class);
                intent.putExtra("isLoggedIn", isLoggedIn);
                intent.putExtra("userId", sharedPreferences.getString("userId", "訪客"));
                intent.putExtra("loggedInUser", sharedPreferences.getString("loggedInUser", "訪客"));
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            } else if (id == R.id.nav_info) {
                Intent intent = new Intent(UploadImage.this, FareQueryActivity.class);
                startActivity(intent);
                Toast.makeText(this, R.string.taipei_info, Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_settings) {
                Intent intent = new Intent(UploadImage.this, SettingsActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
            return true;
        });
        bottomNavigationView.setSelectedItemId(R.id.homefill);

        bigmap.setScaleType(ImageView.ScaleType.FIT_CENTER);
        smallmap.setScaleType(ImageView.ScaleType.FIT_CENTER);
        buttonCorrectLocation.setEnabled(false);
        buttonIncorrectLocation.setEnabled(false);
        buttonUpload.setEnabled(false);

        // Initialize ActivityResultLauncher for ORBActivity and WhereLocation
        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent resultIntent = result.getData();
                        String locationFromORB = resultIntent.getStringExtra("location");
                        Log.d("UploadImage", "Received location from ORBActivity: " + locationFromORB);

                        if (locationFromORB != null && !locationFromORB.isEmpty() && !locationFromORB.equals("未知")) {
                            // 保存成功比對的地點到 SharedPreferences
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(KEY_LAST_LOCATION, locationFromORB);
                            editor.apply();
                            Log.d("UploadImage", "Saved last location to SharedPreferences: " + locationFromORB);

                            currentLocationTextView.setText("Location: " + locationFromORB);
                            Log.d("UploadImage", "Updated currentLocationTextView to: " + locationFromORB);

                            ArrayList<MatchResult> matches = resultIntent.getParcelableArrayListExtra("topMatches");
                            if (matches != null && !matches.isEmpty()) {
                                topMatches.clear();
                                topMatches.addAll(matches);
                                Log.d("UploadImage", "Received topMatches size: " + topMatches.size());

                                if (!topMatches.isEmpty()) {
                                    MatchResult bestMatch = topMatches.get(0);
                                    String imageUriString = bestMatch.getUri();
                                    Log.d("UploadImage", "Best match URI: " + imageUriString);
                                    String fileName = imageUriString.replace("file://assets/", "");
                                    Log.d("UploadImage", "Attempting to load file: " + fileName);
                                    try {
                                        File imageFile = new File(getFilesDir(), fileName);
                                        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                                        if (bitmap != null) {
                                            smallmap.setImageBitmap(bitmap);
                                            Log.d("UploadImage", "Set smallmap image: " + fileName);
                                        } else {
                                            Log.e("UploadImage", "Failed to decode bitmap for smallmap: " + fileName);
                                            //Toast.makeText(this, "無法加載匹配的地圖圖片", Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (Exception e) {
                                        Log.e("UploadImage", "Failed to load smallmap image: " + fileName + ", Error: " + e.getMessage());
                                        //Toast.makeText(this, "無法加載匹配的地圖圖片", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }

                            if (!locationFromORB.equals("未知") && !locationFromORB.isEmpty()) {
                                buttonCorrectLocation.setEnabled(true);
                                buttonIncorrectLocation.setEnabled(true);
                            } else {
                                buttonCorrectLocation.setEnabled(false);
                                buttonIncorrectLocation.setEnabled(false);
                            }
                        } else {
                            Log.w("UploadImage", "locationFromORB is null or empty or unknown");
                            String selectedLocation = resultIntent.getStringExtra("selectedLocation");
                            String selectedImageUri = resultIntent.getStringExtra("selectedImageUri");
                            if (selectedLocation != null && !selectedLocation.isEmpty()) {
                                // 保存手動選擇的地點到 SharedPreferences
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString(KEY_LAST_LOCATION, selectedLocation);
                                editor.apply();
                                Log.d("UploadImage", "Saved selected location to SharedPreferences: " + selectedLocation);

                                currentLocationTextView.setText("Location: " + selectedLocation);
                                Toast.makeText(this, getString(R.string.location_updated) + selectedLocation, Toast.LENGTH_SHORT).show();
                                buttonCorrectLocation.setEnabled(true);
                                buttonIncorrectLocation.setEnabled(true);

                                if (selectedImageUri != null && !selectedImageUri.isEmpty()) {
                                    Log.d("UploadImage", "Received selectedImageUri: " + selectedImageUri);
                                    String fileName = selectedImageUri.replace("file://assets/", "");
                                    try {
                                        File imageFile = new File(getFilesDir(), fileName);
                                        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                                        if (bitmap != null) {
                                            smallmap.setImageBitmap(bitmap);
                                            Log.d("UploadImage", "Set smallmap image from WhereLocation: " + fileName);
                                        } else {
                                            Log.e("UploadImage", "Failed to decode bitmap for smallmap: " + fileName);
                                            //Toast.makeText(this, "無法加載選中的地圖圖片", Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (Exception e) {
                                        Log.e("UploadImage", "Failed to load image from WhereLocation: " + fileName + ", Error: " + e.getMessage());
                                        //Toast.makeText(this, "無法加載選中的地圖圖片", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            } else {
                                Toast.makeText(this, getString(R.string.location_not_selected), Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Log.w("UploadImage", "Result code is not OK or data is null");
                        Toast.makeText(this, getString(R.string.operation_cancelled), Toast.LENGTH_SHORT).show();
                    }
                });

        // Button click listeners
        buttonCorrectLocation.setOnClickListener(v -> {
            Intent intent = new Intent(UploadImage.this, UnityPlayerActivity.class);
            startActivity(intent);
        });

        buttonIncorrectLocation.setOnClickListener(v -> {
            if (topMatches.isEmpty()) {
                return;
            }
            Intent intent = new Intent(UploadImage.this, WhereLocation.class);
            intent.putParcelableArrayListExtra("topMatches", topMatches);
            intent.putExtra("photoUri", photoUri.toString());
            activityResultLauncher.launch(intent);
        });

        buttonUpload.setOnClickListener(v -> {
            Intent intent = new Intent(UploadImage.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // "分享位置" 按鈕點擊事件
        sendToChatButton.setOnClickListener(v -> {
            Set<String> groupNames = sharedPreferences.getStringSet("groupNames", new HashSet<>());
            if (groupNames == null || groupNames.isEmpty()) {
                Toast.makeText(this, getString(R.string.not_joined_any_chatroom), Toast.LENGTH_SHORT).show();
                return;
            }

            final boolean[] checkedItems = new boolean[groupNames.size()];
            final ArrayList<String> groupList = new ArrayList<>(groupNames);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.select_chatroom))
                    .setMultiChoiceItems(groupList.toArray(new String[0]), checkedItems,
                            (dialog, which, isChecked) -> checkedItems[which] = isChecked)
                    .setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
                        String currentLocation = currentLocationTextView.getText().toString().replace("Location: ", "");
                        for (int i = 0; i < checkedItems.length; i++) {
                            if (checkedItems[i]) {
                                String selectedGroup = groupList.get(i);
                                sendLocationToChatroom(selectedGroup, currentLocation);
                            }
                        }
                        Toast.makeText(UploadImage.this, getString(R.string.location_shared_message), Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                    .show();
        });

        // Load and process image from Intent
        Intent intent = getIntent();
        String photoUriString = intent.getStringExtra("photoUri");
        if (photoUriString != null) {
            photoUri = Uri.parse(photoUriString);
            processImage(photoUri);
        } else {
            finish();
        }

        // Restore state if available
        if (savedInstanceState != null) {
            String savedPhotoUriString = savedInstanceState.getString(KEY_PHOTO_URI);
            if (savedPhotoUriString != null) {
                photoUri = Uri.parse(savedPhotoUriString);
                processImage(photoUri);
            }
            currentBitmapPath = savedInstanceState.getString(KEY_CURRENT_BITMAP_PATH);
            if (currentBitmapPath != null) {
                currentBitmap = BitmapFactory.decodeFile(currentBitmapPath);
                if (currentBitmap != null) {
                    bigmap.setImageBitmap(currentBitmap);
                    bigmap.setVisibility(View.VISIBLE);
                    buttonUpload.setEnabled(true);
                }
            }
        }
    }

    private void sendLocationToChatroom(String groupName, String location) {
        Intent intent = new Intent(this, Chatroom.class);
        intent.putExtra("groupName", groupName);
        intent.putExtra("locationMessage", getString(R.string.location_share_prefix) + location);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        Log.d("UploadImage", "Sending location '" + location + "' to chatroom: " + groupName);
    }

    private void processImage(Uri photoUri) {
        if (photoUri == null) {
            Log.e("UploadImage", "photoUri is null");
            return;
        }

        Log.d("UploadImage", "處理圖片，URI: " + photoUri.toString());

        // 檢查 ErrorRecords 是否有該圖片的記錄
        String errorRecord = errorRecords.getString(photoUri.toString(), null);
        if (errorRecord != null) {
            try {
                JSONObject json = new JSONObject(errorRecord);
                String correctLocation = json.getString("correctLocation");
                Log.d("UploadImage", "Found error record for photoUri: " + photoUri + ", correctLocation: " + correctLocation);

                // 載入原始圖片到 bigmap
                new AsyncTask<Void, Void, Bitmap>() {
                    @Override
                    protected Bitmap doInBackground(Void... voids) {
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(photoUri);
                            if (inputStream == null) {
                                Log.e("UploadImage", "無法為 photoUri 打開 InputStream");
                                return null;
                            }

                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            inputStream.close();
                            if (bitmap == null) {
                                Log.e("UploadImage", "從 InputStream 解碼 Bitmap 失敗");
                                return null;
                            }

                            ExifInterface exif = null;
                            try {
                                if (photoUri.getPath() != null) {
                                    exif = new ExifInterface(getContentResolver().openInputStream(photoUri));
                                }
                            } catch (IOException e) {
                                Log.e("UploadImage", "讀取 EXIF 資訊失敗: " + e.getMessage());
                            }

                            if (exif != null) {
                                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                                Log.d("UploadImage", "EXIF 方向: " + orientation);
                                bitmap = rotateBitmapIfNeeded(bitmap, orientation);
                            }

                            return bitmap;
                        } catch (IOException e) {
                            Log.e("UploadImage", "處理圖片錯誤: " + e.getMessage());
                            return null;
                        }
                    }

                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        if (bitmap != null) {
                            currentBitmap = bitmap;
                            currentBitmapPath = saveBitmapToTempFile(currentBitmap);
                            bigmap.setImageBitmap(currentBitmap);
                            bigmap.setVisibility(View.VISIBLE);
                            buttonUpload.setEnabled(true);
                        } else {
                            Log.e("UploadImage", "Failed to load bitmap for bigmap");
                            //runOnUiThread(() -> Toast.makeText(UploadImage.this, "無法載入原始圖片", Toast.LENGTH_SHORT).show());
                        }

                        // 更新 UI
                        runOnUiThread(() -> {
                            currentLocationTextView.setText("Location: " + correctLocation);
                            buttonCorrectLocation.setEnabled(true);
                            buttonIncorrectLocation.setEnabled(true);
                            buttonUpload.setEnabled(true);
                            //Toast.makeText(UploadImage.this, "已使用先前記錄的正確位置: " + correctLocation, Toast.LENGTH_SHORT).show();
                        });

                        // 從資料庫查詢 correctLocation 的地圖圖片
                        PictureDatabaseHelper dbHelper = new PictureDatabaseHelper(UploadImage.this);
                        String imageFileName = dbHelper.getImageUriForLocation(correctLocation);
                        if (imageFileName != null) {
                            try {
                                File imageFile = new File(getFilesDir(), imageFileName);
                                Bitmap bitmapSmall = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                                if (bitmapSmall != null) {
                                    smallmap.setImageBitmap(bitmapSmall);
                                    Log.d("UploadImage", "Set smallmap image for correct location: " + imageFileName);
                                    // 更新 topMatches 以便後續使用，假設 URI 格式為 file://<internal storage path>
                                    String imageUriString = "file://" + imageFile.getAbsolutePath();
                                    topMatches.clear();
                                    topMatches.add(new MatchResult(imageUriString, correctLocation, 1));
                                } else {
                                    Log.e("UploadImage", "Failed to decode bitmap for smallmap: " + imageFileName);
                                    runOnUiThread(() -> Toast.makeText(UploadImage.this, getString(R.string.map_load_failed), Toast.LENGTH_SHORT).show());
                                }
                            } catch (Exception e) {
                                Log.e("UploadImage", "Failed to load smallmap image: " + imageFileName + ", Error: " + e.getMessage());
                                runOnUiThread(() -> Toast.makeText(UploadImage.this, getString(R.string.map_load_failed), Toast.LENGTH_SHORT).show());
                            }
                        } else {
                            Log.w("UploadImage", "No image found for location: " + correctLocation);
                            //runOnUiThread(() -> Toast.makeText(UploadImage.this, "未找到對應地圖圖片", Toast.LENGTH_SHORT).show());
                        }
                    }
                }.execute();
                return; // 直接返回，跳過 ORB 比對
            } catch (JSONException e) {
                Log.e("UploadImage", "Error parsing error record: " + e.getMessage());
            }
        }

        // 創建並顯示進度提示框
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.matching_in_progress));
        progressDialog.setCancelable(false);
        progressDialog.show();

        // 使用 AsyncTask 處理圖片
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... voids) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(photoUri);
                    if (inputStream == null) {
                        Log.e("UploadImage", "無法為 photoUri 打開 InputStream");
                        return null;
                    }

                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();
                    if (bitmap == null) {
                        Log.e("UploadImage", "從 InputStream 解碼 Bitmap 失敗");
                        return null;
                    }

                    ExifInterface exif = null;
                    try {
                        if (photoUri.getPath() != null) {
                            exif = new ExifInterface(getContentResolver().openInputStream(photoUri));
                        }
                    } catch (IOException e) {
                        Log.e("UploadImage", "讀取 EXIF 資訊失敗: " + e.getMessage());
                    }

                    if (exif != null) {
                        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                        Log.d("UploadImage", "EXIF 方向: " + orientation);
                        bitmap = rotateBitmapIfNeeded(bitmap, orientation);
                    }

                    return bitmap;
                } catch (IOException e) {
                    Log.e("UploadImage", "處理圖片錯誤: " + e.getMessage());
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    currentBitmap = bitmap;
                    currentBitmapPath = saveBitmapToTempFile(currentBitmap);
                    bigmap.setImageBitmap(currentBitmap);
                    bigmap.setVisibility(View.VISIBLE);
                    buttonUpload.setEnabled(true);

                    // Convert Bitmap to Mat for ORB processing in a separate thread
                    new Thread(() -> {
                        try {
                            Mat mat = new Mat();
                            Utils.bitmapToMat(currentBitmap, mat);

                            String priorityLocation = sharedPreferences.getString(KEY_LAST_LOCATION, null);
                            Log.d("UploadImage", "Passing priorityLocation to ORBActivity: " + (priorityLocation != null ? priorityLocation : "none"));

                            Intent intent = new Intent(UploadImage.this, ORBActivity.class);
                            intent.putExtra("imageUri", photoUri.toString());
                            intent.putExtra("priorityLocation", priorityLocation);
                            activityResultLauncher.launch(intent);
                        } finally {
                            runOnUiThread(() -> {
                                if (progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                            });
                        }
                    }).start();
                } else {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                }
            }
        }.execute();
    }

    private Bitmap rotateBitmapIfNeeded(Bitmap bitmap, int orientation) {
        if (bitmap == null || bitmap.isRecycled()) {
            Log.w("UploadImage", "Bitmap is null or recycled");
            return null;
        }

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                return bitmap;
        }

        try {
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotatedBitmap != bitmap) {
                bitmap.recycle();
            }
            return rotatedBitmap;
        } catch (OutOfMemoryError e) {
            Log.e("UploadImage", "OutOfMemoryError during bitmap rotation: " + e.getMessage());
            return bitmap;
        }
    }

    private String saveBitmapToTempFile(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            Log.w("UploadImage", "Bitmap 為空或已回收，無法保存到臨時文件");
            return null;
        }

        try {
            File tempDir = new File(getCacheDir(), "temp_bitmaps");
            if (!tempDir.exists()) tempDir.mkdirs();
            File tempFile = File.createTempFile("bitmap_", ".png", tempDir);
            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 80, out);
                out.flush();
            }
            return tempFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e("UploadImage", "保存 Bitmap 到臨時文件失敗: " + e.getMessage());
            return null;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (photoUri != null) {
            outState.putString(KEY_PHOTO_URI, photoUri.toString());
        }
        if (currentBitmapPath != null) {
            outState.putString(KEY_CURRENT_BITMAP_PATH, currentBitmapPath);
        }
    }
}