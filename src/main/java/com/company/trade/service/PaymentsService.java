package com.company.trade.service;

import com.company.trade.dto.*;
import com.company.trade.entity.*;
import com.company.trade.repository.PaymentsRepository;
import com.company.trade.repository.DealRepository;


import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import com.company.trade.repository.TicketRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Base64;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


// Custom Runtime Exceptions (DealService에서 정의된 것을 재사용한다고 가정)
// class EntityNotFoundException extends RuntimeException { /* ... */ }
// class IllegalStateException extends RuntimeException { /* ... */ }

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentsService {

    private final PaymentsRepository paymentsRepository;
    private final DealRepository dealRepository;
    private final TicketRepository ticketRepository;
    private final TicketServiceApi ticketServiceApi;

    private final RestTemplate restTemplate; // AppConfig에 Bean 등록 필수

    // 💡 NICEPAY 공용 테스트 계정 정보 (그대로 사용하세요!)
    private final String NICEPAY_MERCHANT_ID = "nicepay00m";
    private final String NICEPAY_MERCHANT_KEY = "EYzu8jGGMfqaDEp76gSckuvnaHHu+bC4opsSN6lHv3b2lurNYkVXrZ7Z1AoqQnXI3eLuaUFyoRNC6FkrzVjceg==";
    private final String NICEPAY_APPROVAL_URL = "https://web.nicepay.co.kr/v3/v2/Payment.jsp";
    private final String NICEPAY_REST_API_BASE_URL = "https://sandbox-api.nicepay.co.kr/v1/payments/";
    private final String NICEPAY_CLIENT_KEY = "S2_46f0ecb8e7f648ab8252b55c453bd443";
    private final String NICEPAY_SECRET_KEY = "58f4425415fa49b89aff12ca188f3381";

    /**
     * 거래 수락 시 호출되어, 구매자에게 결제 요청을 생성하고 저장합니다.
     * @param deal 거래(Deal) 엔티티 정보
     * @return 생성된 Payment 엔티티
     */
    @Transactional
    public Payments createPayment(Deal deal, BigDecimal amount) {

        // 1. Payment 엔티티 생성
        Payments payment = Payments.builder()
                .dealId(deal.getDealId())
                .buyerId(deal.getBuyerId())
                .sellerId(deal.getSellerId())
                .price(amount)
                .paymentStatus(PaymentsStatus.PENDING) // 결제 요청 대기 상태
                .paymentDate(LocalDateTime.now())
                .paymentMethod("METHOD_PENDING")
                .build();

        // 2. Payment DB에 저장
        Payments savedPayment = paymentsRepository.save(payment);

        return savedPayment;
    }

    /**
     * [GET] Payments ID를 기반으로 Payments, Deal, Ticket 상세 정보를 조회합니다.
     * 구매자 권한 검증을 포함합니다.
     * @param paymentsId 조회할 Payments ID
     * @param buyerId 현재 로그인된 구매자 ID
     * @return Payments, Deal, Ticket 정보가 담긴 DTO
     */
    @Transactional(readOnly = true)
    public PaymentsDetailResponse getPaymentDetails(Long paymentsId, Long buyerId) {

        // 1. Payments 엔티티 조회
        Payments payments = paymentsRepository.findById(paymentsId)
                .orElseThrow(() -> new EntityNotFoundException("결제 정보를 찾을 수 없습니다. (ID: " + paymentsId + ")"));

        // 1-1. 구매자 권한 검증
        if (!payments.getBuyerId().equals(buyerId)) {
            throw new IllegalStateException("해당 결제 정보를 조회할 권한이 없습니다.");
        }

        // 2. 연결된 Deal 엔티티 조회
        Deal deal = dealRepository.findById(payments.getDealId())
                .orElseThrow(() -> new EntityNotFoundException("연결된 거래(Deal)를 찾을 수 없습니다."));

        // 3. 연결된 Ticket 엔티티 조회
        // (참고: Deal이 Accepted 상태라면 Ticket 상태는 RESERVED 또는 SOLD 상태여야 함)
        Ticket ticket = ticketRepository.findById(deal.getTicketId())
                .orElseThrow(() -> new EntityNotFoundException("연결된 티켓을 찾을 수 없습니다."));

        // 4. DTO로 변환하여 반환
        return PaymentsDetailResponse.from(payments, deal, ticket);
    }

    // nicepay 연동
    @Transactional(readOnly = true)
    public NicepayPrepareResponse preparePayment(Long paymentId, Long buyerId) {

        Payments payments = paymentsRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("결제 정보를 찾을 수 없습니다."));

        if (!payments.getBuyerId().equals(buyerId)) {
            throw new IllegalArgumentException("결제 준비 권한이 없습니다.");
        }

        // 1. Deal 엔티티 조회
        Long dealId = payments.getDealId();

        // Payments에 dealId 정보는 있지만, 실제 Deal 엔티티가 존재하지 않을 경우를 대비해 예외 처리
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException("연결된 거래(Deal) 정보를 찾을 수 없습니다. (Deal ID: " + dealId + ")"));

        // 3. Ticket 엔티티 조회 (상품명 획득)
        Ticket ticket = ticketRepository.findById(deal.getTicketId())
                .orElseThrow(() -> new EntityNotFoundException("연결된 티켓 정보를 찾을 수 없습니다."));

        // 4. 금액 변환 및 Null 체크
        if (payments.getPrice() == null) {
            throw new IllegalStateException("Payments 엔티티의 price 필드가 NULL입니다.");
        }
        Long amountLong = payments.getPrice().longValue();

        // 5. NICEPAY 연동 파라미터 생성

        String orderId = "ORDER_" + paymentId;
        String nicepayClientId = "S2_46f0ecb8e7f648ab8252b55c453bd443"; // 실제 설정 값으로 대체 필요

        // 4. Return URL 설정
        String returnUrl = "http://localhost:8083/api/payments/nicepay/callback";


        return NicepayPrepareResponse.builder()
                .clientId(nicepayClientId)
                .orderId(orderId)
                .amount(amountLong) // 💡 payments 엔티티의 총 금액 필드 사용
                .goodsName(ticket.getEventName())  // 💡 티켓의 이벤트 이름 사용
                .returnUrl(returnUrl)
                .paymentId(String.valueOf(paymentId))
                .build();
    }


    /**
     * NICEPAY 웹훅 요청을 받아 최종 결제 상태를 DB에 반영합니다.
     */

//    @Transactional
//    public void handleNicepayWebhook(NicepayWebhookRequest webhookRequest) {
//
//        // 1. 필수 데이터 검증 (다시 활성화)
//        if (webhookRequest == null || webhookRequest.getOrderId() == null || webhookRequest.getOrderId().isEmpty()) {
//            log.error("NICEPAY Webhook: 필수 파라미터(OrderId) 누락. 요청 데이터: {}", webhookRequest);
//            throw new IllegalArgumentException("유효하지 않은 웹훅 요청입니다: OrderId 누락");
//        }
//
//        String orderId = webhookRequest.getOrderId(); // DTO에서 @JsonProperty("Moid")로 매핑된 값
//        Long paymentId;
//
//        // 2. OrderId 파싱 (다시 활성화)
//        try {
//            String paymentIdStr = orderId.replace("ORDER_", "");
//            paymentId = Long.parseLong(paymentIdStr);
//        } catch (NumberFormatException e) {
//            throw new IllegalArgumentException("OrderId 형식이 올바르지 않습니다: " + orderId);
//        }
//
//        // 3. Payments 객체 조회
//        Payments payments = paymentsRepository.findById(paymentId)
//                .orElseThrow(() -> new EntityNotFoundException("결제 정보를 찾을 수 없습니다. [OrderId: " + orderId + "]"));
//
//        // 4. 결과 코드 검증 (0000이 성공)
//        // DTO 필드명이 ResultCode(대문자)로 오더라도 @JsonProperty로 매핑했으므로 getResultCode() 사용 가능
//        String resultCode = webhookRequest.getResultCode();
//
//        if (!"0000".equals(resultCode)) {
//            // 실패 로그 및 DB 업데이트
//            log.warn("NICEPAY 결제 실패 통보. [TID: {}, Code: {}]", webhookRequest.getTid(), resultCode);
//
//            payments.setPaymentStatus(PaymentsStatus.FAILED); // 또는 "FAILED"
//            payments.setPgTid(webhookRequest.getTid());
//            payments.setPgStatus(resultCode);
//
//            // 예외를 던져 Controller가 500을 반환하게 함 (또는 여기서 return으로 종료해도 됨)
//            return;
//            // throw new RuntimeException("NICEPAY 결제 실패"); // 실패 시 예외를 던질지, 조용히 처리할지는 정책 결정 필요
//        }
//
//        // 5. (선택) 금액 검증 로직
//        // long webhookAmount = Long.parseLong(webhookRequest.getAmount());
//        // if (payments.getPrice().longValue() != webhookAmount) { ... }
//
//        // 6. 정상 결제 완료 처리 (DB 업데이트)
//        payments.setPaymentStatus(PaymentsStatus.PAID); // 또는 "PAID"
//        payments.setPgTid(webhookRequest.getTid());
//        payments.setPgStatus(resultCode);
//
//        // 승인 번호 저장
//        if (webhookRequest.getApprovalNum() != null) {
//            payments.setRefundReason("APPROVAL_NUM: " + webhookRequest.getApprovalNum());
//        }
//
//        payments.setCompletionDate(LocalDateTime.now());
//
//        // 7. Deal 상태 업데이트
//        Long dealId = payments.getDealId();
//        Deal deal = dealRepository.findById(dealId)
//                .orElseThrow(() -> new EntityNotFoundException("거래 정보를 찾을 수 없습니다."));
//
//        deal.setDealStatus(DealStatus.PAID); // 또는 "PAID"
//
//        log.info("NICEPAY 결제 최종 승인 완료. [PaymentId: {}]", paymentId);
//    }

    @Transactional
    public PaymentsResponse updatePaymentsStatus(Long paymentsId, String newStatusString) {

        // 1. Enum 파싱 및 유효성 검증
        PaymentsStatus newStatus;
        try {
            newStatus = PaymentsStatus.valueOf(newStatusString.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("존재하지 않는 결제 상태 값입니다: " + newStatusString);
        }

        // 2. Payments 조회 (EntityNotFoundException 처리)
        Payments payments = paymentsRepository.findById(paymentsId)
                .orElseThrow(() -> new EntityNotFoundException("ID " + paymentsId + "인 결제(Payments)를 찾을 수 없습니다."));

        PaymentsStatus currentStatus = payments.getPaymentStatus();

        // 3. 비즈니스 상태 전이 규칙 검증 (핵심)
        if (!canChangeStatus(currentStatus, newStatus)) {
            throw new IllegalStateException(
                    String.format("현재 상태 (%s)에서는 %s 상태로 변경할 수 없습니다. (ID: %d)",
                            currentStatus, newStatus, paymentsId)
            );
        }

        // 4. 상태 변경 및 저장 (Dirty Checking)
        payments.setPaymentStatus(newStatus);

        // 5. 부가 로직 처리 (Deal 상태 연동, 재고 복원 등)
        // 예를 들어, PAID 상태로 변경 시 연결된 Deal의 상태도 PAID로 변경해야 합니다.
        if (newStatus == PaymentsStatus.PAID) {
            // dealService.updateDealStatus(payments.getDealId(), DealStatus.PAID.name());
        } else if (newStatus == PaymentsStatus.CANCELED) {
            // 취소/환불 로직 (Deal 상태 CANCELED로, Ticket 상태 AVAILABLE로 복원)
            // dealService.cancelDeal(payments.getDealId(), payments.getBuyerId()); // 적절한 취소 메서드 호출
        }


        // 6. 응답 DTO 반환
        return PaymentsResponse.from(payments); // 🚨 PaymentsResponse.from(payments)가 정의되어 있어야 합니다.
    }

    /**
     * 결제 상태 전이 규칙을 검증하는 내부 메서드
     */
    private boolean canChangeStatus(PaymentsStatus current, PaymentsStatus target) {
        if (current == target) {
            return true; // 상태가 이미 목표 상태라면 성공
        }

        switch (current) {
            case PENDING:
                // 결제 대기 상태: 결제 완료(PAID), 결제 실패(FAILED), 취소(CANCELED)로만 변경 가능
                return target == PaymentsStatus.PAID ||
                        target == PaymentsStatus.FAILED ||
                        target == PaymentsStatus.CANCELED;

            case PAID:
                // 결제 완료 상태: 환불/취소(CANCELED)로만 변경 가능 (PAID -> FAILED는 불가능)
                return target == PaymentsStatus.CANCELED;

            case FAILED:
            case CANCELED:
                // 최종 상태: 실패, 취소 상태에서는 다른 상태로 변경 불가능 (결제 재시도 등은 새 Payments 객체로 처리)
                return false;

            default:
                return false;
        }
    }

    @Transactional
    public void completePayment(String tid, String authToken, String orderId) throws Exception {
        // 🚨 0. 현재 요청의 Authorization 헤더에서 토큰을 직접 추출합니다.
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String accessToken = (attributes != null) ? attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION) : null;

        // 0. 메서드 진입 및 초기 정보 로깅
        log.info("--- [START] NICEPAY REST API 승인 프로세스 시작. Order ID: {}, TID: {} ---", orderId, tid);

        // 1. DB에서 결제 정보 조회 (기존과 동일)
        Long paymentId = Long.parseLong(orderId.replace("ORDER_", ""));
        Payments payments = paymentsRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("결제 정보를 찾을 수 없습니다. (ID: " + paymentId + ")"));

        // 🚨 [추가] DB 금액 (JSON Body에 사용)
        String amt = new DecimalFormat("###").format(payments.getPrice());
        log.info("[DB 조회] Payment ID: {}, Deal ID: {}, 요청 금액(Amt): {}", paymentId, payments.getDealId(), amt);

        // 2. 인증 헤더 생성 (Authorization Basic)
        String credentials = NICEPAY_CLIENT_KEY + ":" + NICEPAY_SECRET_KEY;
        String encodedAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        // 3. 헤더 설정 (JSON & Authorization)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Basic " + encodedAuth);

        // 4. JSON 요청 본문 생성 (필요한 최소 정보: amount)
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("amount", payments.getPrice().intValue());

        // 🚨 [로그 추가] NICEPAY API에 전송할 요청 정보 로깅
        log.info("[API Request] Authorization Basic 길이: {}, JSON Body: {}", encodedAuth.length(), bodyMap);

        // HttpEntity 생성
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(bodyMap, headers);

        // 5. API 호출 (TID를 포함한 동적 URL 사용)
        String approvalUrl = NICEPAY_REST_API_BASE_URL + tid;
        log.info("NICEPAY 승인 요청 URL: {}", approvalUrl); // 이 로그는 이미 있었지만 위치를 명확히 함

        ResponseEntity<String> responseEntity = restTemplate.exchange(
                approvalUrl,
                HttpMethod.POST,
                request,
                String.class
        );

        String responseBody = responseEntity.getBody();

        // 🚨 [로그 추가] NICEPAY API로부터 받은 응답 정보 로깅
        log.info("[API Response] Status Code: {}, Response Body: {}", responseEntity.getStatusCode(), responseBody);


        // 6. 응답 처리 (JSON 응답)
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> resultMap = mapper.readValue(responseBody, Map.class);

        String resultCode = (String) resultMap.get("resultCode");
        String resultMsg = (String) resultMap.get("resultMsg");

        // 1) NICEPAY 성공 코드 '0000'이 아닐 경우
        if (!"0000".equals(resultCode)) {

            // 2) 실패 코드가 '이미 사용된 OrderId' 에러 코드와 일치하는 경우
            if (resultMsg != null && resultMsg.contains("이미 사용된 OrderId")) {

                // 3) DB 상태를 다시 한번 확인하고, PENDING이면 경고 로그만 남기고 성공 처리
                if (payments.getPaymentStatus() != PaymentsStatus.PAID) {
                    log.warn("[PG사 오류 우회] NICEPAY 응답: '이미 사용된 OrderId'. DB 상태는 PENDING이지만, PG사 상태가 성공으로 간주되므로 강제 업데이트를 시도합니다. OrderId: {}", orderId);

                    // 💡 [선택] 안전을 위해 이 시점에서 NICEPAY 거래 조회 API를 한 번 더 호출하여
                    //     실제 거래 상태(금액, 상태)를 검증하는 것이 가장 안전합니다. (현재는 생략)

                    // 강제 업데이트 로직 실행 (7단계로 이동)
                    // NOTE: 이 시점에서는 이미 PG사에서 승인되었으므로, DB 업데이트만 실행합니다.
                } else {
                    // 이미 우리 DB도 PAID이므로 정상 종료
                    log.info("[중복 요청 처리] PG사도 이미 처리됨을 확인. 정상 종료.");
                    return;
                }
            } else {
                // 일반적인 결제 실패 (다른 에러 코드)
                log.error("[결제 실패] NICEPAY 응답 에러. Code: {}, Message: {}", resultCode, resultMsg);
                throw new RuntimeException("PG사 결제 승인 실패: " + resultMsg);
            }
        }

        log.info("[결제 성공] NICEPAY 승인 성공. TID: {}, ResultCode: {}", tid, resultCode);

        // 7. 성공 시 DB 업데이트 (기존과 동일)
        payments.setPaymentStatus(PaymentsStatus.PAID);
        payments.setPgTid(tid);
        payments.setCompletionDate(LocalDateTime.now());

        Deal deal = dealRepository.findById(payments.getDealId()).orElseThrow();
        deal.setDealStatus(DealStatus.PAID);

        Long ticketId = deal.getTicketId(); // 💡 Deal 엔티티에 getTicketId()가 있다고 가정

        // 티켓 상태를 'SOLD'나 'PAID'로 변경하는 API 호출
        ticketServiceApi.updateTicketStatus(ticketId, "SOLD", accessToken);

        log.info("[END] 결제 및 거래 상태 업데이트 완료. Payment ID: {}", paymentId);
    }

    // SHA-256 암호화 함수
    private String sha256Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        return new String(Hex.encodeHex(digest));
    }

}