package com.company.trade.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "deal")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Deal {

    /**
     * PK (기본 키) - deal_id
     * MySQL DDL에서 AUTO_INCREMENT가 설정되어 있으므로 GenerationType.IDENTITY 사용
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deal_id")
    private Long dealId;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId; // 티켓 ID (FK)

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId; // 구매자 ID (FK)

    @Column(name = "seller_id", nullable = false)
    private Long sellerId; // 판매자 ID (FK)

    @Column(name = "deal_at", nullable = false)
    private LocalDateTime dealAt; // 양도 요청 생성 시간

    /**
     * deal_status ENUM 매핑
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "deal_status", nullable = false)
    private DealStatus dealStatus;

    @Column(name = "quantity", nullable = false)
    private Integer quantity; // 양도 요청 티켓 개수

    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt; // 양도 요청 만료 일시

    @Column(name = "cancel_reason")
    private String cancelReason; // 취소 사유 (NULL 허용)

    /**
     * [비즈니스 로직] 거래 상태 및 취소 사유를 업데이트하는 메서드
     */
    public void updateStatus(DealStatus newStatus, String cancelReason) {
        this.dealStatus = newStatus;
        this.cancelReason = cancelReason;
    }
}