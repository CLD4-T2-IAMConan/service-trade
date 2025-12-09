package com.company.trade.entity;

public enum TicketStatus {
    // DB의 ENUM 값과 완벽히 일치하도록 대문자로 작성해야 합니다.
    AVAILABLE,
    RESERVED,
    SOLD,
    USED,
    EXPIRED
    // ... 기타 필요한 상태
}