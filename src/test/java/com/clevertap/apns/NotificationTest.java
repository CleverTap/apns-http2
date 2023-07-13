package com.clevertap.apns;

import com.clevertap.apns.enums.InterruptionLevel;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NotificationTest {

    @Test
    public void testNotificationBuilder() {
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

}