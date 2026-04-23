package com.medibook.notification.controller;

import com.medibook.notification.dto.request.SendBulkNotificationRequest;
import com.medibook.notification.dto.response.BulkDispatchResponse;
import com.medibook.notification.dto.response.MessageResponse;
import com.medibook.notification.dto.response.NotificationResponse;
import com.medibook.notification.dto.response.UnreadCountResponse;
import com.medibook.notification.enums.NotificationChannel;
import com.medibook.notification.enums.NotificationType;
import com.medibook.notification.security.AuthenticatedUser;
import com.medibook.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) Boolean unreadOnly) {
        return ResponseEntity.ok(notificationService.getByRecipient(
                authenticatedUser.userId(),
                unreadOnly,
                authenticatedUser));
    }

    @GetMapping("/me/unread-count")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UnreadCountResponse> getMyUnreadCount(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(notificationService.getUnreadCount(
                authenticatedUser.userId(),
                authenticatedUser));
    }

    @PutMapping("/me/read-all")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MessageResponse> markMyNotificationsAsRead(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(notificationService.markAllRead(
                authenticatedUser.userId(),
                authenticatedUser));
    }

    @GetMapping("/recipients/{recipientId}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByRecipient(
            @PathVariable String recipientId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) Boolean unreadOnly) {
        return ResponseEntity.ok(notificationService.getByRecipient(
                recipientId,
                unreadOnly,
                authenticatedUser));
    }

    @GetMapping("/recipients/{recipientId}/unread-count")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UnreadCountResponse> getUnreadCountByRecipient(
            @PathVariable String recipientId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(notificationService.getUnreadCount(
                recipientId,
                authenticatedUser));
    }

    @PutMapping("/recipients/{recipientId}/read-all")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MessageResponse> markAllRead(
            @PathVariable String recipientId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(notificationService.markAllRead(
                recipientId,
                authenticatedUser));
    }

    @PutMapping("/{notificationId}/read")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable String notificationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId, authenticatedUser));
    }

    @DeleteMapping("/{notificationId}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MessageResponse> deleteNotification(
            @PathVariable String notificationId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(notificationService.deleteNotification(notificationId, authenticatedUser));
    }

    @PostMapping("/bulk")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BulkDispatchResponse> sendBulk(
            @Valid @RequestBody SendBulkNotificationRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.sendBulk(request, authenticatedUser));
    }

    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<NotificationResponse>> getAll(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) String recipientId,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) NotificationChannel channel,
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) String relatedId) {
        return ResponseEntity.ok(notificationService.getAll(
                recipientId,
                type,
                channel,
                read,
                relatedId,
                authenticatedUser));
    }
}
