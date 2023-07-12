package com.clevertap.apns.enums;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class InterruptionLevelTest {

    @ParameterizedTest
    @MethodSource("providerForTest")
    public void test(InterruptionLevel expected, String key) {
        assertEquals(expected, InterruptionLevel.get(key));
    }

    public static Stream<Arguments> providerForTest() {
        Stream.Builder<Arguments> builder = Stream.builder();

        for (InterruptionLevel interruptionLevel: InterruptionLevel.values()) {
            builder.add(Arguments.of(interruptionLevel, interruptionLevel.getValue()));
        }
        builder.add(Arguments.of(null, "xxx"));
        builder.add(Arguments.of(null, null));

        return builder.build();
    }

}