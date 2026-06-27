package com.tripgenie.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "roles")
public class Role {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    protected Role() {
    }

    public Role(String name) {
        this.name = name;
    }

    @PrePersist
    void prePersist() {
        id = id == null ? UUID.randomUUID() : id;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
