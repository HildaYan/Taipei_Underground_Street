package com.example.cameraproject_2.model;
import com.google.gson.annotations.SerializedName;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName = "fare_entries")
public class FareEntry {
    @PrimaryKey
    @SerializedName("_id")
    private int id;
    @Embedded
    @SerializedName("_importdate")
    private ImportDateInfo importDate;
    @ColumnInfo(name = "from_station")
    @SerializedName("起站")
    private String fromStation;
    @ColumnInfo(name = "to_station")
    @SerializedName("訖站")
    private String toStation;
    @ColumnInfo(name = "full_fare")
    @SerializedName("全票票價")
    private String fullFare;
    @ColumnInfo(name = "concession_fare")
    @SerializedName("敬老卡愛心卡愛心陪伴卡及新北市兒童優惠票價")
    private String concessionFare;
    @ColumnInfo(name = "taipei_child_fare")
    @SerializedName("臺北市兒童優惠票價")
    private String taipeiChildFare;
    @ColumnInfo(name = "distance")
    @SerializedName("距離")
    private String distance;
    public FareEntry() {}
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public ImportDateInfo getImportDate() { return importDate; }
    public void setImportDate(ImportDateInfo importDate) { this.importDate = importDate; }
    public String getFromStation() { return fromStation; }
    public void setFromStation(String fromStation) { this.fromStation = fromStation; }
    public String getToStation() { return toStation; }
    public void setToStation(String toStation) { this.toStation = toStation; }
    public String getFullFare() { return fullFare; }
    public void setFullFare(String fullFare) { this.fullFare = fullFare; }
    public String getConcessionFare() { return concessionFare; }
    public void setConcessionFare(String concessionFare) { this.concessionFare = concessionFare; }
    public String getTaipeiChildFare() { return taipeiChildFare; }
    public void setTaipeiChildFare(String taipeiChildFare) { this.taipeiChildFare = taipeiChildFare; }
    public String getDistance() { return distance; }
    public void setDistance(String distance) { this.distance = distance; }
}