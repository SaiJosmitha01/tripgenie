package com.tripgenie.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_preferences")
public class UserPreference {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "home_airport", length = 50)
    private String homeAirport;

    @Column(name = "preferred_currency", length = 50)
    private String preferredCurrency;

    @Column(name = "default_trip_length_days")
    private Integer defaultTripLengthDays;

    @Column(name = "daily_budget", precision = 12, scale = 2)
    private BigDecimal dailyBudget;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "travel_styles", columnDefinition = "text[]")
    private String[] travelStyles = new String[0];

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "dietary_restrictions", columnDefinition = "text[]")
    private String[] dietaryRestrictions = new String[0];

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "accessibility_needs", columnDefinition = "text[]")
    private String[] accessibilityNeeds = new String[0];

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getHomeAirport() {
        return homeAirport;
    }

    public void setHomeAirport(String homeAirport) {
        this.homeAirport = homeAirport;
    }

    public String getPreferredCurrency() {
        return preferredCurrency;
    }

    public void setPreferredCurrency(String preferredCurrency) {
        this.preferredCurrency = preferredCurrency;
    }

    public Integer getDefaultTripLengthDays() {
        return defaultTripLengthDays;
    }

    public void setDefaultTripLengthDays(Integer defaultTripLengthDays) {
        this.defaultTripLengthDays = defaultTripLengthDays;
    }

    public BigDecimal getDailyBudget() {
        return dailyBudget;
    }

    public void setDailyBudget(BigDecimal dailyBudget) {
        this.dailyBudget = dailyBudget;
    }

    public String[] getTravelStyles() {
        return travelStyles;
    }

    public void setTravelStyles(String[] travelStyles) {
        this.travelStyles = travelStyles == null ? new String[0] : travelStyles;
    }

    public String[] getDietaryRestrictions() {
        return dietaryRestrictions;
    }

    public void setDietaryRestrictions(String[] dietaryRestrictions) {
        this.dietaryRestrictions = dietaryRestrictions == null ? new String[0] : dietaryRestrictions;
    }

    public String[] getAccessibilityNeeds() {
        return accessibilityNeeds;
    }

    public void setAccessibilityNeeds(String[] accessibilityNeeds) {
        this.accessibilityNeeds = accessibilityNeeds == null ? new String[0] : accessibilityNeeds;
    }
}
