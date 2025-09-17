package com.example.cameraproject_2.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;
public class MetroApiResult {
    @SerializedName("results")
    private List<FareEntry> fareEntries;
    @SerializedName("limit")
    private int limit;
    @SerializedName("offset")
    private int offset;
    @SerializedName("count")
    private int count;
    public List<FareEntry> getFareEntries() {
        return fareEntries;
    }
    public int getCount() { // <<<<< 添加 getter
        return count;
    }
}