package com.company.trade.dto; // 💡 실제 프로젝트의 DTO 패키지 경로로 변경해 주세요.

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 거래 확정(구매 확정) 요청에 사용되는 DTO
 * 현재 요청을 보낸 사용자의 ID를 담아 서버에서 권한을 검증하는 용도입니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmDealRequest {

    // 현재 로그인된 사용자의 ID
    // 이 ID는 서버에서 토큰/세션에서 얻은 ID와 일치하는지 검증하는 데 사용되어야 합니다.
    private Long currentUserId;

    // 필요에 따라 요청에 추가적인 검증 정보(예: 결제 확인 코드 등)를 추가할 수 있습니다.
}