package com.example.cameraproject_2;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class WhereLocation extends AppCompatActivity {

    private RecyclerView container;
    private TextView bottomText;
    private static final String TAG = "WhereLocation";
    private SharedPreferences sharedPreferences;
    private String originalPhotoUri; // 儲存原始圖片的 URI
    private ArrayList<MatchResult> topMatches;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_where_location);
        sharedPreferences = getSharedPreferences("ErrorRecords", MODE_PRIVATE);
        container = findViewById(R.id.location_container);
        bottomText = findViewById(R.id.bottom_text);

        container.setLayoutManager(new GridLayoutManager(this, 2));
        LocationAdapter adapter = new LocationAdapter();
        container.setAdapter(adapter);
        container.setHasFixedSize(true);
        container.requestLayout();
        Intent intent = getIntent();
        topMatches = intent.getParcelableArrayListExtra("topMatches");
        originalPhotoUri = intent.getStringExtra("photoUri");
        Log.d(TAG, "topMatches size: " + (topMatches != null ? topMatches.size() : "null"));
        Log.d(TAG, "Original photo URI: " + originalPhotoUri);

        if (topMatches == null || topMatches.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_match_result), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        bottomText.setText(getString(R.string.match_first) + topMatches.size() + getString(R.string.match_second));
        adapter.setData(topMatches);
        findViewById(R.id.backArrow).setOnClickListener(v -> {
            onBackPressed();
        });
    }
    private String getImagePathFromUri(String uriString) {
        String fileName = uriString; // 預設為檔案名稱
        if (uriString.startsWith("file://")) {
            fileName = new File(Uri.parse(uriString).getPath()).getName();
        } else if (uriString.contains("/")) {
            fileName = new File(uriString).getName();
        }
        File imagesDir = new File(getFilesDir(), "images");
        String imagePath = new File(imagesDir, fileName).getAbsolutePath();
        Log.d(TAG, "Image path: " + imagePath);
        return imagePath;
    }
    private void recordError(String originalPhotoUri, String wrongLocation, String correctLocation) {
        try {
            JSONObject errorRecord = new JSONObject();
            errorRecord.put("wrongLocation", wrongLocation);
            errorRecord.put("correctLocation", correctLocation);

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(originalPhotoUri, errorRecord.toString());
            editor.apply();
            Log.d(TAG, "Error recorded: photoUri=" + originalPhotoUri + ", wrongLocation=" + wrongLocation + ", correctLocation=" + correctLocation);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to record error: " + e.getMessage());
        }
    }
    private void showLocationDialog(String location, String uriString) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.confirm_location_title));
        builder.setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("selectedLocation", location);
            resultIntent.putExtra("selectedImageUri", uriString);
            setResult(RESULT_OK, resultIntent);
            if (!topMatches.isEmpty() && !topMatches.get(0).getLocation().equals(location)) {
                String wrongLocation = topMatches.get(0).getLocation();
                recordError(originalPhotoUri, wrongLocation, location);
            }
            finish();
        });
        builder.setNegativeButton(getString(R.string.button_see_again), (dialog, which) -> {
            dialog.dismiss();
        });
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.ViewHolder> {
        private ArrayList<MatchResult> data = new ArrayList<>();
        public void setData(ArrayList<MatchResult> newData) {
            data.clear();
            if (newData != null) {
                data.addAll(newData);
                Log.d(TAG, "Adapter data size: " + data.size());
            }
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_location_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Log.d(TAG, "Binding item at position: " + position);
            MatchResult match = data.get(position);
            String uriString = match.getUri();
            String location = match.getLocation();
            String imagePath = getImagePathFromUri(uriString);
            File imageFile = new File(imagePath);
            Bitmap bitmap = null;
            if (imageFile.exists()) {
                bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                Log.d(TAG, "Image loaded from: " + imagePath);
            }
            if (bitmap != null) {
                holder.imageView.setImageBitmap(bitmap);
            } else {
                Log.e(TAG, "Failed to load image from path: " + imagePath);
                holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
            holder.locationText.setText(location);
            holder.locationText.setVisibility(View.GONE);
            holder.cardView.setOnClickListener(v -> toggleLocationInfo(holder.cardView, location));
            GestureDetector gestureDetector = new GestureDetector(WhereLocation.this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    showLocationDialog(location, uriString);
                    return true;
                }
            });
            holder.cardView.setOnTouchListener((v, event) -> {
                gestureDetector.onTouchEvent(event);
                return false;
            });
        }
        @Override
        public int getItemCount() {
            return data.size();
        }
        public class ViewHolder extends RecyclerView.ViewHolder {
            CardView cardView;
            ImageView imageView;
            TextView locationText;
            public ViewHolder(View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.card_view);
                imageView = itemView.findViewById(R.id.card_image);
                locationText = itemView.findViewById(R.id.card_location);
            }
        }
    }
    private void toggleLocationInfo(CardView cardView, String location) {
        TextView locationText = cardView.findViewById(R.id.card_location);
        if (locationText.getVisibility() == View.GONE) {
            locationText.setVisibility(View.VISIBLE);
            locationText.setText(location);
        } else {
            locationText.setVisibility(View.GONE);
        }
    }
}