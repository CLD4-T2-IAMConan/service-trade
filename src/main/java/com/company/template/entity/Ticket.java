package com.company.template.entity;

import jakarta.persistence.*; // Spring Boot 3.x 이상에서는 jakarta 패키지 사용
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket")
@Getter
@Setter // DealService에서 상태 변경을 위해 @Setter 추가 (혹은 상태 변경 메서드)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "event_name", nullable = false)
    private String eventName;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "event_location", nullable = false)
    private String eventLocation;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_status", nullable = false)
    private TicketStatus status; // DB 컬럼: ticket_status

    // 💡 해결: 'price' 대신 'original_price' 컬럼에 매핑
    @Column(name = "original_price", nullable = false, precision = 10, scale = 0)
    private Integer originalPrice; // Decimal(10,0)은 Java에서 Integer 또는 BigDecimal 사용 가능. 간단하게 Integer 사용.

    // 💡 해결: 'price' 대신 'selling_price' 컬럼에 매핑
    @Column(name = "selling_price", nullable = true, precision = 10, scale = 0)
    private Integer sellingPrice; // Decimal(10,0)은 Integer 사용.

    @Column(name = "seat_info", length = 100, nullable = true)
    private String seatInfo;

    @Column(name = "ticket_type", length = 50, nullable = true)
    private String ticketType;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- 비즈니스 로직 편의 메서드 ---
    /**
     * Deal 요청이 들어왔을 때 티켓 상태를 DEALING으로 변경합니다.
     */
    public void markAsDealing() {
        if (this.status != TicketStatus.AVAILABLE) {
            throw new IllegalStateException("티켓 상태가 AVAILABLE이 아니므로 거래 요청을 진행할 수 없습니다. 현재 상태: " + this.status);
        }
        this.status = TicketStatus.RESERVED;
        this.updatedAt = LocalDateTime.now();
    }
}