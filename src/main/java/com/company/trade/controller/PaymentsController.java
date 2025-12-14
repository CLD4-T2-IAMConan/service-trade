package com.company.trade.controller;

import com.company.trade.dto.*;
import com.company.trade.service.PaymentsService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.security.Principal; // Spring Security 사용자 인증 정보

@Slf4j
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
     * URL: /api/payments/{paymentId}/detail
     * * * 변경 사항: Principal 대신 @RequestParam을 사용하여 currentUserId를 받습니다.
     */
    @GetMapping("/{paymentId}/detail")
    public ResponseEntity<?> getPaymentDetails(
            @PathVariable Long paymentId,
            @RequestParam Long currentUserId) {

        Long buyerId = currentUserId; // 현재는 전달받은 currentUserId를 buyerId로 간주

        // PaymentsService 호출 (권한 검증 포함)
        // Service 계층에서는 이 buyerId가 해당 paymentId의 소유자인지 확인해야 합니다.
        PaymentsDetailResponse response = paymentsService.getPaymentDetails(paymentId, buyerId);

        return ResponseEntity.ok(response);
    }

    /**
     * [GET] NICEPAY 결제창 호출을 위한 준비 데이터 제공 API
     * URL: GET /api/payments/{paymentId}/prepare
     */
    @GetMapping("/{paymentId}/prepare")
    public ResponseEntity<?> preparePayment(
            @PathVariable Long paymentId,
            @RequestParam Long currentUserId) {

        try {
            Long buyerId = currentUserId;

            NicepayPrepareResponse response = paymentsService.preparePayment(paymentId, buyerId);

            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("결제 준비 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * [POST] NICEPAY 인증 성공 후 콜백 받는 엔드포인트 (NICEPAY가 POST 요청)
     * URL: POST /api/payments/nicepay/callback
     */
    @PostMapping("/nicepay/callback")
    public RedirectView nicepayCallback(@ModelAttribute NicepayCallbackRequest request) {
        // 💡 주의: NICEPAY는 폼 데이터(x-www-form-urlencoded)로 POST를 보내므로 @RequestBody 대신 @ModelAttribute를 사용해야 합니다.

        Long paymentId = null;
        try {
            // 1. NICEPAY가 전달한 orderId에서 paymentId 추출
            // orderId 형식: "ORDER_7"
            if (request.getOrderId() == null || !request.getOrderId().startsWith("ORDER_")) {
                throw new IllegalArgumentException("유효하지 않은 주문 번호 형식입니다.");
            }

            String paymentIdStr = request.getOrderId().substring("ORDER_".length());
            paymentId = Long.parseLong(paymentIdStr);

            // 3. 최종 승인 후, 프론트엔드 결과 페이지로 리다이렉트
            String redirectUrl = "http://localhost:3000/payments/" + paymentId + "/result"
                    + "?tid=" + request.getTid()
                    + "&authToken=" + request.getAuthToken()
                    // 💡 NICEPAY 인증 성공 코드를 명시적으로 넘겨서 프론트엔드에서 즉시 처리하도록 함
                    + "&authResultCode=0000";

            return new RedirectView(redirectUrl);

        } catch (IllegalArgumentException e) {
            // 파싱 실패 또는 유효하지 않은 orderId 처리
            return new RedirectView("http://localhost:3000/payments/" + paymentId + "/result");
        } catch (Exception e) {
            // 최종 승인 로직(completePayment) 실패 시 처리
            // 실제 서비스에서는 paymentId가 null이 아닐 경우 이 정보를 사용해 실패 DB 업데이트 후 리다이렉트합니다.
            String failUrl = "http://localhost:3000/payments/" + paymentId + "/result";
            if (paymentId != null) {
                failUrl = "http://localhost:3000/payments/" + paymentId + "/result";
            }
            return new RedirectView(failUrl);
        }
    }

    /**
     * [POST] 결제 최종 승인 요청을 받는 엔드포인트
     * 💡 프론트엔드가 쿼리 파라미터로 tid, authToken을 보낸다고 가정합니다.
     * URL: POST /api/payments/{paymentId}/complete?tid=...&authToken=...
     */
    @PostMapping("/{paymentId}/complete")
    public ResponseEntity<String> completePayment(
            @PathVariable String paymentId, // Payment ID (예: "1")
            @RequestParam("tid") String tid,      // NICEPAY 거래 ID
            @RequestParam("authToken") String authToken // NICEPAY 인증 토큰
    ) {
        try {
            log.info("--- 결제 최종 승인 요청 진입. Payment ID: {} ---", paymentId);
            // 🚨 [임시] DTO 없이, 파라미터만 제대로 넘어왔는지 확인 후 성공 반환
            log.info("NICEPAY 최종 승인 파라미터 확인 완료. TID: {}, AuthToken 길이: {}", tid, authToken.length());

            // ⚠️ 실제 서비스 호출 (주석 해제 필요)
             paymentsService.completePayment(tid, authToken, "ORDER_" + paymentId);

            return ResponseEntity.ok("PAYMENT_APPROVAL_SUCCESS"); // 명확한 성공 메시지

        } catch (Exception e) {
            log.error("결제 ID {} 최종 승인 처리 실패", paymentId, e);
            // 실패 시 500 에러와 함께 메시지 전달
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("결제 최종 승인 중 서버 오류 발생: " + e.getMessage());
        }
    }

    /**
     * [POST] NICEPAY Webhook 수신 엔드포인트
     * NICEPAY 서버가 결제 완료/실패 결과를 직접 통보하는 경로입니다.
     * URL: POST /api/payments/nicepay/webhook
     * * ⚠️ 이 URL은 NICEPAY 개발자 센터에 등록해야 합니다.
     */
    // PaymentsController.java

//    @PostMapping("/nicepay/webhook")
//    public ResponseEntity<String> nicepayWebhookHandler(
//            @RequestBody NicepayWebhookRequest webhookRequest) {
//        try {
//            paymentsService.handleNicepayWebhook(webhookRequest);
//            return ResponseEntity.ok("OK"); // 성공 시 200 OK
//        } catch (Exception e) {
//            // 3. 실패 시: 로그를 남기고 500 에러를 반환하여 NICEPAY가 알 수 있게 함
//            log.error("NICEPAY Webhook 처리 실패: {}", e.getMessage(), e);
//
//            // NICEPAY에게 "처리에 실패했음"을 명확히 알림 (재시도 유도)
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("FAIL");
//        }
//    }
}
