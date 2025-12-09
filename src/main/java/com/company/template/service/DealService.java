package com.company.template.service;

import com.company.template.dto.DealRequest;
import com.company.template.dto.DealResponse;
import com.company.template.entity.Deal;
import com.company.template.entity.DealStatus;
import com.company.template.entity.Ticket;
import com.company.template.entity.TicketStatus;
import com.company.template.repository.DealRepository;
import com.company.template.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 비즈니스 로직에서 사용할 예외 클래스 (별도 정의 필요)
class EntityNotFoundException extends RuntimeException { public EntityNotFoundException(String msg) { super(msg); } }
class IllegalStateException extends RuntimeException { public IllegalStateException(String msg) { super(msg); } }
class DealCreationException extends RuntimeException { public DealCreationException(String msg) { super(msg); } }


@Service
@RequiredArgsConstructor
public class DealService {

    private final TicketRepository ticketRepository;
    private final DealRepository dealRepository;

    /**
     * 구매자 주도 양도 요청 생성 로직
     * 1. 요청 티켓의 유효성 검증 및 판매자(Seller) ID 조회
     * 2. 요청자(Buyer)가 티켓 소유자(Seller) 본인인지 확인 (자가 구매 방지)
     * 3. 티켓 상태를 'DEALING'으로 변경 (트랜잭션에 포함)
     * 4. Deal 레코드 생성 (상태: PENDING)
     * * @param request 클라이언트가 전송한 요청 DTO
     * @param buyerId 인증된 구매자(로그인 사용자) ID
     * @return 생성된 Deal 정보를 담은 응답 DTO
     */
    @Transactional // 트랜잭션 보장
    public DealResponse createDealRequest(DealRequest request, Long buyerId) {

        // 1. 티켓 조회 및 유효성 검증
        Ticket ticket = ticketRepository.findById(request.getTicketId())
                .orElseThrow(() -> new EntityNotFoundException("요청된 티켓을 찾을 수 없습니다. (ID: " + request.getTicketId() + ")"));

        // 1-1. 자가 구매 방지: 요청자(구매자) ID와 티켓 소유자 ID 비교
        if (ticket.getOwnerId().equals(buyerId)) {
            throw new DealCreationException("티켓 소유자는 자신의 티켓에 대해 거래 요청을 생성할 수 없습니다.");
        }

        // 1-2. 티켓 상태 검증: 현재 거래 가능한 상태인지 확인
        if (ticket.getStatus() != TicketStatus.AVAILABLE) {
            throw new IllegalStateException("현재 티켓은 거래 요청을 받을 수 없습니다. (현재 상태: " + ticket.getStatus() + ")");
        }

        // 2. 티켓 상태 변경 (AVAILABLE/LISTED -> DEALING)
        // @Transactional에 의해 메서드 종료 시 DB에 자동 반영됩니다.
        ticket.setStatus(TicketStatus.RESERVED);

        // 3. Deal 엔티티 생성 및 저장
        Deal deal = Deal.builder()
                .ticketId(ticket.getTicketId())
                .sellerId(ticket.getOwnerId()) // 티켓에서 판매자 ID 추출
                .buyerId(buyerId)             // 파라미터로 받은 구매자 ID 설정
                .quantity(request.getQuantity())
                .dealAt(LocalDateTime.now())
                .expireAt(request.getExpireAt())
                .dealStatus(DealStatus.PENDING) // 초기 상태
                .cancelReason(null)
                .build();

        Deal savedDeal = dealRepository.save(deal);

        // 4. 응답 DTO 변환 및 반환
        return DealResponse.from(savedDeal);
    }
}
