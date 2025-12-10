package com.company.trade.dto;

import com.company.trade.entity.Deal;
import com.company.trade.entity.Ticket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DealDetailResponse {

    // 티켓 정보
    private Ticket ticket;

    // PENDING Deal 요청 정보 (없을 경우 null)
    private Deal deal;

    // 필요하다면, Entity를 DTO로 변환하는 정적 메서드를 추가할 수 있습니다.
    public static DealDetailResponse from(Ticket ticket, Deal deal) {
        return DealDetailResponse.builder()
                .ticket(ticket)
                .deal(deal)
                .build();
    }
}