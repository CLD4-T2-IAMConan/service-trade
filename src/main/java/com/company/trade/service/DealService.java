package com.company.trade.service;

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
    private final TicketRepository ticketRepository;
    private final PaymentsRepository paymentsRepository;
    private final PaymentsService paymentsService;

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

        // 4. 응답 DTO 반환
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
    public void rejectDeal(Long dealId, Long sellerId, String cancelReason) { // 🚨 1. cancelReason 매개변수 추가

        // 1. Deal 요청 조회
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException("요청하신 거래(Deal)를 찾을 수 없습니다."));

        // 2. 비즈니스 유효성 검사
        // 2-1. 판매자 권한 검사 (현재 로그인한 사용자가 티켓의 주인인지)
        if (!deal.getSellerId().equals(sellerId)) {
            throw new IllegalStateException("해당 거래를 거절할 권한이 없습니다.");
        }

        // 2-2. 상태 검사 (PENDING 상태일 때만 거절 가능)
        if (deal.getDealStatus() != DealStatus.PENDING) {
            throw new IllegalStateException("현재 거래 상태(" + deal.getDealStatus() + ")에서는 거절할 수 없습니다.");
        }

        // 3. Deal 상태 변경: REJECTED 및 거절 사유 저장
        deal.setDealStatus(DealStatus.REJECTED);

        // 🚨 2. Deal 엔티티에 거절 사유(cancelReason) 저장
        // Deal 엔티티에 'cancelReason' 필드가 존재하고 setter가 있다고 가정합니다.
        deal.setCancelReason(cancelReason);

        dealRepository.save(deal);

        // 4. Ticket 상태 변경: RESERVED -> AVAILABLE
        // 티켓을 조회하고 상태를 변경합니다.
        Ticket ticket = ticketRepository.findById(deal.getTicketId())
                .orElseThrow(() -> new EntityNotFoundException("연결된 티켓을 찾을 수 없습니다."));

        // 4-1. 티켓 상태 검사 (RESERVED 상태일 때만 AVAILABLE로 변경)
        if (ticket.getStatus() != TicketStatus.RESERVED) {
            throw new IllegalStateException("티켓 상태가 RESERVED가 아니므로 AVAILABLE로 변경할 수 없습니다.");
        }

        // 4-2. 상태 변경
        ticket.setStatus(TicketStatus.AVAILABLE);
        ticketRepository.save(ticket);
    }

    /**
     * 거래를 수락하고, 결제 엔티티를 생성한 후, Deal 상태를 ACCEPTED로 변경합니다.
     * @param dealId 수락할 거래 ID
     * @param sellerId 요청한 판매자 ID (권한 검증용)
     */
    @Transactional
    public void acceptDeal(Long dealId, Long sellerId) {

        log.info("[DEAL_ACCEPT_START] 거래 수락 시작. Deal ID: {}, Seller ID: {}", dealId, sellerId);

        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException("요청하신 거래(Deal)를 찾을 수 없습니다."));

        log.debug("[DEAL_ACCEPT_INFO] Deal 조회 완료. Ticket ID: {}, Current Status: {}",
                deal.getTicketId(), deal.getDealStatus());

        // ===================================================================
        // 1. 권한 및 상태 검증
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
        // 2. 티켓 가격 조회 및 결제 금액 계산
        // ===================================================================
        BigDecimal ticketPrice;

        try {
            // 2-1. TicketServiceApi를 통해 티켓 정보 조회
            TicketResponse ticket = ticketServiceApi.getTicketById(deal.getTicketId())
                    .orElseThrow(() -> new EntityNotFoundException("연결된 티켓을 찾을 수 없습니다."));

            log.debug("[TICKET_INFO_CHECK] 조회된 Ticket ID: {}, Selling Price (Raw): {}",
                    ticket.getTicketId(), ticket.getSellingPrice());

            // 2-2. 티켓 가격 추출 (ticketPrice가 null인지 확인)
            ticketPrice = ticket.getSellingPrice();

            // 🚨 [핵심 로그 1] 티켓 가격이 null인지 확인
            if (ticketPrice == null) {
                log.error("[PRICE_NULL_ERROR] TicketService에서 받은 가격이 NULL입니다. Ticket ID: {}", deal.getTicketId());
                throw new RuntimeException("티켓 가격 정보가 누락되었습니다.");
            }

        } catch (EntityNotFoundException e) {
            // 티켓이 DB에 없는 경우
            log.error("[LOG-PRICE-ERROR] 티켓을 찾을 수 없습니다. Deal ID: {}, Ticket ID: {}", dealId, deal.getTicketId());
            throw new EntityNotFoundException(e.getMessage());
        } catch (Exception e) {
            // API 연결 오류, JSON 파싱 오류 등 모든 외부 통신 오류를 포착
            log.error("[LOG-API-ERROR] Ticket API 호출 중 예외 발생: {}", e.getMessage(), e);
            throw new RuntimeException("티켓 가격 정보 조회 중 통신 오류가 발생했습니다.", e);
        }

        // 2-3. 총 결제 금액 계산: (티켓 가격 * 수량)
        log.debug("[CALC_CHECK] Price: {}, Quantity: {}", ticketPrice, deal.getQuantity());

        // 🚨 [핵심 로그 2] 수량(Quantity) 필드에 문제가 없는지 확인
        if (deal.getQuantity() == null || deal.getQuantity() <= 0) {
            log.error("[QUANTITY_ERROR] 거래 수량 값이 유효하지 않습니다. Quantity: {}", deal.getQuantity());
            throw new IllegalStateException("유효하지 않은 거래 수량입니다.");
        }

        BigDecimal paymentAmount = ticketPrice.multiply(BigDecimal.valueOf(deal.getQuantity()));
        log.info("[PAYMENT_AMOUNT] 계산된 최종 결제 금액: {}", paymentAmount);


        // ===================================================================
        // 3. Payment Service 호출 (Payment 엔티티 생성)
        // ===================================================================
        try {
            // Payment 엔티티 생성 및 DB 저장 (PaymentStatus: PENDING)
            paymentsService.createPayment(deal, paymentAmount);
            log.info("[LOG-PAYMENT-SUCCESS] Deal ID {}에 대한 Payment가 성공적으로 생성되었습니다.", dealId);

        } catch (Exception e) {
            // 🚨 [핵심 로그 3] Payment DB 저장 또는 필수 필드 누락 오류 포착
            log.error("[LOG-PAYMENT-FAIL] Payment 생성/DB 저장 실패 (Deal ID {}): {}", dealId, e.getMessage(), e);
            log.error("[LOG-PAYMENT-FAIL] 상세 스택 트레이스:", e); // 상세 스택 트레이스 로깅
            throw new RuntimeException("결제 요청 생성 중 DB 또는 필수 필드 누락 오류가 발생했습니다.", e);
        }

        // ===================================================================
        // 4. Deal 상태 변경: PENDING -> ACCEPTED
        // ===================================================================
        deal.setDealStatus(DealStatus.ACCEPTED);
        dealRepository.save(deal);

        log.info("[DEAL_ACCEPT_END] 거래 수락 및 상태 변경 완료. Deal ID: {} -> ACCEPTED", dealId);
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

    // 1. 거래 취소 메서드 (구매자용)
    @Transactional
    public void cancelDeal(Long dealId, Long buyerId) {

        // 1. Deal 엔티티 조회
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException("취소할 거래(Deal)를 찾을 수 없습니다. (ID: " + dealId + ")"));

        // 2. 권한 및 상태 검증
        if (!deal.getBuyerId().equals(buyerId)) {
            throw new IllegalArgumentException("해당 거래를 취소할 권한이 없습니다.");
        }
        // 취소 가능한 상태(ACCEPTED)인지 확인. (PENDING 상태에서 취소하면 DEAL_REQUEST_PAGE에서 처리할 수도 있으나, 여기서는 ACCEPTED 후 결제 전 상황에 집중)
        if (deal.getDealStatus() != DealStatus.PENDING && deal.getDealStatus() != DealStatus.ACCEPTED) {
            throw new IllegalArgumentException("거래 상태(" + deal.getDealStatus() + ")에서는 취소할 수 없습니다.");
        }

        // 3. Payments 상태 변경: PENDING -> CANCELED
        // Deal ID를 사용하여 Payments를 찾습니다.
        paymentsRepository.findByDealId(dealId)
                .ifPresent(payments -> {
                    // 결제 상태가 PENDING일 때만 취소 처리하는 것이 안전함
                    if (payments.getPaymentStatus() == PaymentsStatus.PENDING) {
                        payments.setPaymentStatus(PaymentsStatus.CANCELED);
                        paymentsRepository.save(payments);
                    }
                });

        // 4. Ticket 상태 복원: RESERVED -> AVAILABLE
        ticketRepository.findById(deal.getTicketId())
                .ifPresent(ticket -> {
                    // 티켓 상태를 예약(RESERVED)에서 구매 가능(AVAILABLE)으로 복원
                    ticket.setStatus(TicketStatus.AVAILABLE);
                    ticketRepository.save(ticket);
                });

        // 5. Deal 상태 변경: ACCEPTED -> CANCELED
        deal.setDealStatus(DealStatus.CANCELED);
        dealRepository.save(deal);
    }


    public void confirmDeal(Long dealId, Long userId) {
        // 🚨 0. 현재 요청의 Authorization 헤더에서 토큰을 직접 추출합니다.
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String accessToken = (attributes != null) ? attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION) : null;

        log.info("[START] 구매 확정 프로세스 시작. Deal ID: {}, 요청 사용자 ID: {}", dealId, userId);

        // 1. Deal 엔티티 조회 및 권한/상태 검증
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException("거래 정보를 찾을 수 없습니다."));

        log.debug("Deal 엔티티 조회 성공. Deal ID: {}, Buyer ID: {}, Deal Status: {}",
                dealId, deal.getBuyerId(), deal.getDealStatus());


        // A. 권한 검증: 요청자가 구매자인지 확인
        if (!deal.getBuyerId().equals(userId)) {
            log.warn("권한 검증 실패: 요청 사용자 ID ({})는 Deal의 구매자 ID ({})와 일치하지 않습니다.", userId, deal.getBuyerId());
            throw new IllegalArgumentException("거래 확정 권한이 없습니다. (구매자만 확정 가능)");
        }
        log.debug("권한 검증 통과. 사용자 ID: {}", userId);

        // B. 상태 검증: Deal이 PAID 상태인지 확인
        if (deal.getDealStatus() != DealStatus.PAID) {
            log.warn("Deal 상태 검증 실패: 현재 상태 ({})는 PAID가 아닙니다.", deal.getDealStatus());
            throw new IllegalArgumentException("거래 상태가 PAID가 아닙니다. 현재 상태: " + deal.getDealStatus());
        }
        log.debug("Deal 상태 검증 통과. 현재 상태: PAID");


        // C. Payments 상태 검증
        Payments payments = paymentsRepository.findByDealId(dealId)
                .orElseThrow(() -> new EntityNotFoundException("결제 정보를 찾을 수 없습니다."));



        if (payments.getPaymentStatus() != PaymentsStatus.PAID) {
            log.warn("Payments 상태 검증 실패: 현재 상태 ({})는 PAID가 아닙니다.", payments.getPaymentStatus());
            throw new IllegalArgumentException("결제 상태가 PAID가 아닙니다. 확정할 수 없습니다.");
        }
        log.debug("Payments 상태 검증 통과. 현재 상태: PAID");


        // 🚨 [핵심 변경] 2. Ticket 상태 검증: TicketServiceApi 사용 및 Enum 비교
        Long ticketId = deal.getTicketId();
        log.info("티켓 상태 검증 시작. 연관 Ticket ID: {}", ticketId);

        // 2-1. TicketServiceApi를 통해 상세 정보 조회
        TicketResponse ticketResponse = ticketServiceApi.getTicketById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("연관된 티켓 정보를 찾을 수 없습니다. (ID: " + ticketId + ")"));

        log.debug("Ticket Service API 조회 성공. Ticket ID: {}, Current Status: {}",
                ticketId, ticketResponse.getTicketStatus());


        // 2-2. TicketResponse에서 상태를 TicketStatus Enum으로 가져와 검증
        TicketStatus currentTicketStatus = ticketResponse.getTicketStatus();
        if (currentTicketStatus != TicketStatus.SOLD) {
            log.warn("Ticket 상태 검증 실패: 현재 상태 ({})는 SOLD가 아닙니다.", currentTicketStatus.name());
            throw new IllegalArgumentException("티켓 상태가 SOLD가 아닙니다. 확정할 수 없습니다. 현재 상태: " + currentTicketStatus.name());
        }
        log.debug("Ticket 상태 검증 통과. 현재 상태: SOLD");


        // 3. 상태 변경 (핵심 로직)
        log.info("DB/API 상태 변경 시작. Ticket ID: {}, Deal ID: {}", ticketId, dealId);

        // A. Ticket 상태 변경: SOLD -> USED (TicketServiceApi 호출)
        String newTicketStatus = TicketStatus.USED.name(); // "USED"
        ticketServiceApi.updateTicketStatus(ticketId, "USED", accessToken);
        log.info("Ticket Service API 호출 완료. Ticket ID {} 상태를 {}로 변경 요청됨.", ticketId, newTicketStatus);


        // B. Deal 상태 변경: PAID -> COMPLETED
        deal.setDealStatus(DealStatus.COMPLETED);
        log.info("Deal 엔티티 상태 변경 완료. Deal ID {} 상태를 COMPLETED로 설정.", dealId);


        log.info("[END] 구매 확정 프로세스 성공적으로 완료. Deal ID: {}", dealId);
    }
}


