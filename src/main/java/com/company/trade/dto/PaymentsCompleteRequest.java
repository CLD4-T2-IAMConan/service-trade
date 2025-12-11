package com.company.trade.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentsCompleteRequest {
    private String paymentMethod; // 예: CARD, BANK_TRANSFER 등
    private String transactionId; // 실제 결제 시스템에서 받은 고유 거래 ID
    // 필요하다면 결제 금액 검증용 price 필드 추가 가능
}