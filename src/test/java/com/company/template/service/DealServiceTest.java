package com.company.template.service;

import com.company.template.dto.DealRequest;
import com.company.template.entity.Deal;
import com.company.template.entity.DealStatus;
import com.company.template.entity.Ticket;
import com.company.template.entity.TicketStatus;
import com.company.template.repository.DealRepository;
import com.company.template.repository.TicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
public class DealServiceTest {

    @Autowired private DealService dealService;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private DealRepository dealRepository;

    private final Long TEST_SELLER_ID = 100L;
    private final Long TEST_BUYER_ID = 500L;

    // 🚨 MySQL에 삽입된 '아이유 콘서트' (AVAILABLE) 티켓의 실제 ID로 변경해야 합니다.
    private final Long AVAILABLE_TICKET_ID = 1L;
    // 🚨 MySQL에 삽입된 '뮤지컬 위키드' (RESERVED) 티켓의 실제 ID로 변경해야 합니다.
    private final Long RESERVED_TICKET_ID = 2L;


    // ====================================================================
    // 1. 성공 케이스 테스트 (Deal 요청 및 DB 상태 변경 검증)
    // ====================================================================
    @Test
    @Rollback(false)
    @DisplayName("성공: AVAILABLE 티켓에 대한 요청은 Deal을 생성하고 Ticket 상태를 DEALING으로 변경해야 한다.")
    void createDealRequest_Success_DB_Verification() {
        // GIVEN
        // 1-1. 시작 상태 검증: 티켓이 AVAILABLE 상태인지 DB에서 확인
        Ticket initialTicket = ticketRepository.findById(AVAILABLE_TICKET_ID)
                .orElseThrow(() -> new AssertionError("테스트를 위한 AVAILABLE 티켓 ID를 DB에서 찾을 수 없습니다."));
        assertThat(initialTicket.getStatus()).isEqualTo(TicketStatus.AVAILABLE);

        // 1-2. Deal 요청 DTO 준비 (구매자 ID 500L이 요청)
        DealRequest request = DealRequest.builder()
                .ticketId(AVAILABLE_TICKET_ID)
                .quantity(1)
                .expireAt(LocalDateTime.now().plusHours(1))
                .build();

        // WHEN
        // DealService 호출 -> DB 변경 발생 (Deal INSERT, Ticket UPDATE)
        dealService.createDealRequest(request, TEST_BUYER_ID);

        // THEN
        // 1. Deal 레코드 생성 확인 (가장 중요)
        // findByTicketIdAndBuyerId를 사용하여 특정 Deal이 생성되었는지 검증
        Optional<Deal> createdDeal = dealRepository.findByTicketIdAndBuyerId(AVAILABLE_TICKET_ID, TEST_BUYER_ID);

        // Deal이 존재해야 하며, 상태는 PENDING이어야 합니다.
        assertThat(createdDeal).isPresent();
        assertThat(createdDeal.get().getSellerId()).isEqualTo(TEST_SELLER_ID); // 판매자 ID 확인
        assertThat(createdDeal.get().getDealStatus()).isEqualTo(DealStatus.PENDING); // 상태 확인

        // 2. Ticket 상태 변경 확인
        // 티켓을 다시 조회하여 상태가 DEALING으로 바뀌었는지 검증
        Ticket updatedTicket = ticketRepository.findById(AVAILABLE_TICKET_ID)
                .orElseThrow();
        assertThat(updatedTicket.getStatus()).isEqualTo(TicketStatus.RESERVED);

        // 3. (추가) Deal ID가 정상적으로 생성되었는지 확인
        assertThat(createdDeal.get().getDealId()).isNotNull();
    }

    // ... (실패 케이스 테스트는 생략)
}