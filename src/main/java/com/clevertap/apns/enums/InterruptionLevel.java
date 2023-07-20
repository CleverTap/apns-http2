package com.clevertap.apns.enums;

public enum InterruptionLevel {

    ACTIVE("active"),
    PASSIVE("passive"),
    TIME_SENSITIVE("time-sensitive"),
    CRITICAL("critical")
    ;


    private final String value;

    InterruptionLevel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
