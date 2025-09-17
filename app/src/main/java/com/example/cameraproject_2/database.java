package com.example.cameraproject_2;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class database extends AppCompatActivity {

    private static final String DATABASE_NAME = "picture.db";
    private PictureDatabaseHelper dbHelper;
    private static final String TABLE_NAME = "picture_data";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_IMAGE = "image";
    private static final String COLUMN_FILE_EXTENSION = "file_extension";
    private static final String COLUMN_DESCRIPTION = "description";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_database);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new PictureDatabaseHelper(this);
        copyDatabase();
        try {
            dbHelper.createDataBase();
            SQLiteDatabase db = dbHelper.getPictureDatabase();
            Toast.makeText(this, "Database opened successfully", Toast.LENGTH_SHORT).show();
            displayImagesFromDatabase(db);
            dbHelper.closeDatabase();
        } catch (Exception e) {
            Log.e("Database", "Error opening database: " + e.getMessage());
            Toast.makeText(this, "Error opening database", Toast.LENGTH_SHORT).show();
        }
        dbHelper.copyImages();
        File imageDir = new File(getApplicationContext().getFilesDir(), "images");
        if (!imageDir.exists()) {
            imageDir.mkdirs();
        }

    }
    private void copyDatabase() {
        try {
            InputStream input = getAssets().open(DATABASE_NAME);
            File dbFile = getDatabasePath(DATABASE_NAME);
            if (dbFile.exists()) {
                dbFile.delete();
            }
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }

            OutputStream output = new FileOutputStream(dbFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            output.flush();
            output.close();
            input.close();
            checkCopiedDatabase();
        } catch (IOException e) {
            Log.e("Database", "Error copying database: " + e.getMessage());
            Toast.makeText(this, "Error copying database", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkCopiedDatabase() {
        File dbFile = getDatabasePath(DATABASE_NAME);
        if (dbFile.exists()) {
            Log.d("Database", "Database copied successfully to: " + dbFile.getAbsolutePath());
        } else {
            Log.e("Database", "Database file not found at: " + dbFile.getAbsolutePath());
        }
    }

    private void displayImagesFromDatabase(SQLiteDatabase db) {
        Cursor cursor = null;
        try {
            ConstraintLayout imagesContainer = findViewById(R.id.imagesContainer);
            imagesContainer.removeAllViews();
            int lastViewId = View.NO_ID;

            String[] columns = {COLUMN_NAME, COLUMN_IMAGE, COLUMN_FILE_EXTENSION, COLUMN_DESCRIPTION};
            cursor = db.query(TABLE_NAME, columns, null, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String imageName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
                    String description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION));
                    String imageFileName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE));
                    String fileExtension = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FILE_EXTENSION));

                    String fullImageFileName = imageFileName + fileExtension;
                    String imagePath = getImagePathFromName(fullImageFileName);
                    Log.d("ImageDisplay", "Image path: " + imagePath);

                    ImageView imageView = createImageView(imagePath);
                    TextView textView = createTextView(description);

                    imagesContainer.addView(imageView);
                    imagesContainer.addView(textView);

                    applyConstraints(imagesContainer, imageView, textView, lastViewId);
                    lastViewId = textView.getId();

                } while (cursor.moveToNext());
            } else {
                Log.e("ImageDisplay", "Cursor is null or empty");
                Toast.makeText(this, "No images found in database", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("ImageDisplay", "Database Query Error: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private ImageView createImageView(String imagePath) {
        ImageView imageView = new ImageView(this);
        imageView.setId(View.generateViewId());
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ConstraintLayout.LayoutParams imageParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
        );
        imageView.setLayoutParams(imageParams);

        File imageFile = new File(imagePath);
        if (imageFile.exists()) {
            Glide.with(this).load(imageFile).into(imageView);
        } else {
            Log.e("ImageDisplay", "Image file not found: " + imagePath);
        }
        return imageView;
    }

    private TextView createTextView(String description) {
        TextView textView = new TextView(this);
        textView.setId(View.generateViewId());
        textView.setText(description);
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        ConstraintLayout.LayoutParams textParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
        );
        textView.setLayoutParams(textParams);

        return textView;
    }

    private void applyConstraints(ConstraintLayout container, ImageView imageView, TextView textView, int lastViewId) {
        ConstraintSet set = new ConstraintSet();
        set.clone(container);
        set.connect(imageView.getId(), ConstraintSet.TOP,
                (lastViewId == View.NO_ID) ? ConstraintSet.PARENT_ID : lastViewId,
                (lastViewId == View.NO_ID) ? ConstraintSet.TOP : ConstraintSet.BOTTOM, 0);
        set.connect(imageView.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, 0);
        set.connect(imageView.getId(), ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, 0);
        set.connect(textView.getId(), ConstraintSet.TOP, imageView.getId(), ConstraintSet.BOTTOM, 0);
        set.connect(textView.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, 0);
        set.connect(textView.getId(), ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, 0);
        set.applyTo(container);
    }

    private String getImagePathFromName(String imageName) {
        File imageDir = new File(getApplicationContext().getFilesDir(), "images");
        Log.d("ImageDisplay", "Image directory: " + imageDir.getAbsolutePath()); // 新增這一行
        return new File(imageDir, imageName).getAbsolutePath();
    }

}