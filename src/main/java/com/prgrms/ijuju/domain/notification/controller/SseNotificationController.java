package com.prgrms.ijuju.domain.notification.controller;

import com.prgrms.ijuju.domain.notification.dto.request.FriendNotificationRequest;
import com.prgrms.ijuju.domain.notification.dto.request.MessageNotificationRequest;
import com.prgrms.ijuju.domain.notification.service.NotificationService;
import com.prgrms.ijuju.domain.notification.service.SseNotificationService;
import com.prgrms.ijuju.global.auth.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class SseNotificationController {
    private final NotificationService notificationService;
    private final SseNotificationService sseNotificationService;

    // 친구 요청 알림 생성
    @PostMapping("/friend-request")
    public ResponseEntity<String> createFriendRequestNotification(
            @AuthenticationPrincipal SecurityUser securityUser,
            @RequestBody FriendNotificationRequest request
    ) {
        notificationService.createFriendRequestNotification(
                securityUser.getUsername(),
                request.getReceiverUsername()
        );
        return ResponseEntity.ok("친구 요청 알림 생성 완료");
    }

    // 친구 수락 알림 생성
    @PostMapping("/friend-accept")
    public ResponseEntity<String> createFriendAcceptNotification(
            @AuthenticationPrincipal SecurityUser securityUser,
            @RequestBody FriendNotificationRequest request
    ) {
        notificationService.createFriendAcceptNotification(
                securityUser.getUsername(),
                request.getReceiverUsername()
        );
        return ResponseEntity.ok("친구 수락 알림 생성 완료");
    }

    // 메시지 알림 생성
    @PostMapping("/message")
    public ResponseEntity<Void> createMessageNotification(
            @AuthenticationPrincipal SecurityUser securityUser,
            @RequestBody MessageNotificationRequest request
    ) {
        notificationService.createMessageNotification(
                securityUser.getUsername(),
                request.getReceiverLoginId(),
                request.getMessageContent()
        );
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribe(@AuthenticationPrincipal SecurityUser securityUser,
                                                @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId) {
        String loginId = securityUser.getUsername();
        SseEmitter ssemitter = sseNotificationService.subscribe(loginId, lastEventId);

        return ResponseEntity.ok(ssemitter);
    }
}