package com.example.cameraproject_2;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PictureDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "PictureDatabaseHelper";
    public static final String PICTURE_DB_NAME = "picture.db";
    private static final int DATABASE_VERSION = 5;
    private static final String DB_PATH = "/data/data/com.example.cameraproject_2/databases";
    private final Context context;
    private SQLiteDatabase pictureDatabase;
    public PictureDatabaseHelper(Context context) {
        super(context, PICTURE_DB_NAME, null, DATABASE_VERSION);
        this.context = context;
        try {
            createDataBase();
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize database: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // No need to create tables, as picture.db is pre-packaged
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE picture_data ADD COLUMN new_column TEXT");
        }
    }

    public void createDataBase() throws IOException {
        boolean dbExists = checkDataBase();
        Log.d(TAG, "Checking if database " + PICTURE_DB_NAME + " exists: " + dbExists);

        boolean isValid = false;
        int currentDbVersion = DATABASE_VERSION;
        if (dbExists) {
            SQLiteDatabase db = null;
            try {
                db = SQLiteDatabase.openDatabase(DB_PATH + "/" + PICTURE_DB_NAME, null, SQLiteDatabase.OPEN_READONLY);
                Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='picture_data'", null);
                isValid = cursor.moveToFirst();
                cursor.close();

                Cursor versionCursor = db.rawQuery("PRAGMA user_version;", null);
                if (versionCursor.moveToFirst()) {
                    currentDbVersion = versionCursor.getInt(0);
                }
                versionCursor.close();
                Log.d(TAG, "Current database version: " + currentDbVersion + ", Expected version: " + DATABASE_VERSION);
            } catch (SQLiteException e) {
                Log.e(TAG, "Error checking database validity: " + e.getMessage());
            } finally {
                if (db != null && db.isOpen()) {
                    db.close();
                }
            }
        }

        if (!dbExists || !isValid || currentDbVersion != DATABASE_VERSION) {
            try {
                if (dbExists) {
                    File dbFile = new File(DB_PATH + "/" + PICTURE_DB_NAME);
                    dbFile.delete();
                    Log.d(TAG, "Deleted old database " + PICTURE_DB_NAME + " due to version mismatch or invalidity");
                }
                copyDataBase();
                copyImagesFromDatabase();
                verifyDatabase();
                SQLiteDatabase db = getWritableDatabase();
                db.execSQL("PRAGMA user_version = " + DATABASE_VERSION);
                db.close();
                Log.d(TAG, "Database " + PICTURE_DB_NAME + " created and updated to version " + DATABASE_VERSION);
            } catch (IOException e) {
                Log.e(TAG, "Error copying database " + PICTURE_DB_NAME + ": " + e.getMessage());
                throw new IOException("Error copying database: " + e.getMessage());
            }
        } else {
            Log.d(TAG, "Database " + PICTURE_DB_NAME + " already exists, is valid, and matches version " + currentDbVersion);
            verifyDatabase();
        }
    }

    public void copyImagesFromDatabase() {
        File dir = new File(context.getFilesDir(), "images");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        SQLiteDatabase db = getPictureDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query("picture_data",
                    new String[]{"image", "file_extension"},
                    null, null, null, null, null);
            while (cursor.moveToNext()) {
                int imageId = cursor.getInt(cursor.getColumnIndexOrThrow("image"));
                String extension = cursor.getString(cursor.getColumnIndexOrThrow("file_extension"));
                String fileName = "images/" + imageId + extension;
                File targetFile = new File(dir, imageId + extension);

                if (!targetFile.exists()) {
                    InputStream input = context.getAssets().open("images/" + imageId + extension);
                    OutputStream output = new FileOutputStream(targetFile);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = input.read(buffer)) > 0) {
                        output.write(buffer, 0, length);
                    }
                    output.flush();
                    output.close();
                    input.close();
                    Log.d(TAG, "Copied image: " + fileName);
                } else {
                    Log.d(TAG, "Image already exists: " + fileName);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error copying images from database: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private boolean checkDataBase() {
        File dbFile = new File(DB_PATH + "/" + PICTURE_DB_NAME);
        return dbFile.exists();
    }

    private void copyDataBase() throws IOException {
        InputStream input = context.getAssets().open(PICTURE_DB_NAME);
        File dir = new File(DB_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
            Log.d(TAG, "Created directory: " + DB_PATH);
        }
        File outputFile = new File(DB_PATH + "/" + PICTURE_DB_NAME);
        OutputStream output = new FileOutputStream(outputFile);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = input.read(buffer)) > 0) {
            output.write(buffer, 0, length);
        }

        output.flush();
        output.close();
        input.close();
    }

    private void verifyDatabase() {
        SQLiteDatabase db = null;
        try {
            String path = DB_PATH + "/" + PICTURE_DB_NAME;
            db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);
            Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='picture_data'", null);
            boolean tableExists = cursor.moveToFirst();
            cursor.close();
            if (!tableExists) {
                Log.e(TAG, "Table picture_data does not exist in " + PICTURE_DB_NAME);
            } else {
                Log.d(TAG, "Table picture_data verified in " + PICTURE_DB_NAME);
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Error verifying database " + PICTURE_DB_NAME + ": " + e.getMessage());
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    public SQLiteDatabase getPictureDatabase() {
        if (pictureDatabase == null || !pictureDatabase.isOpen()) {
            pictureDatabase = this.getWritableDatabase();
            Log.d(TAG, "getPictureDatabase opened at: " + pictureDatabase.getPath());
        }
        return pictureDatabase;
    }

    public void closeDatabase() {
        if (pictureDatabase != null && pictureDatabase.isOpen()) {
            pictureDatabase.close();
        }
    }

    public void copyImages() {
        File dir = new File(context.getFilesDir(), "images");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            String[] files = context.getAssets().list("images");
            if (files != null) {
                for (String file : files) {
                    File targetFile = new File(dir, file);
                    if (targetFile.exists()) {
                        continue;
                    }
                    InputStream input = context.getAssets().open("images/" + file);
                    OutputStream output = new FileOutputStream(targetFile);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = input.read(buffer)) > 0) {
                        output.write(buffer, 0, length);
                    }
                    output.flush();
                    output.close();
                    input.close();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error copying images: " + e.getMessage());
        }
    }
    public String getImageUriForLocation(String locationName) {
        SQLiteDatabase db = getPictureDatabase();
        Cursor cursor = null;
        String fileName = null;
        try {
            Log.d(TAG, "Querying database for locationName: " + locationName);
            cursor = db.query("picture_data",
                    new String[]{"image", "file_extension"},
                    "location_data = ?",
                    new String[]{locationName},
                    null, null, null);
            if (cursor.moveToFirst()) {
                int imageId = cursor.getInt(cursor.getColumnIndexOrThrow("image"));
                String extension = cursor.getString(cursor.getColumnIndexOrThrow("file_extension"));
                fileName = "images/" + imageId + extension;
                Log.d(TAG, "Found filename: " + fileName + " for locationName: " + locationName);
            } else {
                Log.w(TAG, "No record found for locationName: " + locationName);
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Error querying image file name for location: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return fileName;
    }
}