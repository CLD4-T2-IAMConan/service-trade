package com.company.trade.service;

import com.company.sns.EventMessage;
import com.company.sns.SnsEventPublisher;
import com.company.trade.dto.DealDetailResponse;
import com.company.trade.dto.DealRequest;
import com.company.trade.dto.DealResponse;
import com.company.trade.entity.*;
import com.company.trade.repository.DealRepository;
import com.company.trade.repository.PaymentsRepository;
import com.company.trade.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.company.trade.dto.TicketResponse;
import com.company.trade.service.PaymentsService;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String msg) { super(msg); }
}

class IllegalStateException extends RuntimeException {
    public IllegalStateException(String msg) { super(msg); }
}

class DealCreationException extends RuntimeException {
    public DealCreationException(String msg) { super(msg); }
}

@Slf4j
@Service
@RequiredArgsConstructor
public class DealService {

    private final TicketServiceApi ticketServiceApi;
    private final DealRepository dealRepository;
    private final PaymentsRepository paymentsRepository;
    private final PaymentsService paymentsService;
    private final SnsEventPublisher eventPublisher;

    /**
     * [Transactional] 새로운 거래 요청을 생성하고, 티켓 상태를 'RESERVED'로 변경합니다.
     * * @param request 거래 요청에 필요한 데이터 (ticketId, quantity, expireAt 등)
     * @param buyerId 요청을 생성한 구매자 ID
     * @return 생성된 거래 정보 DTO
     */
    @Transactional
    public DealResponse createDealRequest(DealRequest request, Long buyerId) {

        // 🚨 0. 현재 요청의 Authorization 헤더에서 토큰을 직접 추출합니다.
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String accessToken = (attributes != null) ? attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION) : null;

        // ===================================================================
        // 1. 티켓 정보 조회 및 유효성 검증
        // ===================================================================
        TicketResponse ticket = null;
        try {

            // 🚨 TicketServiceApi.getTicketById 호출
            ticket = ticketServiceApi.getTicketById(request.getTicketId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "요청된 티켓을 찾을 수 없습니다. (ID: " + request.getTicketId() + ")"
                    ));

        } catch (Exception e) {
            log.error("[LOG-1-1-ERROR] Ticket API 호출 중 예외 발생: {}", e.getMessage(), e);
            throw new RuntimeException("티켓 정보 조회 중 연결 오류 발생.", e); // 이 예외는 Controller에서 500 처리됨
        }


        // 티켓 상태 검증 (AVAILABLE 상태인지 확인)
        if (ticket.getTicketStatus() != TicketStatus.AVAILABLE) {
            log.warn("[LOG-1-3-FAIL] 티켓 상태 불일치. 현재 상태: {}", ticket.getTicketStatus());
            throw new DealCreationException(
                    "현재 티켓은 거래 요청을 받을 수 없습니다. 현재 상태: " + ticket.getTicketStatus()
            );
        }


        // ===================================================================
        // 2. 티켓 상태 변경 (AVAILABLE -> RESERVED)
        // ===================================================================
        try {

            // 🚨 TicketServiceApi.updateTicketStatus 호출
            ticketServiceApi.updateTicketStatus(request.getTicketId(), TicketStatus.RESERVED.name(), accessToken);

        } catch (RuntimeException e) {
            // 🚨 이 Catch 블록은 API 호출 오류(400, 404, 연결 오류)를 잡고 DealCreationException으로 전환
            log.error("[LOG-2-1-ERROR] 티켓 상태 변경 API 호출 실패: {}", e.getMessage(), e);
            throw new DealCreationException("티켓 상태를 RESERVED로 변경하는 데 실패했습니다. 티켓 서비스 오류: " + e.getMessage());
        }

        // ===================================================================
        // 3. Deal 엔티티 생성 및 저장
        // ===================================================================

        // 3. Deal 엔티티 생성 및 저장
        Deal deal = Deal.builder()
                .ticketId(request.getTicketId())
                .buyerId(buyerId)
                .sellerId(ticket.getOwnerId()) // 조회한 티켓의 소유자 ID 사용
                .quantity(request.getQuantity())
                .expireAt(request.getExpireAt())
                .dealStatus(DealStatus.PENDING) // 거래 요청 시점의 상태
                .dealAt(LocalDateTime.now())
                .build();

        Deal savedDeal = null;
        try {
            // 🚨 DealRepository.save 호출 (DB 저장 시점)
            savedDeal = dealRepository.save(deal);

        } catch (Exception e) {
            log.error("[LOG-3-2-ERROR] Deal DB 저장 실패 (Data Integrity Error 예상): {}", e.getMessage(), e);
            throw new RuntimeException("거래 정보 DB 저장 중 치명적인 오류 발생.", e); // 🚨 500 오류 유발 가능성
        }

        // 4. 이벤트 발행: deal.requested
        try {
            EventMessage event = EventMessage.create(
                "deal.requested",
                "service-trade",
                Map.of(
                    "dealId", savedDeal.getDealId(),
                    "ticketId", savedDeal.getTicketId(),
                    "buyerId", savedDeal.getBuyerId(),
                    "sellerId", savedDeal.getSellerId(),
                    "quantity", savedDeal.getQuantity()
                )
            );
            eventPublisher.publishAsync("deal-events", event);
            log.info("[SNS-EVENT] deal.requested 이벤트 발행 완료. Deal ID: {}", savedDeal.getDealId());
        } catch (Exception e) {
            log.error("[SNS-ERROR] deal.requested 이벤트 발행 실패: {}", e.getMessage());
            // 이벤트 발행 실패는 거래 생성을 중단시키지 않음
        }

        // 5. 응답 DTO 반환
        return DealResponse.fromEntity(savedDeal);
    }

    // dealDetail
    public DealDetailResponse getDealDetail(Long dealId) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException("거래 ID " + dealId + "번을 찾을 수 없습니다."));

        // 1. Ticket 정보 조회 (API 통신)
        Optional<TicketResponse> ticketOpt = ticketServiceApi.getTicketById(deal.getTicketId());

        // 2. Deal 엔티티와 조회된 정보를 합쳐 DealDetailResponse를 생성/반환
        return DealDetailResponse.from(deal, ticketOpt.orElse(null));
    }



    @Transactional
    public void rejectDeal(Long dealId, Long sellerId, String cancelReason) {
        // 🚨 0. 토큰 추출 (Ticket Service 호출 시 권한 인증을 위해 필요)
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String accessToken = (attributes != null) ? attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION) : null;

        // 1. Deal 요청 조회
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException("요청하신 거래(Deal)를 찾을 수 없습니다."));

        // 2. 비즈니스 유효성 검사
        // 2-1. 판매자 권한 검사
        if (!deal.getSellerId().equals(sellerId)) {
            throw new IllegalStateException("해당 거래를 거절할 권한이 없습니다.");
        }

        // 2-2. 상태 검사 (PENDING 상태일 때만 거절 가능)
        if (deal.getDealStatus() != DealStatus.PENDING) {
            throw new IllegalStateException("현재 거래 상태(" + deal.getDealStatus() + ")에서는 거절할 수 없습니다.");
        }

        // ===================================================================
        // 3. Ticket 상태 변경 (RESERVED -> AVAILABLE) - 외부 API 호출
        // ===================================================================
        try {
            // 🚨 중요: 직접 Repository를 쓰지 않고 API를 호출합니다.
            // Ticket Service 내부 로직에서 RESERVED 인지 검증하므로 여기서는 호출만 합니다.
            ticketServiceApi.updateTicketStatus(deal.getTicketId(), TicketStatus.AVAILABLE.name(), accessToken);

        } catch (RuntimeException e) {
            log.error("[REJECT-DEAL-ERROR] 티켓 상태 복구 API 호출 실패: {}", e.getMessage());
            throw new RuntimeException("티켓 상태를 AVAILABLE로 변경하는 데 실패했습니다: " + e.getMessage());
        }

        // ===================================================================
        // 4. Deal 상태 변경 및 저장 (내부 DB)
        // ===================================================================
        deal.setDealStatus(DealStatus.REJECTED);
        deal.setCancelReason(cancelReason);

        try {
            dealRepository.save(deal);
            log.info("[REJECT-DEAL-SUCCESS] 거래 거절 완료. Deal ID: {}, Ticket ID: {}", dealId, deal.getTicketId());
        } catch (Exception e) {
            log.error("[REJECT-DEAL-ERROR] Deal 상태 저장 실패: {}", e.getMessage());
            throw new RuntimeException("거래 거절 상태 저장 중 오류가 발생했습니다.");
        }

        // 이벤트 발행: deal.rejected
        try {
            EventMessage event = EventMessage.create(
                "deal.rejected",
                "service-trade",
                Map.of(
                    "dealId", dealId,
                    "ticketId", deal.getTicketId(),
                    "sellerId", sellerId,
                    "cancelReason", cancelReason != null ? cancelReason : ""
                )
            );
            eventPublisher.publishAsync("deal-events", event);
            log.info("[SNS-EVENT] deal.rejected 이벤트 발행 완료. Deal ID: {}", dealId);
        } catch (Exception e) {
            log.error("[SNS-ERROR] deal.rejected 이벤트 발행 실패: {}", e.getMessage());
        }
    }

    @Transactional
    public void acceptDeal(Long dealId, Long sellerId) {
        log.info("[DEAL_ACCEPT_START] 거래 수락 시작. Deal ID: {}, Seller ID: {}", dealId, sellerId);

        // 🚨 0. 토큰 추출 (Ticket/Payment Service 호출 시 인증 정보 전달을 위해 필요)
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String accessToken = (attributes != null) ? attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION) : null;

        // 1. Deal 요청 조회
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException("요청하신 거래(Deal)를 찾을 수 없습니다."));

        // ===================================================================
        // 1. 권한 및 상태 검증 (내부 DB 로직)
        // ===================================================================
        if (!deal.getSellerId().equals(sellerId)) {
            log.warn("[AUTH_FAIL] 권한 불일치. 요청 Seller ID: {}, 거래 Owner ID: {}", sellerId, deal.getSellerId());
            throw new IllegalStateException("해당 거래를 수락할 권한이 없습니다.");
        }
        if (deal.getDealStatus() != DealStatus.PENDING) {
            log.warn("[STATUS_FAIL] 상태 불일치. 현재 상태: {}", deal.getDealStatus());
            throw new IllegalStateException("현재 거래 상태(" + deal.getDealStatus() + ")에서는 수락할 수 없습니다.");
        }

        // ===================================================================
        // 2. 티켓 가격 조회 및 결제 금액 계산 (외부 API 호출 포함)
        // ===================================================================
        BigDecimal ticketPrice;
        try {
            // 🚨 TicketServiceApi를 호출할 때 토큰을 함께 넘길 수 있도록 구조가 잡혀있어야 합니다.
            // 만약 getTicketById도 토큰이 필요하다면 메서드 시그니처를 수정하여 accessToken을 넘겨주세요.
            TicketResponse ticket = ticketServiceApi.getTicketById(deal.getTicketId())
                    .orElseThrow(() -> new EntityNotFoundException("연결된 티켓을 찾을 수 없습니다."));

            ticketPrice = ticket.getSellingPrice();

            if (ticketPrice == null) {
                log.error("[PRICE_NULL_ERROR] TicketService에서 받은 가격이 NULL입니다.");
                throw new RuntimeException("티켓 가격 정보가 누락되었습니다.");
            }
        } catch (Exception e) {
            log.error("[LOG-API-ERROR] Ticket API 호출 중 예외 발생: {}", e.getMessage());
            throw new RuntimeException("티켓 정보 조회 중 오류가 발생했습니다.", e);
        }

        // 결제 금액 계산
        if (deal.getQuantity() == null || deal.getQuantity() <= 0) {
            throw new IllegalStateException("유효하지 않은 거래 수량입니다.");
        }
        BigDecimal paymentAmount = ticketPrice.multiply(BigDecimal.valueOf(deal.getQuantity()));

        // ===================================================================
        // 3. Payment Service 호출 (Payment 엔티티 생성)
        // ===================================================================
        try {
            // 🚨 paymentsService 내부에서도 외부 API(결제 서비스 등)를 호출한다면
            // 여기서 accessToken을 인자로 넘겨주도록 수정하는 것이 좋습니다.
            // 예: paymentsService.createPayment(deal, paymentAmount, accessToken);
            paymentsService.createPayment(deal, paymentAmount);
            log.info("[LOG-PAYMENT-SUCCESS] Deal ID {}에 대한 Payment 생성 완료.", dealId);

        } catch (Exception e) {
            log.error("[LOG-PAYMENT-FAIL] Payment 생성 실패: {}", e.getMessage());
            throw new RuntimeException("결제 요청 생성 중 오류가 발생했습니다.", e);
        }

        // ===================================================================
        // 4. Deal 상태 변경 및 저장 (내부 DB)
        // ===================================================================
        deal.setDealStatus(DealStatus.ACCEPTED);

        try {
            dealRepository.save(deal);
            log.info("[DEAL_ACCEPT_END] 거래 수락 완료. Deal ID: {} -> ACCEPTED", dealId);
        } catch (Exception e) {
            log.error("[DEAL_SAVE_ERROR] Deal 상태 저장 실패: {}", e.getMessage());
            throw new RuntimeException("거래 상태 업데이트 중 오류가 발생했습니다.");
        }

        // 이벤트 발행: deal.accepted
        try {
            EventMessage event = EventMessage.create(
                "deal.accepted",
                "service-trade",
                Map.of(
                    "dealId", dealId,
                    "ticketId", deal.getTicketId(),
                    "buyerId", deal.getBuyerId(),
                    "sellerId", sellerId,
                    "paymentAmount", paymentAmount.toString()
                )
            );
            eventPublisher.publishAsync("deal-events", event);
            log.info("[SNS-EVENT] deal.accepted 이벤트 발행 완료. Deal ID: {}", dealId);
        } catch (Exception e) {
            log.error("[SNS-ERROR] deal.accepted 이벤트 발행 실패: {}", e.getMessage());
        }
    }


    @Transactional
    public DealResponse updateDealStatus(Long dealId, String newStatusString) {

        // 1. Enum 파싱 및 유효성 검증
        DealStatus newStatus;
        try {
            // 입력받은 문자열을 Enum으로 변환
            newStatus = DealStatus.valueOf(newStatusString.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 유효하지 않은 Enum 값일 경우 예외 발생
            throw new IllegalArgumentException("존재하지 않는 거래 상태 값입니다: " + newStatusString);
        }

        // 2. Deal 조회 (EntityNotFoundException 처리)
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException("ID " + dealId + "인 거래(Deal)를 찾을 수 없습니다."));

        // 3. 비즈니스 상태 전이 규칙 검증 (핵심)
        if (!canChangeStatus(deal.getDealStatus(), newStatus)) {
            throw new IllegalStateException(
                    String.format("현재 상태 (%s)에서는 %s 상태로 변경할 수 없습니다.",
                            deal.getDealStatus(), newStatus)
            );
        }

        // 4. 상태 변경 및 저장 (Dirty Checking)
        deal.setDealStatus(newStatus);
        // dealRepository.save(deal); // @Transactional이 있으므로 생략 가능하나 명시적으로 호출할 수도 있습니다.

        // 5. 응답 DTO 반환
        return DealResponse.fromEntity(deal); // 🚨 DealResponse.fromEntity(deal)가 정의되어 있어야 합니다.
    }

    /**
     * 거래 상태 전이 규칙을 검증하는 내부 메서드
     * DealStatus: PENDING, ACCEPTED, REJECTED, PAID, COMPLETED, CANCELED, FAILED
     */
    private boolean canChangeStatus(DealStatus current, DealStatus target) {
        if (current == target) {
            return true; // 상태가 이미 목표 상태라면 성공
        }

        switch (current) {
            case PENDING:
                // 요청 상태: 수락, 거절, 취소/실패만 가능
                return target == DealStatus.ACCEPTED ||
                        target == DealStatus.REJECTED ||
                        target == DealStatus.CANCELED ||
                        target == DealStatus.FAILED;

            case ACCEPTED:
                // 수락 상태: 결제 완료(PAID), 취소/실패만 가능 (판매자가 거절할 수 없음)
                return target == DealStatus.PAID ||
                        target == DealStatus.CANCELED ||
                        target == DealStatus.FAILED;

            case PAID:
                // 결제 완료: 최종 완료(COMPLETED), 취소/실패만 가능
                return target == DealStatus.COMPLETED ||
                        target == DealStatus.CANCELED ||
                        target == DealStatus.FAILED;

            case REJECTED:
            case CANCELED:
            case COMPLETED:
            case FAILED:
                // 최종 상태: 이미 거절, 취소, 완료, 실패된 거래는 상태 변경 불가능 (종료 상태)
                return false;

            default:
                return false;
        }
    }

    @Transactional
    public void cancelDeal(Long dealId, Long buyerId) {
        log.info("[CANCEL_DEAL_START] 거래 취소 시작. Deal ID: {}, Buyer ID: {}", dealId, buyerId);

        // 🚨 0. 토큰 추출 (Ticket Service 상태 복구를 위해 전달 필요)
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String accessToken = (attributes != null) ? attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION) : null;

        // 1. Deal 엔티티 조회
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException("취소할 거래(Deal)를 찾을 수 없습니다. (ID: " + dealId + ")"));

        // 2. 권한 및 상태 검증
        if (!deal.getBuyerId().equals(buyerId)) {
            log.warn("[AUTH_FAIL] 권한 없음. 요청 Buyer: {}, 거래 Buyer: {}", buyerId, deal.getBuyerId());
            throw new IllegalArgumentException("해당 거래를 취소할 권한이 없습니다.");
        }

        // 취소 가능 상태 확인 (PENDING 또는 ACCEPTED 상태에서만 취소 가능)
        if (deal.getDealStatus() != DealStatus.PENDING && deal.getDealStatus() != DealStatus.ACCEPTED) {
            log.warn("[STATUS_FAIL] 취소 불가 상태: {}", deal.getDealStatus());
            throw new IllegalArgumentException("현재 거래 상태(" + deal.getDealStatus() + ")에서는 취소할 수 없습니다.");
        }

        // ===================================================================
        // 3. Ticket 상태 복원 (RESERVED -> AVAILABLE) - 외부 API 호출
        // ===================================================================
        try {
            // 🚨 중요: 레포지토리를 직접 쓰지 않고 API를 통해 티켓 서비스를 업데이트합니다.
            ticketServiceApi.updateTicketStatus(deal.getTicketId(), TicketStatus.AVAILABLE.name(), accessToken);
            log.info("[API-TICKET-SUCCESS] 티켓 상태를 AVAILABLE로 복구 완료. Ticket ID: {}", deal.getTicketId());
        } catch (Exception e) {
            log.error("[API-TICKET-ERROR] 티켓 상태 복구 중 API 오류 발생: {}", e.getMessage());
            throw new RuntimeException("티켓 서비스와의 통신 중 오류가 발생하여 취소를 완료할 수 없습니다.");
        }

        // ===================================================================
        // 4. 내부 데이터 상태 변경 (Payments & Deal)
        // ===================================================================

        // 4-1. Payments 상태 변경: PENDING -> CANCELED
        paymentsRepository.findByDealId(dealId)
                .ifPresent(payments -> {
                    if (payments.getPaymentStatus() == PaymentsStatus.PENDING) {
                        payments.setPaymentStatus(PaymentsStatus.CANCELLED);
                        paymentsRepository.save(payments);
                        log.info("[PAYMENT_CANCEL_SUCCESS] 결제 대기 데이터 취소 완료.");
                    }
                });

        // 4-2. Deal 상태 변경: CANCELED
        deal.setDealStatus(DealStatus.CANCELED);
        dealRepository.save(deal);

        log.info("[CANCEL_DEAL_END] 거래 취소 완료. Deal ID: {} -> CANCELED", dealId);

        // 이벤트 발행: deal.cancelled
        try {
            EventMessage event = EventMessage.create(
                "deal.cancelled",
                "service-trade",
                Map.of(
                    "dealId", dealId,
                    "ticketId", deal.getTicketId(),
                    "buyerId", buyerId
                )
            );
            eventPublisher.publishAsync("deal-events", event);
            log.info("[SNS-EVENT] deal.cancelled 이벤트 발행 완료. Deal ID: {}", dealId);
        } catch (Exception e) {
            log.error("[SNS-ERROR] deal.cancelled 이벤트 발행 실패: {}", e.getMessage());
        }
    }


    @Transactional
    public void confirmDeal(Long dealId, Long userId) {
        // 🚨 0. 토큰 추출 (Ticket Service 상태 변경 시 권한 인증을 위해 필요)
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String accessToken = (attributes != null) ? attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION) : null;

        log.info("[CONFIRM_DEAL_START] 구매 확정 프로세스 시작. Deal ID: {}, User ID: {}", dealId, userId);

        // 1. Deal 엔티티 조회
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException("거래 정보를 찾을 수 없습니다. (ID: " + dealId + ")"));

        // ===================================================================
        // 2. 권한 및 상태 검증 (Trade DB 내부 로직)
        // ===================================================================

        // A. 권한 검증: 구매자 본인인지 확인
        if (!deal.getBuyerId().equals(userId)) {
            log.warn("[AUTH_FAIL] 권한 없음. 요청자: {}, 거래 구매자: {}", userId, deal.getBuyerId());
            throw new IllegalArgumentException("거래 확정 권한이 없습니다. (구매자만 확정 가능)");
        }

        // B. Deal 상태 검증: 결제가 완료된(PAID) 상태여야 확정 가능
        if (deal.getDealStatus() != DealStatus.PAID) {
            log.warn("[STATUS_FAIL] 거래 상태 부적절. 현재 상태: {}", deal.getDealStatus());
            throw new IllegalArgumentException("결제가 완료된 거래만 확정할 수 있습니다. 현재 상태: " + deal.getDealStatus());
        }

        // C. Payments 상태 검증
        Payments payments = paymentsRepository.findByDealId(dealId)
                .orElseThrow(() -> new EntityNotFoundException("결제 정보를 찾을 수 없습니다."));

        if (payments.getPaymentStatus() != PaymentsStatus.PAID) { // 🚨 Enum 체크 시 PAID 또는 SUCCESS 확인
            throw new IllegalArgumentException("결제 상태가 완료(PAID)가 아닙니다.");
        }

        // ===================================================================
        // 3. Ticket 상태 확인 및 변경 (외부 API 호출)
        // ===================================================================
        Long ticketId = deal.getTicketId();

        try {
            // 3-1. TicketServiceApi를 통해 실시간 티켓 정보 조회
            TicketResponse ticket = ticketServiceApi.getTicketById(ticketId)
                    .orElseThrow(() -> new EntityNotFoundException("연관된 티켓 정보를 찾을 수 없습니다. (ID: " + ticketId + ")"));

            log.debug("[TICKET_INFO] 조회된 티켓 상태: {}", ticket.getTicketStatus());

            // 3-2. 티켓 상태 검증 (SOLD 상태일 때만 USED로 변경 가능)
            if (ticket.getTicketStatus() != TicketStatus.SOLD) {
                log.warn("[TICKET_STATUS_FAIL] 티켓 상태 부적절. 현재: {}", ticket.getTicketStatus());
                throw new IllegalArgumentException("티켓이 판매 완료(SOLD) 상태가 아닙니다. 현재 상태: " + ticket.getTicketStatus());
            }

            // 3-3. Ticket 상태 변경: SOLD -> USED
            // 🚨 createDeal 패턴: API를 호출하여 티켓 서비스의 상태를 업데이트합니다.
            ticketServiceApi.updateTicketStatus(ticketId, TicketStatus.USED.name(), accessToken);
            log.info("[API-TICKET-SUCCESS] 티켓 상태를 USED로 변경 완료. Ticket ID: {}", ticketId);

        } catch (IllegalArgumentException e) {
            throw e; // 비즈니스 로직 예외는 그대로 던짐
        } catch (Exception e) {
            log.error("[API-TICKET-ERROR] Ticket API 호출 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("티켓 서비스와의 통신 중 오류가 발생했습니다.", e);
        }

        // ===================================================================
        // 4. Deal 상태 변경 및 저장 (내부 DB)
        // ===================================================================
        deal.setDealStatus(DealStatus.COMPLETED);

        try {
            dealRepository.save(deal);
            log.info("[CONFIRM_DEAL_END] 구매 확정 성공. Deal ID: {} -> COMPLETED", dealId);
        } catch (Exception e) {
            log.error("[DEAL_SAVE_ERROR] Deal 상태 저장 실패: {}", e.getMessage());
            throw new RuntimeException("거래 완료 처리 중 데이터베이스 오류가 발생했습니다.");
        }

        // 이벤트 발행: deal.confirmed
        try {
            EventMessage event = EventMessage.create(
                "deal.confirmed",
                "service-trade",
                Map.of(
                    "dealId", dealId,
                    "ticketId", ticketId,
                    "buyerId", userId,
                    "sellerId", deal.getSellerId()
                )
            );
            eventPublisher.publishAsync("deal-events", event);
            log.info("[SNS-EVENT] deal.confirmed 이벤트 발행 완료. Deal ID: {}", dealId);
        } catch (Exception e) {
            log.error("[SNS-ERROR] deal.confirmed 이벤트 발행 실패: {}", e.getMessage());
        }
    }
}


