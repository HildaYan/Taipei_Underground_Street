package com.example.cameraproject_2;

import android.os.Parcel;
import android.os.Parcelable;

public class MatchResult implements Parcelable {
    private String uri;
    private String location;
    private int matches;
    public MatchResult(String uri, String location, int matches) {
        this.uri = uri;
        this.location = location;
        this.matches = matches;
    }
    protected MatchResult(Parcel in) {
        uri = in.readString();
        location = in.readString();
        matches = in.readInt();
    }
    public static final Creator<MatchResult> CREATOR = new Creator<MatchResult>() {
        @Override
        public MatchResult createFromParcel(Parcel in) {
            return new MatchResult(in);
        }

        @Override
        public MatchResult[] newArray(int size) {
            return new MatchResult[size];
        }
    };
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uri);
        dest.writeString(location);
        dest.writeInt(matches);
    }
    @Override
    public int describeContents() {
        return 0;
    }

    public String getUri() {
        return uri;
    }

    public String getLocation() {
        return location;
    }
    public int getMatches() {
        return matches;
    }
}