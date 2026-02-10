package com.pavelfatin.sleeparchiver.model;

public enum WatchModel {
    PRO("Sleeptracker Pro", 2400),
    ELITE("Sleeptracker Elite", 19200),
    ELITE2("Sleeptracker Elite 2", 19200);

    private final String displayName;
    private final int baudRate;

    WatchModel(String displayName, int baudRate) {
        this.displayName = displayName;
        this.baudRate = baudRate;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getBaudRate() {
        return baudRate;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
