package com.company.trade.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentsCompleteRequest {
    private String tid;       // NICEPAY 거래 ID
    private String authToken; // NICEPAY 인증 토큰
    // 프론트엔드가 POST로 보낼 다른 모든 파라미터를 포함해야 합니다.
}