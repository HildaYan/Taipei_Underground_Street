package com.example.cameraproject_2;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "picture.db";
    private static final int DATABASE_VERSION = 1;
    private final Context myContext;
    private String dbPath;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        myContext = context;
        dbPath = context.getDatabasePath(DATABASE_NAME).getAbsolutePath();
    }

    // 建立資料庫
    public void createDataBase() throws IOException {
        boolean dbExist = checkDataBase();
        if (!dbExist) {
            this.getReadableDatabase();
            try {
                copyDataBase();
            } catch (IOException e) {
                throw new Error("Error copying database");
            }
        }
    }

    // 檢查資料庫是否存在
    private boolean checkDataBase() {
        File dbFile = new File(dbPath);
        return dbFile.exists();
    }

    // 複製資料庫
    public void copyDataBase() throws IOException {
        InputStream input = myContext.getAssets().open(DATABASE_NAME);
        String outFileName = myContext.getDatabasePath(DATABASE_NAME).getPath();
        File databaseFile = new File(outFileName);
        databaseFile.getParentFile().mkdirs();
        OutputStream output = new FileOutputStream(outFileName);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = input.read(buffer)) > 0) {
            output.write(buffer, 0, length);
        }
        output.flush();
        output.close();
        input.close();
    }

    // 複製圖片檔案
    public void copyImages() {
        File imagesDir = new File(myContext.getFilesDir(), "images");
        if (!imagesDir.exists()) {
            imagesDir.mkdirs();
        }

        try {
            String[] imageFiles = myContext.getAssets().list("images");
            if (imageFiles != null) {
                for (String imageFile : imageFiles) {
                    File targetFile = new File(imagesDir, imageFile);
                    if (!targetFile.exists()) {
                        InputStream in = null;
                        OutputStream out = null;
                        try {
                            in = myContext.getAssets().open("images/" + imageFile);
                            out = new FileOutputStream(targetFile);
                            byte[] buffer = new byte[1024];
                            int read;
                            while ((read = in.read(buffer)) != -1) {
                                out.write(buffer, 0, read);
                            }
                            out.flush();
                        } catch (IOException e) {
                            Log.e("DatabaseHelper", "Error copying image: " + imageFile, e);
                        } finally {
                            if (in != null) {
                                try {
                                    in.close();
                                } catch (IOException e) {
                                    // Ignore
                                }
                            }
                            if (out != null) {
                                try {
                                    out.close();
                                } catch (IOException e) {
                                    // Ignore
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.e("DatabaseHelper", "Error listing assets", e);
        }
    }

    public SQLiteDatabase openDataBase() {
        return SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 當使用預構建的資料庫時，不需要在這裡建立表
        Log.d("DatabaseHelper", "onCreate() called, but should not be when using pre-built DB");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("DatabaseHelper", "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
        // 移除 db.execSQL("DROP TABLE IF EXISTS picture_data");
    }

    //  打開資料庫內容
    public SQLiteDatabase openDatabase() {
        return getReadableDatabase();
    }

    public void closeDatabase() {
        close();
    }
}

