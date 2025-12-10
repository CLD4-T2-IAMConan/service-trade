package com.company.trade.entity;

import com.company.trade.entity.PaymentsStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.math.BigDecimal; // price, refund_amount에 사용
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId; // payment_id

    @Column(nullable = false)
    private Long dealId; // deal_id (FK to Deal)

    @Column(nullable = false)
    private Long buyerId; // buyer_id (FK to Users)

    @Column(nullable = false)
    private Long sellerId; // seller_id (FK to Users)

    @Column(nullable = false, precision = 10, scale = 0)
    private BigDecimal price; // price (결제 금액)

    // 결제 상태 (PENDING, PAID 등)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentsStatus paymentStatus; // payment_status

    // 결제 요청/생성 시간 (payment_date)
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime paymentDate;

    // 트랜잭션 상태 (PG사와의 통신 상태 등, 복잡하므로 일단 Enum 대신 String으로 매핑)
    @Column(nullable = false, length = 50)
    private String transactionStatus; // transaction_status

    // 결제 완료 시간
    private LocalDateTime completionDate; // completion_date

    @Column(nullable = false, length = 50)
    private String paymentMethod; // payment_method (예: CARD, BANK_TRANSFER)

    private BigDecimal refundAmount; // refund_amount
    private String refundReason; // refund_reason
    private LocalDateTime refundDate; // refund_date
    private String cancelPaymentReason; // cancel_payment_reason

    // PG사 거래 ID 및 상태 (외부 통신 필드)
    @Column(length = 100)
    private String pgTid; // pg_tid

    @Column(length = 50)
    private String pgStatus; // pg_status

    @PrePersist
    protected void onCreate() {
        this.paymentDate = LocalDateTime.now();
    }
}
