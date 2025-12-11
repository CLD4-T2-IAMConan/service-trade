package com.company.trade.controller;

import com.company.trade.dto.PaymentsDetailResponse;
import com.company.trade.dto.PaymentsCompleteRequest;
import com.company.trade.service.PaymentsService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal; // Spring Security 사용자 인증 정보

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentsController {

    private final PaymentsService paymentsService;

    // ⚠️ 임시 사용자 ID 추출 함수 (실제는 Spring Security Context에서 추출해야 함)
    // 현재 로그인된 사용자의 ID를 얻는 로직을 가정합니다.
    private Long getUserId(Principal principal) {
        // 실제 구현에서는 principal.getName() (username)을 사용하여 DB에서 ID를 조회해야 함
        // 여기서는 임시로 하드코딩된 값을 반환한다고 가정합니다.
        // **실제 배포 시에는 반드시 인증 로직으로 대체해야 합니다.**
        return 1L; // 예시: 현재 구매자(테스터) ID가 1이라고 가정
    }

    /**
     * [GET] 결제 정보 상세 조회 API
     * URL: /api/payments/{paymentId}/details
     */
    @GetMapping("/{paymentId}/details")
    public ResponseEntity<?> getPaymentDetails(
            @PathVariable Long paymentId,
            Principal principal) {
            Long buyerId = getUserId(principal); // 현재 로그인된 사용자 ID

            // PaymentsService 호출 (권한 검증 포함)
            PaymentsDetailResponse response = paymentsService.getPaymentDetails(paymentId, buyerId);

            return ResponseEntity.ok(response);
    }

    /**
     * [POST] 결제 완료 처리 API
     * URL: /api/payments/{paymentId}/complete
     */
    @PostMapping("/{paymentId}/complete")
    public ResponseEntity<?> completePayment(
            @PathVariable Long paymentId,
            @RequestBody PaymentsCompleteRequest request) {

        try {
            // 💡 결제 시스템과의 연동/검증 로직은 PaymentsService 내부에서 처리되었다고 가정합니다.
            paymentsService.completePayment(paymentId, request);

            // 성공 응답 (HTTP 200 OK)
            return ResponseEntity.ok().body("결제가 성공적으로 완료되었습니다.");

        } catch (EntityNotFoundException e) {
            // Payments나 연결된 Deal이 없는 경우
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (IllegalStateException e) {
            // Payments 상태가 PENDING이 아니거나 Deal 상태가 ACCEPTED가 아닌 경우
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            // 기타 서버 오류
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("결제 완료 처리 중 예상치 못한 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
