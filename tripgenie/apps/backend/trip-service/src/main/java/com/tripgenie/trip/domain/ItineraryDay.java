package com.tripgenie.trip.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "itinerary_days")
public class ItineraryDay extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "day_number", nullable = false)
    private int dayNumber;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 160)
    private String title;

    @Column(columnDefinition = "text")
    private String notes;

    @OneToMany(mappedBy = "itineraryDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private final List<ItineraryItem> items = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<ItineraryItem> getItems() {
        return items;
    }

    public void addItem(ItineraryItem item) {
        item.setItineraryDay(this);
        items.add(item);
    }
}
