package com.company.trade.dto; // 적절한 DTO 패키지로 설정하세요.

import com.company.trade.entity.Payments;
import com.company.trade.entity.PaymentsStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentsResponse {

    private Long paymentId;
    private Long dealId;
    private Long buyerId;
    private Long sellerId;
    private BigDecimal price;
    private PaymentsStatus paymentStatus;
    private LocalDateTime paymentDate;
    private LocalDateTime completionDate;
    private String paymentMethod;
    private BigDecimal refundAmount;
    private LocalDateTime refundDate;
    private String pgTid;
    private String pgStatus;

    /**
     * Payments 엔티티로부터 응답 DTO를 생성하는 팩토리 메서드
     */
    public static PaymentsResponse from(Payments payments) {
        return PaymentsResponse.builder()
                .paymentId(payments.getPaymentId())
                .dealId(payments.getDealId())
                .buyerId(payments.getBuyerId())
                .sellerId(payments.getSellerId())
                .price(payments.getPrice())
                .paymentStatus(payments.getPaymentStatus())
                .paymentDate(payments.getPaymentDate())
                .completionDate(payments.getCompletionDate())
                .paymentMethod(payments.getPaymentMethod())
                .refundAmount(payments.getRefundAmount())
                .refundDate(payments.getRefundDate())
                .pgTid(payments.getPgTid())
                .pgStatus(payments.getPgStatus())
                .build();
    }
}