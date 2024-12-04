package com.prgrms.ijuju.domain.friend.controller;

import com.prgrms.ijuju.domain.friend.dto.request.FriendRequestStatusDTO;
import com.prgrms.ijuju.domain.friend.dto.request.FriendRequestDTO;
import com.prgrms.ijuju.domain.friend.dto.response.FriendListResponseDTO;
import com.prgrms.ijuju.domain.friend.dto.response.FriendResponseDTO;
import com.prgrms.ijuju.domain.friend.entity.RequestStatus;
import com.prgrms.ijuju.domain.friend.service.FriendService;
import com.prgrms.ijuju.global.auth.SecurityUser;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
@Slf4j
public class FriendController {
    
    private final FriendService friendService;

    // 친구 요청 보내기
    @PostMapping("/request")
    public ResponseEntity<String> sendFriendRequest(
            @AuthenticationPrincipal SecurityUser user,
            @RequestBody FriendRequestDTO requestDTO) {
        return ResponseEntity.ok(friendService.sendFriendRequest(user.getId(), requestDTO.getReceiverId()));
    }

    // 친구 요청 처리 (수락/거절)
    @PostMapping("/request/{requestId}")
    public ResponseEntity<String> processFriendRequest(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long requestId,
            @RequestBody FriendRequestStatusDTO statusDTO) {
        String response;
        if (statusDTO.getStatus() == RequestStatus.ACCEPTED) {
            response = friendService.acceptFriendRequest(requestId, user.getId());
        } else {
            response = friendService.rejectFriendRequest(requestId, user.getId());
        }
        return ResponseEntity.ok(response);
    }

    // 보낸 친구 요청 목록 조회
    @GetMapping("/request/sent")
    public ResponseEntity<List<FriendResponseDTO>> showSentFriendRequests(
            @AuthenticationPrincipal SecurityUser user) {
        return ResponseEntity.ok(friendService.showSentFriendRequests(user.getId()));
    }

    // 보낸 친구 요청 취소
    @DeleteMapping("/request/sent/{requestId}")
    public ResponseEntity<String> cancelFriendRequest(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long requestId) {
        String response = friendService.cancelFriendRequest(user.getId(), requestId);
        return ResponseEntity.ok(response);
    }

    // 받은 친구 요청 목록 조회
    @GetMapping("/request/received")
    public ResponseEntity<List<FriendResponseDTO>> showReceivedFriendRequests(
            @AuthenticationPrincipal SecurityUser user) {
        return ResponseEntity.ok(friendService.showReceivedFriendRequests(user.getId()));
    }

    // 친구 목록 조회
    @GetMapping("/list")
    public ResponseEntity<Page<FriendListResponseDTO>> showFriends(
            @AuthenticationPrincipal SecurityUser user,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(friendService.showFriends(user.getId(), page, size));
    }

    // 친구 삭제
    @DeleteMapping("/remove/{friendId}")
    public ResponseEntity<String> removeFriend(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long friendId) {
        return ResponseEntity.ok(friendService.removeFriend(user.getId(), friendId));
    }
}
