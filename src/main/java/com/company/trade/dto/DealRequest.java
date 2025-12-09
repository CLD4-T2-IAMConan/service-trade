package com.company.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DealRequest: 구매자 주도 양도 요청 생성 시 서버로 전송하는 DTO
 * (구매자가 원하는 티켓과 수량, 만료 시간 명시)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealRequest {

    // 구매자가 거래를 요청하는 티켓 ID
    private Long ticketId;

    // 요청하는 티켓의 개수
    private Integer quantity;

    // 양도 요청 만료 일시 (판매자가 이 시간 내에 수락해야 함)
    private LocalDateTime expireAt;

    // 이외에 buyerId는 Controller에서 로그인 정보로 가져옵니다.
}