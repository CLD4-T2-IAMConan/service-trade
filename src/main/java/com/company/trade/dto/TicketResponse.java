package com.company.trade.dto;
import com.company.trade.entity.TicketStatus;
import com.company.trade.entity.TradeType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {

    private Long ticketId;

    private String eventName;
    private LocalDateTime eventDate;
    private String eventLocation;

    private Long ownerId;
    private TicketStatus ticketStatus;

    private BigDecimal originalPrice;
    private BigDecimal sellingPrice;

    private String seatInfo;
    private String ticketType;

    private Long categoryId;

    private String image1;
    private String image2;

    private String description;

    private TradeType tradeType;


    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
