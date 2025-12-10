package com.company.trade.repository;

import com.company.trade.entity.Payments;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentsRepository extends JpaRepository<Payments, Long> {
    // 필요한 쿼리 메서드가 있다면 여기에 추가합니다.
    // 예: Optional<Payments> findByDealId(Long dealId);
}
