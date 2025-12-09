package com.company.template.controller;

import com.company.template.dto.DealRequest;
import com.company.template.dto.DealResponse;
import com.company.template.service.DealService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Spring Security 인증 객체를 사용하는 경우 필요한 Import (가정)
// import org.springframework.security.core.annotation.AuthenticationPrincipal;
// import com.company.template.auth.UserDetailsImpl;

@RestController
@RequestMapping("/deals") // 양도 요청 API 기본 경로
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
            // , @AuthenticationPrincipal UserDetailsImpl userDetails // (1) 실제 Buyer ID를 가져오는 방법
    ) {

        // (1) 실제 환경에서는 로그인된 사용자(구매자) ID를 인증 정보에서 가져옵니다.
        // Long buyerId = userDetails.getUserId();

        // *** 현재 기능 구현 테스트를 위해 임시 buyerId를 사용합니다. ***
        Long buyerId = 500L;

        // 2. 서비스 호출: 구매자 주도 로직 실행
        DealResponse response = dealService.createDealRequest(request, buyerId);

        // 3. 201 Created 응답 반환 (자원 생성 성공)
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
