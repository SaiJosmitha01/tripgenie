package com.tripgenie.user.repository;

import com.tripgenie.user.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    @EntityGraph(attributePaths = {"roles", "preferences"})
    Optional<User> findWithRolesAndPreferencesById(UUID id);
}
