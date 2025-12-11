package com.company.trade.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NicepayCallbackRequest {
    // NICEPAY가 POST로 전달하는 필수 인증 파라미터 (이름은 NICEPAY 가이드 참고)
    private String tid; // Transaction ID
    private String authToken; // 인증 토큰
    private String signature; // 위변조 검증 시그니처
    private String orderId;
    // 그 외 NICEPAY가 전달하는 모든 필드 (예: AuthResultCode, TxnStatus 등)를 포함해야 합니다.
}