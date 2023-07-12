package com.clevertap.apns.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    private static final Map<String, InterruptionLevel> interruptionLevelMap = Arrays.stream(InterruptionLevel.values())
            .collect(Collectors.toMap(InterruptionLevel::getValue, Function.identity()));


    public static InterruptionLevel get(String value) {
        return interruptionLevelMap.get(value);
    }
}
