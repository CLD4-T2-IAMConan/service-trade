// com/company/trade/dto/DealRejectRequest.java

package com.company.trade.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DealRejectRequest: 양도 거절 시 필요한 정보를 담는 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DealRejectRequest {

    // 프론트엔드에서 보낸 거절 사유를 받습니다.
    private String cancelReason;
    private Long currentUserId;
}
