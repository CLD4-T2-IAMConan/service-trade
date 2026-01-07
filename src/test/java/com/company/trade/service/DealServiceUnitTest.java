package com.company.trade.service;

import com.company.sns.SnsEventPublisher;
import com.company.trade.dto.DealRequest;
import com.company.trade.dto.DealResponse;
import com.company.trade.dto.TicketResponse;
import com.company.trade.entity.Deal;
import com.company.trade.entity.DealStatus;
import com.company.trade.entity.TicketStatus;
import com.company.trade.repository.DealRepository;
import com.company.trade.repository.PaymentsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("DealService 단위 테스트")
class DealServiceUnitTest {

    @Mock
    private TicketServiceApi ticketServiceApi;

    @Mock
    private DealRepository dealRepository;

    @Mock
    private PaymentsRepository paymentsRepository;

    @Mock
    private PaymentsService paymentsService;

    @Mock
    private SnsEventPublisher eventPublisher;

    @Mock
    private ServletRequestAttributes servletRequestAttributes;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private DealService dealService;

    private Long testTicketId;
    private Long testBuyerId;
    private Long testSellerId;
    private DealRequest dealRequest;
    private TicketResponse ticketResponse;

    @BeforeEach
    void setUp() {
        testTicketId = 1L;
        testBuyerId = 100L;
        testSellerId = 200L;

        dealRequest = DealRequest.builder()
                .ticketId(testTicketId)
                .quantity(1)
                .expireAt(LocalDateTime.now().plusHours(1))
                .build();

        ticketResponse = TicketResponse.builder()
                .ticketId(testTicketId)
                .ownerId(testSellerId)
                .ticketStatus(TicketStatus.AVAILABLE)
                .sellingPrice(BigDecimal.valueOf(50000))
                .eventName("테스트 콘서트")
                .build();

        // RequestContextHolder 설정 (lenient로 설정하여 모든 테스트에서 사용되지 않아도 오류가 발생하지 않도록)
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        lenient().when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        lenient().when(httpServletRequest.getHeader(anyString())).thenReturn("Bearer test-token");
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("성공: AVAILABLE 티켓에 대한 거래 요청 생성")
    void createDealRequest_Success() {
        // GIVEN
        // RequestContextHolder 재설정 (tearDown에서 reset되었을 수 있으므로)
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        // setUp에서 lenient로 설정했지만, 테스트에서 명시적으로 재설정
        // reset 후 다시 설정하여 깨끗한 상태로 시작
        reset(servletRequestAttributes, httpServletRequest);
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader(anyString())).thenReturn("Bearer test-token");
        
        when(ticketServiceApi.getTicketById(testTicketId))
                .thenReturn(Optional.of(ticketResponse));
        doNothing().when(ticketServiceApi).updateTicketStatus(any(), anyString(), anyString());

        Deal savedDeal = Deal.builder()
                .dealId(1L)
                .ticketId(testTicketId)
                .buyerId(testBuyerId)
                .sellerId(testSellerId)
                .quantity(1)
                .expireAt(dealRequest.getExpireAt())
                .dealStatus(DealStatus.PENDING)
                .dealAt(LocalDateTime.now())
                .build();

        when(dealRepository.save(any(Deal.class))).thenReturn(savedDeal);

        // WHEN
        DealResponse response = dealService.createDealRequest(dealRequest, testBuyerId);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.getDealId()).isEqualTo(1L);
        assertThat(response.getTicketId()).isEqualTo(testTicketId);
        assertThat(response.getBuyerId()).isEqualTo(testBuyerId);
        assertThat(response.getSellerId()).isEqualTo(testSellerId);
        assertThat(response.getDealStatus()).isEqualTo(DealStatus.PENDING);

        verify(ticketServiceApi).getTicketById(testTicketId);
        verify(ticketServiceApi).updateTicketStatus(eq(testTicketId), eq(TicketStatus.RESERVED.name()), anyString());
        verify(dealRepository).save(any(Deal.class));
    }

    @Test
    @DisplayName("실패: 티켓을 찾을 수 없는 경우")
    void createDealRequest_Fail_TicketNotFound() {
        // GIVEN
        when(ticketServiceApi.getTicketById(testTicketId))
                .thenReturn(Optional.empty());

        // WHEN & THEN
        // Optional.empty()일 때 orElseThrow()가 EntityNotFoundException을 던지고,
        // 이것이 catch 블록에서 RuntimeException으로 변환됩니다
        assertThatThrownBy(() -> dealService.createDealRequest(dealRequest, testBuyerId))
                .isInstanceOf(RuntimeException.class);
        
        // 예외 메시지는 두 가지 중 하나일 수 있습니다:
        // 1. "요청된 티켓을 찾을 수 없습니다. (ID: ...)" - EntityNotFoundException이 직접 던져질 때
        // 2. "티켓 정보 조회 중 연결 오류 발생." - catch 블록에서 변환될 때

        verify(ticketServiceApi).getTicketById(testTicketId);
        verify(dealRepository, never()).save(any());
    }

    @Test
    @DisplayName("실패: 티켓 상태가 AVAILABLE이 아닌 경우")
    void createDealRequest_Fail_TicketNotAvailable() {
        // GIVEN
        ticketResponse.setTicketStatus(TicketStatus.RESERVED);
        when(ticketServiceApi.getTicketById(testTicketId))
                .thenReturn(Optional.of(ticketResponse));

        // WHEN & THEN
        assertThatThrownBy(() -> dealService.createDealRequest(dealRequest, testBuyerId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("현재 티켓은 거래 요청을 받을 수 없습니다");

        verify(ticketServiceApi).getTicketById(testTicketId);
        verify(ticketServiceApi, never()).updateTicketStatus(any(), anyString(), anyString());
        verify(dealRepository, never()).save(any());
    }

    @Test
    @DisplayName("성공: 거래 상세 조회")
    void getDealDetail_Success() {
        // GIVEN
        Long dealId = 1L;
        Deal deal = Deal.builder()
                .dealId(dealId)
                .ticketId(testTicketId)
                .buyerId(testBuyerId)
                .sellerId(testSellerId)
                .quantity(1)
                .dealStatus(DealStatus.PENDING)
                .dealAt(LocalDateTime.now())
                .expireAt(LocalDateTime.now().plusHours(1))
                .build();

        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
        when(ticketServiceApi.getTicketById(testTicketId))
                .thenReturn(Optional.of(ticketResponse));

        // WHEN
        var result = dealService.getDealDetail(dealId);

        // THEN
        assertThat(result).isNotNull();
        verify(dealRepository).findById(dealId);
        verify(ticketServiceApi).getTicketById(testTicketId);
    }

    @Test
    @DisplayName("실패: 거래를 찾을 수 없는 경우")
    void getDealDetail_Fail_DealNotFound() {
        // GIVEN
        Long dealId = 999L;
        when(dealRepository.findById(dealId)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> dealService.getDealDetail(dealId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("거래 ID " + dealId + "번을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("성공: 거래 상태 업데이트 - PENDING -> ACCEPTED")
    void updateDealStatus_Success_PendingToAccepted() {
        // GIVEN
        Long dealId = 1L;
        Deal deal = Deal.builder()
                .dealId(dealId)
                .ticketId(testTicketId)
                .buyerId(testBuyerId)
                .sellerId(testSellerId)
                .quantity(1)
                .dealStatus(DealStatus.PENDING)
                .dealAt(LocalDateTime.now())
                .expireAt(LocalDateTime.now().plusHours(1))
                .build();

        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));

        // WHEN
        DealResponse response = dealService.updateDealStatus(dealId, "ACCEPTED");

        // THEN
        assertThat(response).isNotNull();
        assertThat(deal.getDealStatus()).isEqualTo(DealStatus.ACCEPTED);
        verify(dealRepository).findById(dealId);
    }

    @Test
    @DisplayName("실패: 유효하지 않은 상태 값")
    void updateDealStatus_Fail_InvalidStatus() {
        // GIVEN
        Long dealId = 1L;
        // IllegalArgumentException이 먼저 발생하므로 dealRepository.findById는 호출되지 않음
        // 따라서 stubbing을 lenient로 설정하거나 제거해야 함

        // WHEN & THEN
        assertThatThrownBy(() -> dealService.updateDealStatus(dealId, "INVALID_STATUS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 거래 상태 값");
    }

    @Test
    @DisplayName("실패: 상태 전이 규칙 위반 - PENDING -> PAID")
    void updateDealStatus_Fail_InvalidTransition() {
        // GIVEN
        Long dealId = 1L;
        Deal deal = Deal.builder()
                .dealId(dealId)
                .dealStatus(DealStatus.PENDING)
                .build();

        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));

        // WHEN & THEN
        assertThatThrownBy(() -> dealService.updateDealStatus(dealId, "PAID"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("현재 상태 (PENDING)에서는 PAID 상태로 변경할 수 없습니다");
    }
}

