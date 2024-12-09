package com.prgrms.ijuju.domain.stock.adv.stockrecord.entity;

import com.prgrms.ijuju.domain.member.entity.Member;
import com.prgrms.ijuju.domain.stock.adv.advancedinvest.entity.AdvancedInvest;
import com.prgrms.ijuju.domain.stock.adv.stockrecord.constant.TradeType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne
    @JoinColumn(name = "adv_id")
    private AdvancedInvest advancedInvest;

    private String symbol;
    private double quantity;
    private BigDecimal pricePerUnit;

    @Enumerated(EnumType.STRING)
    private TradeType tradeType;

    private LocalDateTime tradeDate;


}