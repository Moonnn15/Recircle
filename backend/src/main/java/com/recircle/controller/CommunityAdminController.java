package com.recircle.controller;

import com.recircle.dto.ApiDtos.*;
import com.recircle.enums.UserRole;
import com.recircle.security.AuthContext;
import com.recircle.security.AuthPrincipal;
import com.recircle.service.AdminAnalyticsService;
import com.recircle.service.NotificationService;
import com.recircle.service.ReviewDisputeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class CommunityAdminController {

    private final ReviewDisputeService reviewDisputeService;
    private final NotificationService notificationService;
    private final AdminAnalyticsService adminAnalyticsService;

    public CommunityAdminController(
        ReviewDisputeService reviewDisputeService,
        NotificationService notificationService,
        AdminAnalyticsService adminAnalyticsService
    ) {
        this.reviewDisputeService = reviewDisputeService;
        this.notificationService = notificationService;
        this.adminAnalyticsService = adminAnalyticsService;
    }

    // --- Reviews ---

    @PostMapping("/reviews")
    public ResponseEntity<ReviewDto> createReview(@RequestBody ReviewCreateRequest req) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.status(HttpStatus.CREATED).body(
            reviewDisputeService.createReview(p.getUserId(), req)
        );
    }

    @GetMapping("/reviews/{id}")
    public ResponseEntity<ReviewDto> getReviewById(@PathVariable Long id) {
        return ResponseEntity.ok(reviewDisputeService.getReviewById(id));
    }

    // --- Notifications ---

    @GetMapping("/notifications")
    public ResponseEntity<Map<String, Object>> getNotifications() {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(Map.of(
            "unreadCount", notificationService.getUnreadCount(p.getUserId()),
            "notifications", notificationService.getUserNotifications(p.getUserId())
        ));
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<NotificationDto> markNotificationRead(@PathVariable Long id) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(notificationService.markAsRead(id, p.getUserId()));
    }

    @PutMapping("/notifications/read-all")
    public ResponseEntity<Map<String, Object>> markAllNotificationsRead() {
        AuthPrincipal p = AuthContext.requireAuth();
        int updated = notificationService.markAllAsRead(p.getUserId());
        return ResponseEntity.ok(Map.of("updatedCount", updated, "message", "All notifications marked as read."));
    }

    // --- Disputes ---

    @PostMapping("/disputes")
    public ResponseEntity<DisputeDto> createDispute(@RequestBody DisputeCreateRequest req) {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.status(HttpStatus.CREATED).body(
            reviewDisputeService.createDispute(p.getUserId(), req)
        );
    }

    @GetMapping({"/disputes/my-disputes", "/disputes/my"})
    public ResponseEntity<List<DisputeDto>> getMyDisputes() {
        AuthPrincipal p = AuthContext.requireAuth();
        return ResponseEntity.ok(reviewDisputeService.getMyDisputes(p.getUserId()));
    }

    @GetMapping("/admin/disputes")
    public ResponseEntity<List<DisputeDto>> getAllDisputes() {
        AuthContext.requireRole(UserRole.ADMIN, UserRole.MODERATOR);
        return ResponseEntity.ok(reviewDisputeService.getAllDisputes());
    }

    @PutMapping("/admin/disputes/{id}/resolve")
    public ResponseEntity<DisputeDto> resolveDispute(
        @PathVariable Long id,
        @RequestBody DisputeResolveRequest req
    ) {
        AuthPrincipal p = AuthContext.requireRole(UserRole.ADMIN, UserRole.MODERATOR);
        return ResponseEntity.ok(reviewDisputeService.resolveDispute(id, p.getUserId(), req));
    }

    // --- Admin & Moderator Operations ---

    @GetMapping("/admin/dashboard")
    public ResponseEntity<Map<String, Object>> getAdminDashboard() {
        AuthContext.requireRole(UserRole.ADMIN, UserRole.MODERATOR);
        return ResponseEntity.ok(adminAnalyticsService.getAdminDashboardKpis());
    }

    @GetMapping("/admin/analytics")
    public ResponseEntity<Map<String, Object>> getAdminAnalytics(
        @RequestParam(defaultValue = "30d") String range
    ) {
        AuthContext.requireRole(UserRole.ADMIN, UserRole.MODERATOR);
        return ResponseEntity.ok(adminAnalyticsService.getAdminAnalytics(range));
    }

    @GetMapping("/admin/users")
    public ResponseEntity<List<UserSummaryDto>> getAllUsers() {
        AuthContext.requireRole(UserRole.ADMIN, UserRole.MODERATOR);
        return ResponseEntity.ok(adminAnalyticsService.getAllUsers());
    }

    @PutMapping("/admin/users/{id}/status")
    public ResponseEntity<UserSummaryDto> updateUserStatus(
        @PathVariable Long id,
        @RequestBody Map<String, Object> body
    ) {
        AuthPrincipal p = AuthContext.requireRole(UserRole.ADMIN);
        String status = body.get("accountStatus") != null
            ? String.valueOf(body.get("accountStatus"))
            : (body.get("status") != null ? String.valueOf(body.get("status")) : null);
        Boolean verified = body.get("campusVerified") != null ? Boolean.valueOf(String.valueOf(body.get("campusVerified"))) : null;
        return ResponseEntity.ok(adminAnalyticsService.updateUserStatus(id, p.getUserId(), status, verified));
    }

    @GetMapping("/admin/items")
    public ResponseEntity<List<ItemDto>> getAllItemsForAdmin() {
        AuthContext.requireRole(UserRole.ADMIN, UserRole.MODERATOR);
        return ResponseEntity.ok(adminAnalyticsService.getAllItemsForAdmin());
    }

    @GetMapping("/admin/transactions")
    public ResponseEntity<List<TransactionDto>> getAllTransactionsForAdmin() {
        AuthContext.requireRole(UserRole.ADMIN, UserRole.MODERATOR);
        return ResponseEntity.ok(adminAnalyticsService.getAllTransactionsForAdmin());
    }

    @GetMapping("/admin/audit-logs")
    public ResponseEntity<List<AuditLogDto>> getAuditLogs() {
        AuthContext.requireRole(UserRole.ADMIN, UserRole.MODERATOR);
        return ResponseEntity.ok(adminAnalyticsService.getAuditLogs());
    }
}
