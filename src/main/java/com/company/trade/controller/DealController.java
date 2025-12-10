package com.company.trade.controller;


import com.company.trade.dto.DealDetailResponse;
import com.company.trade.dto.DealRequest;
import com.company.trade.dto.DealResponse;
import com.company.trade.entity.Deal;
//import com.company.trade.service.DealCreationException;
import com.company.trade.service.DealService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

// 현재 로그인한 사용자 정보 (구매자 ID)를 가져오기 위해 Spring Security 의존성이 필요함
// 가정: UserDetailsImpl 객체에서 getUserId()를 통해 ID를 얻을 수 있습니다.
// import org.springframework.security.core.annotation.AuthenticationPrincipal;
// import com.passit.auth.UserDetailsImpl;


@RestController
@RequestMapping("/api/deals")
@RequiredArgsConstructor
public class DealController {

    private final DealService dealService;

    /**
     * [POST] 구매자가 특정 티켓에 대한 양도 요청을 생성합니다.
     * URI: POST /api/deals/request
     */
    @PostMapping("/request")
    public ResponseEntity<?> createDealRequest(
            @RequestBody DealRequest request
             , Principal principal // 인증 시스템 사용 시
    ) {
        // (1) 실제 환경에서는 인증된 사용자 정보를 가져와야 합니다.
        // Long buyerId = Long.parseLong(principal.getName());

        // *** 테스트를 위해 임시로 buyerId를 설정합니다. ***
        Long buyerId = 1L;

        try {
            // 2. 서비스 호출
            DealResponse response = dealService.createDealRequest(request, buyerId);

            // 3. 201 Created 응답 반환
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) { // 🌟🌟🌟 모든 RuntimeException을 잡습니다. 🌟🌟🌟
            // Service 계층에서 던지는 EntityNotFoundException, DealCreationException 등
            // 모든 사용자 정의 예외는 RuntimeException을 상속하므로 여기서 잡힙니다.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // 예상치 못한 서버 내부 오류
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("거래 요청 중 서버 오류가 발생했습니다.");
        }
    }

    @GetMapping("/ticket/{ticketId}/request")
    public ResponseEntity<?> getPendingDealDetails(
            @PathVariable Long ticketId
            , Principal principal // 인증 시스템 사용 시
    ) {
        // (1) 실제 환경에서는 판매자 인증 및 권한 검사가 필요합니다.
        // Long sellerId = Long.parseLong(principal.getName());

        // *** 테스트를 위해 임시 Seller ID를 3L로 설정하고, 서비스 내부에서 권한 검사를 수행한다고 가정합니다. ***
        // (여기서는 일단 데이터 조회만 성공시키는 것을 목표로 합니다.)

        try {
            // 2. 서비스 호출: 티켓 ID로 티켓 정보와 PENDING Deal 정보를 함께 가져옵니다.
            DealDetailResponse response = dealService.getPendingDealDetails(ticketId);

            // 3. 200 OK 응답 반환
            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            // 티켓을 찾을 수 없을 때
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (RuntimeException e) {
            // 기타 비즈니스 로직 오류 (예: PENDING 딜이 여러 개일 때 등)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // 예상치 못한 서버 내부 오류
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("거래 요청 상세 조회 중 서버 오류가 발생했습니다.");
        }
    }
}