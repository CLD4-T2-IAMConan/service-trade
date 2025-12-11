package com.company.trade.dto;

import com.company.trade.entity.Payments;
import com.company.trade.entity.Deal;
import com.company.trade.entity.Ticket;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentsDetailResponse {

    private Payments payments;
    private Deal deal;
    private Ticket ticket;

    // 필요하다면, 엔티티를 DTO로 변환하는 정적 팩토리 메서드를 추가할 수 있습니다.
    public static PaymentsDetailResponse from(Payments payments, Deal deal, Ticket ticket) {
        return PaymentsDetailResponse.builder()
                .payments(payments)
                .deal(deal)
                .ticket(ticket)
                .build();
    }
}
