// com/company/trade/dto/DealDetailResponse.java

package com.company.trade.dto;

import com.company.trade.entity.Deal;
import com.company.trade.entity.DealStatus;
import com.company.trade.entity.TicketStatus;
import com.company.trade.entity.TradeType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DealDetailResponse: 특정 거래의 상세 정보를 담는 DTO
 * (Deal 엔티티 정보 + TicketResponse 정보 결합)
 */
@Getter
@Builder
public class DealDetailResponse {

    // =======================================================
    // 1. 거래 (Deal) 정보
    // =======================================================
    private Long dealId;
    private Long sellerId; // 거래의 판매자 (티켓 소유자)
    private Long buyerId;  // 거래의 구매자 (요청자)
    private DealStatus dealStatus;
    private LocalDateTime dealAt;
    private LocalDateTime expireAt;
    private Integer quantity;


    // =======================================================
    // 2. 티켓 (Ticket) 상세 정보 (TicketResponse에서 가져옴)
    // =======================================================
    private Long ticketId; // Deal 정보에 이미 있지만, TicketResponse 필드와 묶어둠
    private String eventName;
    private LocalDateTime eventDate;
    private String eventLocation;
    private TicketStatus ticketStatus; // 현재 티켓의 상태 (AVAILABLE, RESERVED 등)
    private BigDecimal sellingPrice; // 거래 가격
    private String seatInfo;
    private String ticketType;
    private TradeType tradeType;
    private String description;
    private String image1;


    /**
     * Entity (Deal) 와 DTO (TicketResponse) 를 받아 상세 DTO로 변환하는 팩토리 메서드
     */
    public static DealDetailResponse from(Deal deal, TicketResponse ticketResponse) {
        // TicketResponse가 Optional 형태로 넘어올 수 있으므로 null 체크
        boolean hasTicket = ticketResponse != null;

        return DealDetailResponse.builder()
                // 1. Deal 정보 매핑
                .dealId(deal.getDealId())
                .sellerId(deal.getSellerId())
                .buyerId(deal.getBuyerId())
                .dealStatus(deal.getDealStatus())
                .dealAt(deal.getDealAt())
                .expireAt(deal.getExpireAt())
                .quantity(deal.getQuantity())

                // 2. Ticket 정보 매핑 (null 안전하게 처리)
                .ticketId(deal.getTicketId())
                .eventName(hasTicket ? ticketResponse.getEventName() : "조회 불가")
                .eventDate(hasTicket ? ticketResponse.getEventDate() : null)
                .eventLocation(hasTicket ? ticketResponse.getEventLocation() : "조회 불가")
                .ticketStatus(hasTicket ? ticketResponse.getTicketStatus() : null)
                .sellingPrice(hasTicket ? ticketResponse.getSellingPrice() : null)
                .seatInfo(hasTicket ? ticketResponse.getSeatInfo() : null)
                .ticketType(hasTicket ? ticketResponse.getTicketType() : null)
                .tradeType(hasTicket ? ticketResponse.getTradeType() : null)
                .description(hasTicket ? ticketResponse.getDescription() : null)
                .image1(hasTicket ? ticketResponse.getImage1() : null)

                .build();
    }
}