package com.tripgenie.user.service;

import com.tripgenie.common.dto.TravelPreferencesRequest;
import com.tripgenie.common.dto.UpdateProfileRequest;
import com.tripgenie.common.dto.UserProfileResponse;
import com.tripgenie.common.exception.NotFoundException;
import com.tripgenie.user.domain.Role;
import com.tripgenie.user.domain.User;
import com.tripgenie.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {
    @Mock
    private UserRepository userRepository;

    private UserProfileService userProfileService;
    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileService(userRepository);
        userId = UUID.randomUUID();
        user = new User();
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(user, "email", "jane@example.com");
        ReflectionTestUtils.setField(user, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
        ReflectionTestUtils.setField(user, "updatedAt", Instant.parse("2026-01-01T00:00:00Z"));
        user.setFirstName("Jane");
        user.setLastName("Doe");
        Role role = new Role();
        ReflectionTestUtils.setField(role, "name", "ROLE_USER");
        user.getRoles().add(role);
    }

    @Test
    void getProfileReturnsCurrentUser() {
        when(userRepository.findWithRolesAndPreferencesById(userId)).thenReturn(Optional.of(user));

        UserProfileResponse profile = userProfileService.getProfile(userId);

        assertThat(profile.id()).isEqualTo(userId);
        assertThat(profile.email()).isEqualTo("jane@example.com");
        assertThat(profile.roles()).containsExactly("USER");
        assertThat(profile.preferences().travelStyles()).isEmpty();
    }

    @Test
    void updateProfileChangesEditableFields() {
        when(userRepository.findWithRolesAndPreferencesById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse profile = userProfileService.updateProfile(userId, new UpdateProfileRequest("Janet", "Roe", "555-0100", "America/New_York"));

        assertThat(profile.firstName()).isEqualTo("Janet");
        assertThat(profile.lastName()).isEqualTo("Roe");
        assertThat(profile.phoneNumber()).isEqualTo("555-0100");
        assertThat(profile.timezone()).isEqualTo("America/New_York");
    }

    @Test
    void updatePreferencesCreatesPreferencesWhenMissing() {
        when(userRepository.findWithRolesAndPreferencesById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TravelPreferencesRequest request = new TravelPreferencesRequest(
                "JFK",
                "USD",
                7,
                new BigDecimal("250.00"),
                Set.of("culture", "food"),
                Set.of("vegetarian"),
                Set.of("wheelchair")
        );

        UserProfileResponse profile = userProfileService.updatePreferences(userId, request);

        assertThat(profile.preferences().homeAirport()).isEqualTo("JFK");
        assertThat(profile.preferences().preferredCurrency()).isEqualTo("USD");
        assertThat(profile.preferences().defaultTripLengthDays()).isEqualTo(7);
        assertThat(profile.preferences().dailyBudget()).isEqualByComparingTo("250.00");
        assertThat(profile.preferences().travelStyles()).containsExactlyInAnyOrder("culture", "food");
        assertThat(profile.preferences().dietaryRestrictions()).containsExactly("vegetarian");
        assertThat(profile.preferences().accessibilityNeeds()).containsExactly("wheelchair");
    }

    @Test
    void updateProfileRejectsMissingUser() {
        when(userRepository.findWithRolesAndPreferencesById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.updateProfile(userId, new UpdateProfileRequest("Janet", null, null, null)))
                .isInstanceOf(NotFoundException.class);
    }
}
