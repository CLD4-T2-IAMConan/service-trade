package com.company.trade.service;

import com.company.trade.dto.*;
import com.company.trade.entity.*;
import com.company.trade.repository.PaymentsRepository;
import com.company.trade.repository.DealRepository;

import java.math.BigDecimal;
import java.util.Date;

import com.company.trade.repository.TicketRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
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

    private final RestTemplate restTemplate; // AppConfig에 Bean 등록 필수

    // 💡 NICEPAY 공용 테스트 계정 정보 (그대로 사용하세요!)
    private final String NICEPAY_MERCHANT_ID = "nicepay00m";
    private final String NICEPAY_MERCHANT_KEY = "EYzu8jGGMfqaDEp76gSckuvnaHHu+bC4opsSN6lHv3b2lurNYkVXrZ7Z1AoqQnXI3eLuaUFyoRNC6FkrzVjceg==";
    private final String NICEPAY_APPROVAL_URL = "https://web.nicepay.co.kr/v3/v2/Payment.jsp";

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

    @Transactional
    public void handleNicepayWebhook(NicepayWebhookRequest webhookRequest) {

        // 1. 필수 데이터 검증 (다시 활성화)
        if (webhookRequest == null || webhookRequest.getOrderId() == null || webhookRequest.getOrderId().isEmpty()) {
            log.error("NICEPAY Webhook: 필수 파라미터(OrderId) 누락. 요청 데이터: {}", webhookRequest);
            throw new IllegalArgumentException("유효하지 않은 웹훅 요청입니다: OrderId 누락");
        }

        String orderId = webhookRequest.getOrderId(); // DTO에서 @JsonProperty("Moid")로 매핑된 값
        Long paymentId;

        // 2. OrderId 파싱 (다시 활성화)
        try {
            String paymentIdStr = orderId.replace("ORDER_", "");
            paymentId = Long.parseLong(paymentIdStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("OrderId 형식이 올바르지 않습니다: " + orderId);
        }

        // 3. Payments 객체 조회
        Payments payments = paymentsRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("결제 정보를 찾을 수 없습니다. [OrderId: " + orderId + "]"));

        // 4. 결과 코드 검증 (0000이 성공)
        // DTO 필드명이 ResultCode(대문자)로 오더라도 @JsonProperty로 매핑했으므로 getResultCode() 사용 가능
        String resultCode = webhookRequest.getResultCode();

        if (!"0000".equals(resultCode)) {
            // 실패 로그 및 DB 업데이트
            log.warn("NICEPAY 결제 실패 통보. [TID: {}, Code: {}]", webhookRequest.getTid(), resultCode);

            payments.setPaymentStatus(PaymentsStatus.FAILED); // 또는 "FAILED"
            payments.setPgTid(webhookRequest.getTid());
            payments.setPgStatus(resultCode);

            // 예외를 던져 Controller가 500을 반환하게 함 (또는 여기서 return으로 종료해도 됨)
            return;
            // throw new RuntimeException("NICEPAY 결제 실패"); // 실패 시 예외를 던질지, 조용히 처리할지는 정책 결정 필요
        }

        // 5. (선택) 금액 검증 로직
        // long webhookAmount = Long.parseLong(webhookRequest.getAmount());
        // if (payments.getPrice().longValue() != webhookAmount) { ... }

        // 6. 정상 결제 완료 처리 (DB 업데이트)
        payments.setPaymentStatus(PaymentsStatus.PAID); // 또는 "PAID"
        payments.setPgTid(webhookRequest.getTid());
        payments.setPgStatus(resultCode);

        // 승인 번호 저장
        if (webhookRequest.getApprovalNum() != null) {
            payments.setRefundReason("APPROVAL_NUM: " + webhookRequest.getApprovalNum());
        }

        payments.setCompletionDate(LocalDateTime.now());

        // 7. Deal 상태 업데이트
        Long dealId = payments.getDealId();
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException("거래 정보를 찾을 수 없습니다."));

        deal.setDealStatus(DealStatus.PAID); // 또는 "PAID"

        log.info("NICEPAY 결제 최종 승인 완료. [PaymentId: {}]", paymentId);
    }

    @Transactional
    public void completePayment(String tid, String authToken, String orderId) throws Exception {

        // 1. DB에서 결제 정보 조회
        Long paymentId = Long.parseLong(orderId.replace("ORDER_", ""));
        Payments payments = paymentsRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("결제 정보를 찾을 수 없습니다."));

        // 2. 요청 데이터 준비
        String amt = String.valueOf(payments.getPrice().longValue());
        String ediDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        // 💡 SignData 생성 (공용 키 사용)
        // 서명 데이터 = authToken + mid + amt + ediDate + merchantKey (순서 중요!)
        String signDataStr = authToken + NICEPAY_MERCHANT_ID + amt + ediDate + NICEPAY_MERCHANT_KEY;
        String signData = sha256Hex(signDataStr);

        // 3. 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 4. 바디 설정
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("TID", tid);
        params.add("AuthToken", authToken);
        params.add("MID", NICEPAY_MERCHANT_ID);
        params.add("Amt", amt);
        params.add("EdiDate", ediDate);
        params.add("SignData", signData);
        params.add("CharSet", "utf-8");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        // 5. API 호출 (여기가 진짜 결제 승인 단계)
        log.info("NICEPAY 승인 요청 시작: {}", params);
        String responseBody = restTemplate.postForObject(NICEPAY_APPROVAL_URL, request, String.class);
        log.info("NICEPAY 승인 응답: {}", responseBody);

        // 6. 응답 처리 (간단한 파싱)
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> resultMap = mapper.readValue(responseBody, Map.class);

        String resultCode = (String) resultMap.get("ResultCode");

        if (!"3001".equals(resultCode)) { // 3001이 카드 결제 성공 코드 (테스트환경)
            throw new RuntimeException("결제 승인 실패: " + resultMap.get("ResultMsg"));
        }

        // 7. 성공 시 DB 업데이트
        payments.setPaymentStatus(PaymentsStatus.PAID);
        payments.setPgTid(tid);
        payments.setCompletionDate(LocalDateTime.now());

        Deal deal = dealRepository.findById(payments.getDealId()).orElseThrow();
        deal.setDealStatus(DealStatus.PAID);
    }

    // SHA-256 암호화 함수
    private String sha256Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        return new String(Hex.encodeHex(digest));
    }

}