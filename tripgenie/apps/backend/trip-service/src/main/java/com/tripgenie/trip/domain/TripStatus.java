package com.tripgenie.trip.domain;

import java.util.EnumSet;
import java.util.Set;

public enum TripStatus {
    DRAFT,
    PLANNED,
    ACTIVE,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(TripStatus next) {
        if (this == next) {
            return true;
        }
        return allowedTransitions().contains(next);
    }

    private Set<TripStatus> allowedTransitions() {
        return switch (this) {
            case DRAFT -> EnumSet.of(PLANNED, CANCELLED);
            case PLANNED -> EnumSet.of(DRAFT, ACTIVE, CANCELLED);
            case ACTIVE -> EnumSet.of(COMPLETED, CANCELLED);
            case COMPLETED, CANCELLED -> EnumSet.noneOf(TripStatus.class);
        };
    }
}
