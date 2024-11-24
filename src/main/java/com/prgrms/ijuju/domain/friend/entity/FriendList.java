package com.prgrms.ijuju.domain.friend.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.prgrms.ijuju.domain.member.entity.Member;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
public class FriendList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "send_id", nullable = false)
    @NotNull
    private Member sender;

    @ManyToOne
    @JoinColumn(name = "receive_id", nullable = false)
    @NotNull
    private Member receiver;

    @NotNull
    @Enumerated(EnumType.STRING)
    private RelationshipStatus status;

    @CreatedDate
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}