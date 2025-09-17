package com.example.cameraproject_2.ui;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.cameraproject_2.model.AppDatabase;
import com.example.cameraproject_2.model.FareEntryDao;
import com.example.cameraproject_2.model.FareEntry;
import com.example.cameraproject_2.model.MetroApiResponse;
import com.example.cameraproject_2.model.MetroApiResult;
import com.example.cameraproject_2.network.TaipeiMetroApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FareQueryViewModel extends AndroidViewModel {
    private static final String TAG = "FareQueryVM";
    private static final String PREFS_NAME = "FarePrefs";
    private static final String KEY_LAST_UPDATE_TIMESTAMP = "lastUpdateTimestamp";
    private static final long CACHE_EXPIRY_DURATION_MS = 24 * 60 * 60 * 1000;
    private final MutableLiveData<List<String>> _stationList = new MutableLiveData<>();
    public final LiveData<List<String>> stationList = _stationList;
    private final MutableLiveData<String> _fullFareResult = new MutableLiveData<>();
    public final LiveData<String> fullFareResult = _fullFareResult;
    private final MutableLiveData<String> _concessionFareResult = new MutableLiveData<>();
    public final LiveData<String> concessionFareResult = _concessionFareResult;
    private final MutableLiveData<String> _taipeiChildFareResult = new MutableLiveData<>();
    public final LiveData<String> taipeiChildFareResult = _taipeiChildFareResult;
    private final MutableLiveData<String> _distanceResult = new MutableLiveData<>();
    public final LiveData<String> distanceResult = _distanceResult;
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public final LiveData<Boolean> isLoading = _isLoading;
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public final LiveData<String> errorMessage = _errorMessage;
    private List<FareEntry> allFareEntries = new ArrayList<>();
    private final TaipeiMetroApiService apiService;
    private final FareEntryDao fareEntryDao;
    private final SharedPreferences sharedPreferences;
    private final ExecutorService databaseExecutor;
    private static final int API_LIMIT_PER_REQUEST = 1000;
    private int currentOffset = 0;
    private boolean isLoadingAllPagesFromApi = false;
    public FareQueryViewModel(Application application) {
        super(application);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://data.taipei/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(TaipeiMetroApiService.class);
        AppDatabase database = AppDatabase.getDatabase(application);
        fareEntryDao = database.fareEntryDao();
        sharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        databaseExecutor = Executors.newSingleThreadExecutor();
        loadFareData();
    }
    private void loadFareData() {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        databaseExecutor.execute(() -> {
            long lastUpdateTime = sharedPreferences.getLong(KEY_LAST_UPDATE_TIMESTAMP, 0);
            boolean isCacheExpired = (System.currentTimeMillis() - lastUpdateTime) > CACHE_EXPIRY_DURATION_MS;
            List<FareEntry> cachedEntries = null;

            if (!isCacheExpired) {
                Log.d(TAG, "Cache not expired. Trying to load from database...");
                cachedEntries = fareEntryDao.getAllFareEntries();
            } else {
                Log.d(TAG, "Cache expired or first load.");
            }

            if (cachedEntries != null && !cachedEntries.isEmpty()) {
                Log.d(TAG, "Loaded " + cachedEntries.size() + " entries from database cache.");
                allFareEntries.clear();
                allFareEntries.addAll(cachedEntries);
                ContextCompat.getMainExecutor(getApplication()).execute(this::processLoadedData);
            } else {
                fetchAllFareDataPagesFromApi();
            }
        });
    }
    private void fetchAllFareDataPagesFromApi() {
        if (isLoadingAllPagesFromApi) {
            return;
        }
        allFareEntries.clear();
        currentOffset = 0;
        isLoadingAllPagesFromApi = true;
        ContextCompat.getMainExecutor(getApplication()).execute(this::fetchNextPageFromApi);
    }

    private void fetchNextPageFromApi() {
        Log.d(TAG, "Fetching page from API with offset: " + currentOffset + ", limit: " + API_LIMIT_PER_REQUEST);
        apiService.getMetroFares(API_LIMIT_PER_REQUEST, currentOffset).enqueue(new Callback<MetroApiResponse>() {
            @Override
            public void onResponse(Call<MetroApiResponse> call, Response<MetroApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    MetroApiResult result = response.body().getResult();
                    List<FareEntry> currentPageEntries = result.getFareEntries();
                    if (currentPageEntries != null && !currentPageEntries.isEmpty()) {
                        allFareEntries.addAll(currentPageEntries);
                        int totalCount = result.getCount();
                        if (allFareEntries.size() < totalCount && currentPageEntries.size() == API_LIMIT_PER_REQUEST) {
                            currentOffset += API_LIMIT_PER_REQUEST;
                            fetchNextPageFromApi();
                        } else {
                            saveFareDataToDatabase(new ArrayList<>(allFareEntries));
                            processLoadedData();
                        }
                    } else {
                        Log.d(TAG, "API: Current page has no entries or API returned empty list. Assuming all fetched.");
                        if (!allFareEntries.isEmpty()) {
                            saveFareDataToDatabase(new ArrayList<>(allFareEntries));
                        }
                        processLoadedData();
                    }
                } else {
                    isLoadingAllPagesFromApi = false;
                    String errorMsg = "無法載入部分票價資料 (錯誤碼：" + response.code() + ")";
                    try {
                        if (response.errorBody() != null) errorMsg += " " + response.errorBody().string();
                    } catch (IOException ignored) {}
                    _errorMessage.postValue(errorMsg);
                    if (!allFareEntries.isEmpty()) {
                        Log.w(TAG, "Processing potentially partial data due to API error.");
                        processLoadedData();
                    } else {
                        _isLoading.postValue(false);
                        _stationList.postValue(new ArrayList<>());
                    }
                }
            }

            @Override
            public void onFailure(Call<MetroApiResponse> call, Throwable t) {
                isLoadingAllPagesFromApi = false;
                Log.e(TAG, "Network Error during paged API fetch", t);
                _errorMessage.postValue("網路連線失敗，請稍後再試。");
                if (!allFareEntries.isEmpty()) {
                    Log.w(TAG, "Processing potentially partial data due to network error.");
                    processLoadedData();
                } else {
                    _isLoading.postValue(false);
                    _stationList.postValue(new ArrayList<>());
                }
            }
        });
    }

    private void saveFareDataToDatabase(final List<FareEntry> entriesToSave) {
        databaseExecutor.execute(() -> {
            try {
                fareEntryDao.deleteAll();
                fareEntryDao.insertAll(entriesToSave);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putLong(KEY_LAST_UPDATE_TIMESTAMP, System.currentTimeMillis());
                editor.apply();
                Log.d(TAG, "Saved " + entriesToSave.size() + " entries to database and updated timestamp.");
            } catch (Exception e) {
                Log.e(TAG, "Error saving data to database", e);
            }
        });
    }
    private void processLoadedData() {
        isLoadingAllPagesFromApi = false;
        _isLoading.setValue(false);
        if (allFareEntries.isEmpty()) {
            Log.w(TAG, "No fare entries to process.");
            if (_errorMessage.getValue() == null || _errorMessage.getValue().isEmpty()) {
                _errorMessage.setValue("未能載入任何票價資料。");
            }
            _stationList.setValue(new ArrayList<>());
            return;
        }

        Set<String> stationSet = new HashSet<>();
        for (FareEntry entry : allFareEntries) {
            if (entry.getFromStation() != null && !entry.getFromStation().isEmpty()) {
                stationSet.add(entry.getFromStation());
            }
            if (entry.getToStation() != null && !entry.getToStation().isEmpty()) {
                stationSet.add(entry.getToStation());
            }
        }
        List<String> sortedStations = new ArrayList<>(stationSet);
        Collections.sort(sortedStations);
        _stationList.setValue(sortedStations);
        Log.d(TAG, "Station list updated with " + sortedStations.size() + " unique stations from processed data.");
    }
    public void queryFare(String startStation, String endStation) {
        if (isLoadingAllPagesFromApi) {
            _errorMessage.setValue("票價資料仍在從網路載入中，請稍候...");
            return;
        }
        if (_isLoading.getValue() != null && _isLoading.getValue()) {
            _errorMessage.setValue("票價資料準備中，請稍候...");
            return;
        }
        _isLoading.setValue(true);
        _fullFareResult.setValue(null);
        _concessionFareResult.setValue(null);
        _taipeiChildFareResult.setValue(null);
        _distanceResult.setValue(null);
        _errorMessage.setValue(null);
        if (startStation == null || startStation.isEmpty() || endStation == null || endStation.isEmpty()) {
            _errorMessage.setValue("請選擇起點和終點站。");
            _isLoading.setValue(false);
            return;
        }
        FareEntry foundEntry = null;
        if (!allFareEntries.isEmpty()) {
            for (FareEntry entry : allFareEntries) {
                if (startStation.equals(entry.getFromStation()) && endStation.equals(entry.getToStation())) {
                    foundEntry = entry;
                    break;
                }
            }
            if (foundEntry == null) {
                for (FareEntry entry : allFareEntries) {
                    if (endStation.equals(entry.getFromStation()) && startStation.equals(entry.getToStation())) {
                        foundEntry = entry;
                        break;
                    }
                }
            }
        }
        if (foundEntry != null) {
            _fullFareResult.setValue("全票票價：NT$ " + foundEntry.getFullFare());
            _concessionFareResult.setValue("敬老愛心/兒童(新北)：NT$ " + foundEntry.getConcessionFare());
            _taipeiChildFareResult.setValue("兒童(北市)：NT$ " + foundEntry.getTaipeiChildFare());
            String distanceStr = foundEntry.getDistance();
            try {
                if (distanceStr != null && !distanceStr.trim().isEmpty()) {
                    double distValue = Double.parseDouble(distanceStr);
                    _distanceResult.setValue(String.format(Locale.US, "距離：%.1f km", distValue));
                } else {
                    _distanceResult.setValue("距離：無資料");
                }
            } catch (NumberFormatException e) {
                _distanceResult.setValue("距離：" + (distanceStr != null ? distanceStr : "無資料"));
            }
        } else {
            _errorMessage.setValue("找不到從 " + startStation + " 到 " + endStation + " 的票價資訊。");
        }
        _isLoading.setValue(false);
    }
    @Override
    protected void onCleared() {
        super.onCleared();
        databaseExecutor.shutdown();
    }
}

