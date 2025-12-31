package com.company.trade.dto;

import com.company.trade.entity.Deal;
import com.company.trade.entity.Payments;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentsDetailResponse {

    // 🚨 엔티티 대신 Response DTO들을 담도록 변경합니다.
    private PaymentsResponse payments;
    private DealResponse deal;
    private TicketResponse ticket;

    /**
     * [수정] Ticket 엔티티 대신 TicketResponse(DTO)를 매개변수로 받습니다.
     */
    public static PaymentsDetailResponse from(Payments payments, Deal deal, TicketResponse ticketResponse) {
        return PaymentsDetailResponse.builder()
                .payments(PaymentsResponse.from(payments)) // 엔티티 -> DTO 변환
                .deal(DealResponse.fromEntity(deal))             // 엔티티 -> DTO 변환
                .ticket(ticketResponse)                    // 외부 API에서 받은 DTO 그대로 사용
                .build();
    }
}