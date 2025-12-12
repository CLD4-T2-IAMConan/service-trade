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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.company.trade.dto.TicketResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    /**
     * [Transactional] 새로운 거래 요청을 생성하고, 티켓 상태를 'RESERVED'로 변경합니다.
     * * @param request 거래 요청에 필요한 데이터 (ticketId, quantity, expireAt 등)
     * @param buyerId 요청을 생성한 구매자 ID
     * @return 생성된 거래 정보 DTO
     */
    @Transactional
    public DealResponse createDealRequest(DealRequest request, Long buyerId) {

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
            ticketServiceApi.updateTicketStatus(request.getTicketId(), TicketStatus.RESERVED.name());

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
        return DealResponse.from(savedDeal);
    }


    // DealService.java (추가해야 할 메서드 예시)
    public DealDetailResponse getPendingDealDetails(Long ticketId) {
        // 1. Ticket 조회
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("티켓을 찾을 수 없습니다."));

        // 2. PENDING Deal 조회
        // 💡 dealRepository에 findByTicketIdAndDealStatus(Long ticketId, DealStatus status) 메서드가 필요함
        Deal deal = dealRepository.findByTicketIdAndDealStatus(ticketId, DealStatus.PENDING)
                .orElse(null);

        // 3. DTO로 변환 및 반환
        return DealDetailResponse.from(ticket, deal);
    }

    @Transactional // 💡 두 테이블의 상태 변경이 한 트랜잭션으로 묶여야 합니다.
    public void rejectDeal(Long dealId, Long sellerId) {
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

        // 3. Deal 상태 변경: REJECTED
        deal.setDealStatus(DealStatus.REJECTED);
        // deal.setCancelReason("판매자가 요청 거절"); // 필요하다면 거절 사유 추가
        dealRepository.save(deal);

        // 4. Ticket 상태 변경: RESERVED -> AVAILABLE
        // 티켓을 조회하고 상태를 변경합니다.
        Ticket ticket = ticketRepository.findById(deal.getTicketId())
                .orElseThrow(() -> new EntityNotFoundException("연결된 티켓을 찾을 수 없습니다."));

        // 4-1. 티켓 상태 검사 (RESERVED 상태일 때만 AVAILABLE로 변경)
        if (ticket.getStatus() != TicketStatus.RESERVED) {
            // 이 예외는 이론적으로 발생해서는 안되지만, 데이터 정합성을 위해 체크합니다.
            throw new IllegalStateException("티켓 상태가 RESERVED가 아니므로 AVAILABLE로 변경할 수 없습니다.");
        }

        // 4-2. 상태 변경
        ticket.setStatus(TicketStatus.AVAILABLE);
        ticketRepository.save(ticket);
    }

    @Transactional // Transactional 어노테이션 확인
    public void acceptDeal(Long dealId, Long sellerId) {
        // 1. Deal 요청 조회
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new EntityNotFoundException("요청하신 거래(Deal)를 찾을 수 없습니다."));

        // 2. 비즈니스 유효성 검사
        // 2-1. 판매자 권한 검사
        if (!deal.getSellerId().equals(sellerId)) {
            throw new IllegalStateException("해당 거래를 수락할 권한이 없습니다.");
        }

        // 2-2. 상태 검사 (PENDING 상태일 때만 수락 가능)
        if (deal.getDealStatus() != DealStatus.PENDING) {
            throw new IllegalStateException("현재 거래 상태(" + deal.getDealStatus() + ")에서는 수락할 수 없습니다.");
        }

        // 🌟🌟🌟 💡 수정된 로직: Ticket에서 가격 정보 가져오기 🌟🌟🌟
        Ticket ticket = ticketRepository.findById(deal.getTicketId())
                .orElseThrow(() -> new EntityNotFoundException("연결된 티켓을 찾을 수 없습니다."));

        // 1. Integer 타입의 가격을 가져옴
        Integer sellingPriceInt = ticket.getSellingPrice();

        if (sellingPriceInt == null) {
            throw new IllegalStateException("티켓 가격 정보가 누락되었습니다.");
        }

        // 2. Integer를 BigDecimal로 변환
        // Integer.valueOf(0) 대신 new BigDecimal(sellingPriceInt) 또는 BigDecimal.valueOf(sellingPriceInt) 사용
        //BigDecimal dealPrice = BigDecimal.valueOf(sellingPriceInt.longValue()); // longValue()를 사용하거나
        BigDecimal dealPrice = new BigDecimal(sellingPriceInt); // 이렇게 직접 변환

        // 2. Deal 상태 변경: PENDING -> ACCEPTED (기존 로직 유지)
        deal.setDealStatus(DealStatus.ACCEPTED);
        // ... (Deal 저장 로직 유지)

        // 3. Payments 대기 데이터 생성 (수정 없음, 이제 dealPrice는 BigDecimal임)
        Payments payment = Payments.builder()
                .dealId(dealId)
                .buyerId(deal.getBuyerId())
                .sellerId(deal.getSellerId())
                .price(dealPrice) // 🌟 BigDecimal로 변환된 가격 사용
                .paymentStatus(PaymentsStatus.PENDING)
                .paymentMethod("TBD")
                .build();

        paymentsRepository.save(payment);
        // 이 시점에서 해당 티켓이 다른 PENDING Deal이 있다면 모두 REJECTED 처리하는 로직을 추가할 수 있지만,
        // 지금은 하나의 PENDING Deal만 존재한다고 가정하고 넘어갑니다.
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
        if (deal.getDealStatus() != DealStatus.ACCEPTED) {
            throw new IllegalArgumentException("거래 상태(" + deal.getDealStatus() + ")에서는 취소할 수 없습니다. (ACCEPTED 상태여야 함)");
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
}

