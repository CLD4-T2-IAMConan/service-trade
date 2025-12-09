package com.company.template.repository;

import com.company.template.entity.Deal;
import com.company.template.entity.DealStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface DealRepository extends JpaRepository<Deal, Long> {

    // --- DealServiceIntegrationTest에서 사용된 커스텀 쿼리 메서드 ---

    /**
     * 특정 티켓 ID와 구매자 ID에 해당하는 Deal 엔티티를 조회합니다.
     * (SELECT * FROM deal WHERE ticket_id = ? AND buyer_id = ?)
     */
    Optional<Deal> findByTicketIdAndBuyerId(Long ticketId, Long buyerId);

    /**
     * 특정 티켓 ID에 해당하는 Deal 엔티티를 조회합니다.
     * (SELECT * FROM deal WHERE ticket_id = ?)
     */
    Optional<Deal> findByTicketId(Long ticketId);

    // 이전에 정의했던 메서드 (필요 시)
    // List<Deal> findBySellerIdAndDealStatus(Long sellerId, DealStatus status);
}