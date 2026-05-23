package com.tripgenie.user.service;

import com.tripgenie.common.dto.TravelPreferencesRequest;
import com.tripgenie.common.dto.TravelPreferencesResponse;
import com.tripgenie.common.dto.UpdateProfileRequest;
import com.tripgenie.common.dto.UserProfileResponse;
import com.tripgenie.common.exception.NotFoundException;
import com.tripgenie.user.domain.Role;
import com.tripgenie.user.domain.User;
import com.tripgenie.user.domain.UserPreference;
import com.tripgenie.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserProfileService {
    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        return toProfileResponse(findUser(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        if (request.firstName() != null) {
            user.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName().trim());
        }
        user.setPhoneNumber(trimToNull(request.phoneNumber()));
        user.setTimezone(trimToNull(request.timezone()));
        return toProfileResponse(userRepository.save(user));
    }

    @Transactional
    public UserProfileResponse updatePreferences(UUID userId, TravelPreferencesRequest request) {
        User user = findUser(userId);
        UserPreference preferences = user.getPreferences();
        if (preferences == null) {
            preferences = new UserPreference();
            user.setPreferences(preferences);
        }
        preferences.setHomeAirport(trimToNull(request.homeAirport()));
        preferences.setPreferredCurrency(trimToNull(request.preferredCurrency()));
        preferences.setDefaultTripLengthDays(request.defaultTripLengthDays());
        preferences.setDailyBudget(request.dailyBudget());
        preferences.setTravelStyles(toArray(request.travelStyles()));
        preferences.setDietaryRestrictions(toArray(request.dietaryRestrictions()));
        preferences.setAccessibilityNeeds(toArray(request.accessibilityNeeds()));
        return toProfileResponse(userRepository.save(user));
    }

    private User findUser(UUID userId) {
        return userRepository.findWithRolesAndPreferencesById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User was not found"));
    }

    private UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getTimezone(),
                user.getRoles().stream().map(Role::getName).map(role -> role.replace("ROLE_", "")).collect(Collectors.toUnmodifiableSet()),
                toPreferencesResponse(user.getPreferences()),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private TravelPreferencesResponse toPreferencesResponse(UserPreference preferences) {
        if (preferences == null) {
            return new TravelPreferencesResponse(null, null, null, null, Set.of(), Set.of(), Set.of());
        }
        return new TravelPreferencesResponse(
                preferences.getHomeAirport(),
                preferences.getPreferredCurrency(),
                preferences.getDefaultTripLengthDays(),
                preferences.getDailyBudget(),
                toSet(preferences.getTravelStyles()),
                toSet(preferences.getDietaryRestrictions()),
                toSet(preferences.getAccessibilityNeeds())
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String[] toArray(Set<String> values) {
        if (values == null) {
            return new String[0];
        }
        return values.stream().map(String::trim).filter(value -> !value.isBlank()).sorted().toArray(String[]::new);
    }

    private Set<String> toSet(String[] values) {
        if (values == null) {
            return Set.of();
        }
        return Arrays.stream(values).collect(Collectors.toUnmodifiableSet());
    }
}
