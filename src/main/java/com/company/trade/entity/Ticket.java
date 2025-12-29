package com.company.trade.entity;

import com.company.trade.entity.TicketStatus;
import com.company.trade.entity.TradeType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long ticketId;

    // 티켓 기본 정보
    @Column(name = "event_name", nullable = false)
    private String eventName;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "event_location", nullable = false)
    private String eventLocation;

    // 소유자 정보 (외래키: users.user_id)
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    // 티켓 상태 ENUM 매핑
    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_status", nullable = false)
    private TicketStatus ticketStatus = TicketStatus.AVAILABLE;

    // 가격 정보
    @Column(name = "original_price", nullable = false, precision = 10, scale = 0)
    private BigDecimal originalPrice;

    @Column(name = "selling_price", precision = 10, scale = 0)
    private BigDecimal sellingPrice;


    // 티켓 상세 정보
    @Column(name = "seat_info")
    private String seatInfo;

    @Column(name = "ticket_type")
    private String ticketType;

    // 카테고리 ID (FK)
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    // 이미지 (선택 사항)
    @Column(name = "image1")
    private String image1;

    @Column(name = "image2")
    private String image2;

    // 상세 설명 (TEXT)
    @Column(name = "description", columnDefinition = "TEXT", nullable = true)
    private String description;

    // 거래 방식 ENUM
    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type", nullable = false)
    private TradeType tradeType;



    // 타임스탬프
    @Column(name = "created_at", insertable = false,  updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at" , insertable = false, updatable = false)
    private LocalDateTime updatedAt;


}
