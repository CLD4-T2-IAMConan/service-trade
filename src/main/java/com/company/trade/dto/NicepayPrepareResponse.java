// src/main/java/com/company/trade/dto/NicepayPrepareResponse.java

package com.company.trade.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NicepayPrepareResponse {
    // 프론트엔드에서 NICEPAY JS SDK 호출 시 필요한 값
    private String clientId;        // 가맹점 클라이언트 ID (개발자 페이지에서 확인)
    private String orderId;         // 주문 번호 (Deal ID와 연결된 고유 값)
    private Long amount;            // 최종 결제 금액 (Payments.totalAmount)
    private String goodsName;       // 상품명 (Ticket.eventName)
    private String returnUrl;       // NICEPAY 인증 성공 후 돌아올 URL
    private String paymentId;       // 거래를 식별하기 위한 Payments ID
    // 필요 시 Signature 등 보안 파라미터 추가 가능
}