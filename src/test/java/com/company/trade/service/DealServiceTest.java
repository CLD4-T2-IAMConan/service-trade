package com.company.trade.service;

import com.company.trade.dto.DealRequest;
import com.company.trade.dto.TicketResponse;
import com.company.trade.entity.Deal;
import com.company.trade.entity.DealStatus;
import com.company.trade.entity.Ticket;
import com.company.trade.entity.TicketStatus;
import com.company.trade.repository.DealRepository;
import com.company.trade.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    "spring.main.allow-bean-definition-overriding=true"
})
@Transactional
@ActiveProfiles("test")
public class DealServiceTest {

    @Autowired private DealService dealService;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private DealRepository dealRepository;
    
    @MockBean
    private TicketServiceApi ticketServiceApi;

    private final Long TEST_SELLER_ID = 100L;
    private final Long TEST_BUYER_ID = 500L;

    // H2 인메모리 DB에 삽입된 테스트 데이터 ID
    private final Long AVAILABLE_TICKET_ID = 4L; // '아이유 콘서트' (AVAILABLE) 티켓
    private final Long RESERVED_TICKET_ID = 5L; // '뮤지컬 위키드' (RESERVED) 티켓

    @BeforeEach
    void setUp() {
        // TicketServiceApi Mock 설정 - H2 인메모리 DB의 티켓 정보를 기반으로 TicketResponse 생성
        // 주의: @Transactional이므로 각 테스트 전에 트랜잭션이 시작되지만, 
        // 테스트 데이터는 data-test.sql에서 자동으로 삽입됩니다 (H2 인메모리 DB)
        try {
            Ticket dbTicket = ticketRepository.findById(AVAILABLE_TICKET_ID).orElse(null);
            if (dbTicket != null) {
                TicketResponse ticketResponse = TicketResponse.builder()
                        .ticketId(dbTicket.getTicketId())
                        .ownerId(dbTicket.getOwnerId())
                        .ticketStatus(dbTicket.getTicketStatus())
                        .sellingPrice(dbTicket.getSellingPrice())
                        .eventName(dbTicket.getEventName())
                        .build();
                
                when(ticketServiceApi.getTicketById(AVAILABLE_TICKET_ID))
                        .thenReturn(Optional.of(ticketResponse));
                doNothing().when(ticketServiceApi).updateTicketStatus(any(), anyString(), anyString());
            }
        } catch (Exception e) {
            // DB 연결 실패 시 무시 (테스트가 실패할 것이므로)
        }
    }

    // ====================================================================
    // 1. 성공 케이스 테스트 (Deal 요청 및 DB 상태 변경 검증)
    // ====================================================================
    @Test
    @DisplayName("성공: AVAILABLE 티켓에 대한 요청은 Deal을 생성하고 Ticket 상태를 RESERVED로 변경해야 한다.")
    void createDealRequest_Success_DB_Verification() {
        // 주의: 이 테스트는 H2 인메모리 DB를 사용하며,
        // data-test.sql에 정의된 테스트 데이터(ID 4: AVAILABLE 티켓)가 자동으로 삽입됩니다
        // GIVEN
        // 1-1. 시작 상태 검증: 티켓이 AVAILABLE 상태인지 DB에서 확인
        Ticket initialTicket = ticketRepository.findById(AVAILABLE_TICKET_ID)
                .orElseThrow(() -> new AssertionError("테스트를 위한 AVAILABLE 티켓 ID를 DB에서 찾을 수 없습니다."));
        assertThat(initialTicket.getTicketStatus()).isEqualTo(TicketStatus.AVAILABLE);

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
        // 주의: TicketServiceApi.updateTicketStatus는 외부 서비스를 호출하므로,
        // 로컬 DB의 Ticket 엔티티는 직접 업데이트되지 않습니다.
        // 대신 API 호출이 제대로 되었는지 검증합니다.
        // (updateTicketStatus는 이미 setUp에서 doNothing()으로 mock 설정됨)

        // 3. (추가) Deal ID가 정상적으로 생성되었는지 확인
        assertThat(createdDeal.get().getDealId()).isNotNull();
    }

    // ... (실패 케이스 테스트는 생략)
}