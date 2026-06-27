package com.tripgenie.trip.repository;

import com.tripgenie.trip.domain.AiItineraryGeneration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiItineraryGenerationRepository extends JpaRepository<AiItineraryGeneration, UUID> {
}
