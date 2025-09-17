package com.example.cameraproject_2.model;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface FareEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<FareEntry> fareEntries);
    @Query("SELECT * FROM fare_entries")
    List<FareEntry> getAllFareEntries();
    @Query("DELETE FROM fare_entries")
    void deleteAll();
}