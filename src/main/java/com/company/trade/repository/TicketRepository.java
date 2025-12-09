package com.company.trade.repository;


import com.company.trade.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * TicketRepository 인터페이스
 *
 * JpaRepository를 상속하여 기본적인 CRUD 기능을 자동으로 제공받습니다.
 * service-trade 모듈에서는 주로 findById (티켓 유효성/소유권 검증) 및 save (상태 변경)에 사용됩니다.
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // JpaRepository를 상속하면 다음 메서드가 자동으로 제공됩니다.
    // - Optional<Ticket> findById(Long id); // 티켓 조회
    // - <S extends Ticket> S save(S entity); // 티켓 저장 및 업데이트 (상태 변경 포함)
    // - List<Ticket> findAll();
    // ...

    // 만약 상태(TicketStatus)를 기준으로 티켓을 조회하는 기능이 필요하다면 추가할 수 있습니다.
    // List<Ticket> findByStatus(TicketStatus status);
}