package com.example.finalapplication;

import android.os.Parcel;
import android.os.Parcelable;

public class PropertyEvent<T> implements Parcelable {
    private final int mPropertyId;
    private final int mStatus;
    private final long mTimestamp;
    private T mValue;
    public static final int STATUS_AVAILABLE = 0;
    public static final int STATUS_UNAVAILABLE = 1;

    public PropertyEvent(int propertyId, int status, long timestamp, T value) {
        mPropertyId = propertyId;
        mStatus = status;
        mTimestamp = timestamp;
        mValue = value;
    }

    protected PropertyEvent(Parcel in) {
        mPropertyId = in.readInt();
        mStatus = in.readInt();
        mTimestamp = in.readLong();
        String dataClassName = in.readString();
        Class<?> dataClass;
        try {
            dataClass = Class.forName(dataClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Class not found: " + dataClassName);
        }
        mValue = (T) in.readValue(dataClass.getClassLoader());
    }

    public void setValue(T value) {
        mValue = value;
    }

    public T getValue() {
        return mValue;
    }

    public int getPropertyId() {
        return mPropertyId;
    }

    public int getStatus() {
        return mStatus;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public static final Creator<PropertyEvent> CREATOR = new Creator<PropertyEvent>() {
        @Override
        public PropertyEvent createFromParcel(Parcel in) {
            return new PropertyEvent(in);
        }

        @Override
        public PropertyEvent[] newArray(int size) {
            return new PropertyEvent[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mPropertyId);
        dest.writeInt(mStatus);
        dest.writeLong(mTimestamp);
        Class<?> dataClass = mValue == null ? null : mValue.getClass();
        dest.writeString(dataClass == null ? null : dataClass.getName());
        dest.writeValue(mValue);
    }
}
