package com.company.trade.entity;

public enum PaymentsStatus {
    PENDING,    // 거래 수락 직후: 결제 대기
    PAID,       // 결제 완료 (구매자가 결제 성공)
    FAILED,     // 결제 실패
    CANCELLED   // 결제 취소 (환불 또는 거래 파기)
}
