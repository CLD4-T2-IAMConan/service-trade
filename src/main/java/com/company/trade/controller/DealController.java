package com.company.trade.controller;


import com.company.trade.dto.DealRequest;
import com.company.trade.dto.DealResponse;
import com.company.trade.service.DealService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 현재 로그인한 사용자 정보 (구매자 ID)를 가져오기 위해 Spring Security 의존성이 필요함
// 가정: UserDetailsImpl 객체에서 getUserId()를 통해 ID를 얻을 수 있습니다.
// import org.springframework.security.core.annotation.AuthenticationPrincipal;
// import com.passit.auth.UserDetailsImpl;


@RestController
@RequestMapping("/api/deals") // 기본 경로 설정
@RequiredArgsConstructor
public class DealController {

    private final DealService dealService;

    /**
     * [POST] 구매자가 특정 티켓에 대한 양도 요청을 생성합니다.
     * URI: POST /api/deals
     */
    @PostMapping
    public ResponseEntity<DealResponse> createDealRequest(
            @RequestBody DealRequest request
            // @AuthenticationPrincipal UserDetailsImpl userDetails // (1) 인증 정보 추출
    ) {

        // (1) 실제 환경에서는 인증된 사용자 정보를 가져와야 합니다.
        // Long buyerId = userDetails.getUserId();

        // *** 테스트를 위해 임시로 buyerId를 설정합니다. ***
        Long buyerId = 500L;

        // 2. 서비스 호출
        DealResponse response = dealService.createDealRequest(request, buyerId);

        // 3. 201 Created 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
