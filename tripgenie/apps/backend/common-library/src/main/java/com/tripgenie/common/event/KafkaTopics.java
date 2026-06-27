package com.tripgenie.common.event;

public final class KafkaTopics {
    public static final String TRIP_CREATED = "trip-created";
    public static final String TRIP_UPDATED = "trip-updated";
    public static final String ITINERARY_GENERATED = "itinerary-generated";
    public static final String NOTIFICATIONS = "notifications";

    private KafkaTopics() {
    }
}
