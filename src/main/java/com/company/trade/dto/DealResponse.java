package com.company.trade.dto;


import com.company.trade.entity.Deal;
import com.company.trade.entity.DealStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * DealResponse: 양도 요청 생성 성공 후 서버가 클라이언트에게 반환하는 DTO
 */
@Getter
@Builder
public class DealResponse {

    // 생성된 양도 요청 ID
    private Long dealId;

    // 대상 티켓 ID
    private Long ticketId;

    // 판매자 ID
    private Long sellerId;

    // 구매자 ID
    private Long buyerId;

    // 요청 생성 시간
    private LocalDateTime dealAt;

    // 현재 거래 상태 (예: PENDING)
    private DealStatus dealStatus;

    /**
     * Entity -> DTO 변환을 위한 정적 팩토리 메서드
     */
    public static DealResponse fromEntity(Deal deal) {
        return DealResponse.builder()
                .dealId(deal.getDealId())
                .ticketId(deal.getTicketId())
                .sellerId(deal.getSellerId())
                .buyerId(deal.getBuyerId())
                .dealAt(deal.getDealAt())
                .dealStatus(deal.getDealStatus())
                .build();
    }
}