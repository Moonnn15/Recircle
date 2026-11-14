package com.recircle.controller;

import com.recircle.dto.ApiDtos.*;
import com.recircle.security.AuthContext;
import com.recircle.security.AuthPrincipal;
import com.recircle.service.AdminAnalyticsService;
import com.recircle.service.AuthService;
import com.recircle.service.ReviewDisputeService;
import com.recircle.service.TrustScoreService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AuthUserController {

    private final AuthService authService;
    private final TrustScoreService trustScoreService;
    private final ReviewDisputeService reviewDisputeService;
    private final AdminAnalyticsService adminAnalyticsService;

    public AuthUserController(
        AuthService authService,
        TrustScoreService trustScoreService,
        ReviewDisputeService reviewDisputeService,
        AdminAnalyticsService adminAnalyticsService
    ) {
        this.authService = authService;
        this.trustScoreService = trustScoreService;
        this.reviewDisputeService = reviewDisputeService;
        this.adminAnalyticsService = adminAnalyticsService;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Map<String, String>> logout() {
        AuthPrincipal p = AuthContext.requireAuth();
        authService.logout(p.getUserId());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }

    @PostMapping("/auth/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestBody VerifyEmailRequest req) {
        return ResponseEntity.ok(authService.verifyEmail(req));
    }

    @PostMapping("/auth/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        return ResponseEntity.ok(authService.forgotPassword(req));
    }

    @PostMapping("/auth/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody ResetPasswordRequest req) {
        return ResponseEntity.ok(authService.resetPassword(req));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(authService.refresh(req));
    }

    @GetMapping("/users/me")
    public ResponseEntity<UserSummaryDto> getMyProfile() {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(authService.getUserProfile(p.getUserId()));
    }

    @PutMapping("/users/me")
    public ResponseEntity<UserSummaryDto> updateMyProfile(@RequestBody UpdateProfileRequest req) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(authService.updateProfile(p.getUserId(), req));
    }

    @GetMapping("/users/me/dashboard")
    public ResponseEntity<Map<String, Object>> getMyDashboard() {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(adminAnalyticsService.getStudentDashboard(p.getUserId()));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserSummaryDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(authService.getUserProfile(id));
    }

    @GetMapping("/users/{id}/trust-score")
    public ResponseEntity<TrustScoreDetailDto> getUserTrustScore(@PathVariable Long id) {
        return ResponseEntity.ok(trustScoreService.getTrustScoreDetails(id));
    }

    @GetMapping("/users/{id}/reviews")
    public ResponseEntity<List<ReviewDto>> getUserReviews(@PathVariable Long id) {
        return ResponseEntity.ok(reviewDisputeService.getUserReviews(id));
    }
}
