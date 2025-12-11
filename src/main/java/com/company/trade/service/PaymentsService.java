package com.company.trade.service;

import com.company.trade.dto.PaymentsDetailResponse;
import com.company.trade.dto.PaymentsCompleteRequest;
import com.company.trade.entity.*;
import com.company.trade.repository.PaymentsRepository;
import com.company.trade.repository.DealRepository;
import com.company.trade.repository.TicketRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Custom Runtime Exceptions (DealService에서 정의된 것을 재사용한다고 가정)
// class EntityNotFoundException extends RuntimeException { /* ... */ }
// class IllegalStateException extends RuntimeException { /* ... */ }

@Service
@RequiredArgsConstructor
public class PaymentsService {

    private final PaymentsRepository paymentsRepository;
    private final DealRepository dealRepository;
    private final TicketRepository ticketRepository;

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

    /**
     * [POST] 결제 시스템에서 결제 완료 통보를 받은 후, 최종 상태를 처리합니다.
     * 이 메서드는 Payments 상태를 COMPLETED로, 연결된 Deal 상태도 COMPLETED로 변경합니다.
     * @param paymentsId 처리할 Payments ID
     * @param request 결제 수단, 거래 ID 등 포함 (결제 검증 로직은 생략)
     */
    @Transactional
    public void completePayment(Long paymentsId, PaymentsCompleteRequest request) {

        // 1. Payments 엔티티 조회
        Payments payments = paymentsRepository.findById(paymentsId)
                .orElseThrow(() -> new EntityNotFoundException("처리할 결제 정보를 찾을 수 없습니다. (ID: " + paymentsId + ")"));

        // 2. 상태 검증: PENDING 상태일 때만 완료 처리 가능
        if (payments.getPaymentStatus() != PaymentsStatus.PENDING) {
            throw new IllegalStateException("현재 결제 상태(" + payments.getPaymentStatus() + ")에서는 완료 처리할 수 없습니다.");
        }

        // 3. Payments 상태 변경: PENDING -> COMPLETED
        payments.setPaymentStatus(PaymentsStatus.PAID);
        payments.setPaymentMethod(request.getPaymentMethod());
        paymentsRepository.save(payments); // 명시적 저장

        // 4. 연결된 Deal 엔티티 조회 및 상태 변경: ACCEPTED -> COMPLETED
        Deal deal = dealRepository.findById(payments.getDealId())
                .orElseThrow(() -> new EntityNotFoundException("연결된 거래(Deal)를 찾을 수 없습니다."));

        // 4-1. Deal 상태 검증: ACCEPTED 상태일 때만 COMPLETED 처리 가능
        if (deal.getDealStatus() != DealStatus.ACCEPTED) {
            throw new IllegalStateException("연결된 거래 상태(" + deal.getDealStatus() + ")가 수락(ACCEPTED) 상태가 아닙니다.");
        }

        deal.setDealStatus(DealStatus.COMPLETED);
        dealRepository.save(deal); // 명시적 저장

        // 5. 연결된 Ticket 상태 변경 (DealService.acceptDeal에서 이미 SOLD로 변경했으므로, 재검증만)
        Ticket ticket = ticketRepository.findById(deal.getTicketId())
                .orElseThrow(() -> new EntityNotFoundException("연결된 티켓을 찾을 수 없습니다."));

        // 💡 중요: Ticket이 SOLD 상태가 아니라면 데이터 정합성 오류이므로 예외 처리 (선택적)
        if (ticket.getStatus() != TicketStatus.SOLD) {
            // 이 상황은 이전에 DealService.acceptDeal에서 오류가 났음을 의미
            // throw new IllegalStateException("티켓 상태가 SOLD가 아닙니다. 데이터 정합성 오류.");
        }
    }

    // 참고: DealService에 구현되어야 할 public void cancelDeal(Long dealId, Long buyerId)는 PaymentsService에 포함하지 않습니다.
}