package com.company.trade.repository;

import com.company.trade.entity.Deal;
import com.company.trade.entity.DealStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DealRepository extends JpaRepository<Deal, Long> {

    /**
     * 특정 Ticket ID와 특정 Buyer ID를 가진 Deal 레코드를 찾습니다.
     * 필드 이름: TicketId와 BuyerId를 And 조건으로 결합
     */
    Optional<Deal> findByTicketIdAndBuyerId(Long ticketId, Long buyerId);

    // 💡 참고: 만약 Deal 엔티티 필드 이름이 ticketId가 아니라 targetTicketId 등이었다면
    // 메서드 이름은 findByTargetTicketIdAndBuyerId로 변경해야 합니다.
}