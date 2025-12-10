package com.company.trade.service;

import com.company.trade.dto.DealDetailResponse;
import com.company.trade.dto.DealRequest;
import com.company.trade.dto.DealResponse;
import com.company.trade.entity.Deal;
import com.company.trade.entity.DealStatus;
import com.company.trade.entity.Ticket;
import com.company.trade.entity.TicketStatus;
import com.company.trade.repository.DealRepository;
import com.company.trade.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
@RequiredArgsConstructor
public class DealService {

    private final TicketRepository ticketRepository;
    private final DealRepository dealRepository;

    /**
     * 구매자 주도 양도 요청 생성 로직
     * * @param request 클라이언트가 전송한 요청 DTO (ticketId, quantity, expireAt 포함)
     * @param buyerId 인증된 구매자(로그인 사용자) ID
     * @return 생성된 Deal 정보를 담은 응답 DTO
     */
    @Transactional
    public DealResponse createDealRequest(DealRequest request, Long buyerId) {

        // 1. 티켓 조회 및 유효성 검증
        Ticket ticket = ticketRepository.findById(request.getTicketId())
                .orElseThrow(() -> new EntityNotFoundException("요청된 티켓을 찾을 수 없습니다. (ID: " + request.getTicketId() + ")"));

        // 1-1. 자가 구매 방지: 요청자(구매자)가 티켓 소유자(판매자) 본인인지 확인
        if (ticket.getOwnerId().equals(buyerId)) {
            throw new DealCreationException("티켓 소유자는 해당 티켓에 대해 거래 요청을 생성할 수 없습니다.");
        }

        // 1-2. 티켓 상태 검증: 현재 거래 가능한 상태인지 확인
        // 주의: 현재 코드에는 TicketStatus.AVAILABLE이 두 번 반복되어 있습니다.
        // 다른 상태(LISTED 등)가 있다면 추가하고, 중복을 제거해야 합니다.
        if (ticket.getStatus() != TicketStatus.AVAILABLE /* && ticket.getStatus() != TicketStatus.LISTED */) {
            throw new IllegalStateException("현재 티켓은 거래 요청을 받을 수 없습니다. (현재 상태: " + ticket.getStatus() + ")");
        }

        // 1-3. (재고 검증 로직 추가 예정)
        // if (request.getQuantity() > ticket.getRemainingQuantity()) { ... }

        // 2. 티켓 상태 변경 (AVAILABLE -> RESERVED)
        // 티켓을 거래 요청 상태로 묶어둡니다.
        ticket.setStatus(TicketStatus.RESERVED);
        // @Transactional에 의해 변경 감지(Dirty Checking)로 자동 저장

        // 3. Deal 엔티티 생성 및 저장
        Deal deal = Deal.builder()
                .ticketId(request.getTicketId()) // DTO에서 바로 가져옵니다.
                .sellerId(ticket.getOwnerId())  // 티켓의 소유자 ID를 판매자 ID로 설정
                .buyerId(buyerId)               // 로그인한 사용자 ID를 구매자 ID로 설정
                .quantity(request.getQuantity())
                .dealAt(LocalDateTime.now())
                .expireAt(request.getExpireAt())
                .dealStatus(DealStatus.PENDING) // 초기 상태는 PENDING
                .cancelReason(null)
                .build();

        Deal savedDeal = dealRepository.save(deal);

        // 4. 응답 DTO 변환 및 반환
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
        deal.setDealStatus(DealStatus.CANCELED);
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

        // 3. Deal 상태 변경: PENDING -> ACCEPTED
        deal.setDealStatus(DealStatus.ACCEPTED);
        deal.setDealAt(LocalDateTime.now()); // 거래 확정 시간 기록
        dealRepository.save(deal);

        // 4. Ticket 상태 변경: (생략)
        // 티켓 상태는 RESERVED를 유지하고, 실제 결제가 확인되었을 때 SOLD로 변경합니다.

        // 이 시점에서 해당 티켓이 다른 PENDING Deal이 있다면 모두 REJECTED 처리하는 로직을 추가할 수 있지만,
        // 지금은 하나의 PENDING Deal만 존재한다고 가정하고 넘어갑니다.
    }
}

