package com.tripgenie.trip.audit.domain;

public enum AuditAction {
    USER_REGISTERED,
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    TRIP_CREATED,
    TRIP_UPDATED,
    TRIP_DELETED,
    ITINERARY_GENERATED,
    LOCATIONS_ENRICHED,
    NOTIFICATION_PROCESSED
}
