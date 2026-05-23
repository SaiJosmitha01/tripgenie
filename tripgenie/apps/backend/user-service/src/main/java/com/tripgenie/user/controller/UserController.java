package com.tripgenie.user.controller;

import com.tripgenie.common.api.ApiResponse;
import com.tripgenie.common.dto.TravelPreferencesRequest;
import com.tripgenie.common.dto.UpdateProfileRequest;
import com.tripgenie.common.dto.UserProfileResponse;
import com.tripgenie.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('USER')")
public class UserController {
    private final UserProfileService userProfileService;

    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's profile")
    ApiResponse<UserProfileResponse> me(JwtAuthenticationToken authentication) {
        return ApiResponse.success("Profile retrieved", userProfileService.getProfile(currentUserId(authentication)));
    }

    @PutMapping("/me")
    @Operation(summary = "Update the authenticated user's profile")
    ApiResponse<UserProfileResponse> updateMe(JwtAuthenticationToken authentication, @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success("Profile updated", userProfileService.updateProfile(currentUserId(authentication), request));
    }

    @PutMapping("/me/preferences")
    @Operation(summary = "Update the authenticated user's travel preferences")
    ApiResponse<UserProfileResponse> updatePreferences(JwtAuthenticationToken authentication, @Valid @RequestBody TravelPreferencesRequest request) {
        return ApiResponse.success("Preferences updated", userProfileService.updatePreferences(currentUserId(authentication), request));
    }

    private UUID currentUserId(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getName());
    }
}
