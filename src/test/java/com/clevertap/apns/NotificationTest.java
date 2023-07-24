package com.clevertap.apns;

import com.clevertap.apns.enums.InterruptionLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    @Test
    void testNotificationBuilder() {
        Notification.Builder builder = new Notification.Builder("token");
        builder.relevanceScore(0.1);
        builder.interruptionLevel(InterruptionLevel.PASSIVE);
        builder.mutableContent(true);
        builder.alertBody("body");
        builder.alertTitle("title");
        builder.category("cat1");
        builder.priority(Notification.Priority.IMMEDIATE);
        Notification notification = builder.build();
        assertEquals("{\"aps\":{\"interruption-level\":\"passive\",\"relevance-score\":0.1,\"alert\":{\"body\":\"body\",\"title\":\"title\"},\"category\":\"cat1\",\"mutable-content\":1}}", notification.getPayload());
    }

    @Test
    void testNotificationBuilderWithResetOptions() {
        Notification.Builder builder = new Notification.Builder("token");
        builder.relevanceScore(0.1);
        builder.interruptionLevel(InterruptionLevel.PASSIVE);
        builder.mutableContent(true);
        builder.alertBody("body");
        builder.alertTitle("title");
        builder.category("cat1");
        builder.priority(Notification.Priority.IMMEDIATE);
        builder.resetRelevanceScore();
        builder.resetInterruptionLevel();
        Notification notification = builder.build();
        assertEquals("{\"aps\":{\"alert\":{\"body\":\"body\",\"title\":\"title\"},\"category\":\"cat1\",\"mutable-content\":1}}", notification.getPayload());
    }


    @ParameterizedTest
    @MethodSource("providerForRelevanceScore")
    void testRelevanceScore(String expected, double relevanceScore) {
        Notification.Builder builder = new Notification.Builder("token");
        builder.relevanceScore(relevanceScore);
        assertEquals(expected, builder.build().getPayload());
    }

    static Stream<Arguments> providerForRelevanceScore() {
        Stream.Builder<Arguments> builder = Stream.builder();
        builder.add(Arguments.of("{\"aps\":{\"relevance-score\":0.0,\"alert\":{}}}", 0.0));
        builder.add(Arguments.of("{\"aps\":{\"relevance-score\":1.0,\"alert\":{}}}", 1.0));
        builder.add(Arguments.of("{\"aps\":{\"relevance-score\":0.75,\"alert\":{}}}", 0.75));
        builder.add(Arguments.of("{\"aps\":{\"alert\":{}}}", 5.0));
        builder.add(Arguments.of("{\"aps\":{\"alert\":{}}}", -1.0));
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource("providerInterruptionLevel")
    void testInterruptionLevel(String expected, InterruptionLevel interruptionLevel) {
        Notification.Builder builder = new Notification.Builder("token");
        builder.interruptionLevel(interruptionLevel);
        assertEquals(expected, builder.build().getPayload());
    }

    static Stream<Arguments> providerInterruptionLevel() {
        Stream.Builder<Arguments> builder = Stream.builder();

        String payloadFormat = "{\"aps\":{\"interruption-level\":\"%s\",\"alert\":{}}}";
        for (InterruptionLevel interruptionLevel: InterruptionLevel.values()) {
            builder.add(Arguments.of(String.format(payloadFormat, interruptionLevel.getValue()), interruptionLevel));
        }
        builder.add(Arguments.of("{\"aps\":{\"alert\":{}}}", null));
        return builder.build();
    }

    @Test
    void resetRelevanceScore() {
        Notification.Builder builder = new Notification.Builder("token");
        builder.relevanceScore(0.5);
        Notification notification = builder.build();
        assertEquals("{\"aps\":{\"relevance-score\":0.5,\"alert\":{}}}", notification.getPayload());

        builder.resetRelevanceScore();
        Notification notification2 = builder.build();
        assertEquals("{\"aps\":{\"alert\":{}}}", notification2.getPayload());
    }

    @Test
    void resetInterruptionLevel() {
        Notification.Builder builder = new Notification.Builder("token");
        builder.interruptionLevel(InterruptionLevel.ACTIVE);
        Notification notification = builder.build();
        assertEquals("{\"aps\":{\"interruption-level\":\"active\",\"alert\":{}}}", notification.getPayload());

        builder.resetInterruptionLevel();
        Notification notification2 = builder.build();
        assertEquals("{\"aps\":{\"alert\":{}}}", notification2.getPayload());
    }

}