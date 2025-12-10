package com.company.trade.service;

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
}