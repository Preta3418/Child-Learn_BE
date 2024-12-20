package com.prgrms.ijuju.domain.notification.entity;

import com.prgrms.ijuju.domain.member.entity.Member;
import com.prgrms.ijuju.global.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private Member receiver;

    private Long senderLoginId;

    private String senderUsername;

    @Column(nullable = false)
    private String title;

    private String content;

    @Column(nullable = false)
    private Boolean isRead;

    @Column(nullable = false)
    private Boolean isDeleted; // soft delete

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    // 읽음 처리 메서드
    public void markAsRead() {
        this.isRead = true;
    }

    // 소프트 삭제 메서드
    public void softDelete() {
        this.isDeleted = true;
    }
}
