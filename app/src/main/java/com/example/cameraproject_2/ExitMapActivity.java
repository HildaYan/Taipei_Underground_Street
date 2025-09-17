package com.example.cameraproject_2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class ExitMapActivity extends AppCompatActivity {

    private static final String TAG = "ExitMapActivity";
    private WebView webView;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        setContentView(R.layout.activity_exit_map);

        webView = findViewById(R.id.map_webview);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setAllowFileAccess(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webSettings.setAllowFileAccessFromFileURLs(true);
            webSettings.setAllowUniversalAccessFromFileURLs(true);
        }
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "Page finished loading: " + url);
            }

            // 如果你需要攔截請求或處理錯誤，可以在這裡添加更多方法
            // 例如 onReceivedError
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                Log.d(TAG, "WebView requesting geolocation permission for origin: " + origin);
                if (ContextCompat.checkSelfPermission(ExitMapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(ExitMapActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "App has location permission. Granting to WebView.");
                    callback.invoke(origin, true, false);
                } else {
                    Log.d(TAG, "App does NOT have location permission. Requesting from user.");
                    ActivityCompat.requestPermissions(ExitMapActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                            LOCATION_PERMISSION_REQUEST_CODE);
                    callback.invoke(origin, true, false);
                }
            }

            // 如果你的 HTML 使用了 alert(), confirm(), prompt()，你可能需要覆寫這些方法
            // 例如: @Override public boolean onJsAlert(WebView view, String url, String message, JsResult result) { ... }
        });
        webView.loadUrl("file:///android_asset/leaflet/exit_map.html");
        checkAndRequestLocationPermission();
    }


    private void checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED || (grantResults.length > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED))) {
                Toast.makeText(this, "位置權限已獲取", Toast.LENGTH_SHORT).show();
                if (webView != null) {
                    webView.evaluateJavascript("javascript:if(typeof goToCurrentLocation === 'function'){goToCurrentLocation(true);} else { console.error('goToCurrentLocation function not found'); }", null);
                }
            } else {
                Log.w(TAG, "Android Location permission denied by user.");
                Toast.makeText(this, "位置權限被拒絕，定位功能可能無法使用。", Toast.LENGTH_LONG).show();
            }
        }
    }
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
