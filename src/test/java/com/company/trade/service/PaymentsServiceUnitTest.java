package com.company.trade.service;

import com.company.trade.dto.PaymentsDetailResponse;
import com.company.trade.dto.PaymentsResponse;
import com.company.trade.dto.TicketResponse;
import com.company.trade.entity.Deal;
import com.company.trade.entity.DealStatus;
import com.company.trade.entity.Payments;
import com.company.trade.entity.PaymentsStatus;
import com.company.trade.entity.TicketStatus;
import com.company.trade.repository.DealRepository;
import com.company.trade.repository.PaymentsRepository;
import com.company.trade.repository.TicketRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
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
@DisplayName("PaymentsService 단위 테스트")
class PaymentsServiceUnitTest {

    @Mock
    private PaymentsRepository paymentsRepository;

    @Mock
    private DealRepository dealRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketServiceApi ticketServiceApi;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ServletRequestAttributes servletRequestAttributes;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private PaymentsService paymentsService;

    private Long testPaymentId;
    private Long testDealId;
    private Long testBuyerId;
    private Long testSellerId;
    private Long testTicketId;
    private Deal testDeal;
    private Payments testPayments;
    private TicketResponse testTicketResponse;

    @BeforeEach
    void setUp() {
        testPaymentId = 1L;
        testDealId = 1L;
        testBuyerId = 100L;
        testSellerId = 200L;
        testTicketId = 1L;

        testDeal = Deal.builder()
                .dealId(testDealId)
                .ticketId(testTicketId)
                .buyerId(testBuyerId)
                .sellerId(testSellerId)
                .quantity(1)
                .dealStatus(DealStatus.ACCEPTED)
                .dealAt(LocalDateTime.now())
                .expireAt(LocalDateTime.now().plusHours(1))
                .build();

        testPayments = Payments.builder()
                .paymentId(testPaymentId)
                .dealId(testDealId)
                .buyerId(testBuyerId)
                .sellerId(testSellerId)
                .price(BigDecimal.valueOf(50000))
                .paymentStatus(PaymentsStatus.PENDING)
                .paymentDate(LocalDateTime.now())
                .paymentMethod("METHOD_PENDING")
                .build();

        testTicketResponse = TicketResponse.builder()
                .ticketId(testTicketId)
                .ownerId(testSellerId)
                .ticketStatus(TicketStatus.RESERVED)
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
    @DisplayName("성공: 결제 생성")
    void createPayment_Success() {
        // GIVEN
        BigDecimal amount = BigDecimal.valueOf(50000);
        when(paymentsRepository.save(any(Payments.class))).thenReturn(testPayments);

        // WHEN
        Payments result = paymentsService.createPayment(testDeal, amount);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getPaymentId()).isEqualTo(testPaymentId);
        assertThat(result.getDealId()).isEqualTo(testDealId);
        assertThat(result.getBuyerId()).isEqualTo(testBuyerId);
        assertThat(result.getPrice()).isEqualTo(amount);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentsStatus.PENDING);

        verify(paymentsRepository).save(any(Payments.class));
    }

    @Test
    @DisplayName("성공: 결제 상세 조회")
    void getPaymentDetails_Success() {
        // GIVEN
        when(paymentsRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayments));
        when(dealRepository.findById(testDealId)).thenReturn(Optional.of(testDeal));
        when(ticketServiceApi.getTicketById(testTicketId))
                .thenReturn(Optional.of(testTicketResponse));

        // WHEN
        PaymentsDetailResponse result = paymentsService.getPaymentDetails(testPaymentId, testBuyerId);

        // THEN
        assertThat(result).isNotNull();
        verify(paymentsRepository).findById(testPaymentId);
        verify(dealRepository).findById(testDealId);
        verify(ticketServiceApi).getTicketById(testTicketId);
    }

    @Test
    @DisplayName("실패: 결제 정보를 찾을 수 없는 경우")
    void getPaymentDetails_Fail_PaymentNotFound() {
        // GIVEN
        when(paymentsRepository.findById(testPaymentId)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> paymentsService.getPaymentDetails(testPaymentId, testBuyerId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("결제 정보를 찾을 수 없습니다");

        verify(paymentsRepository).findById(testPaymentId);
    }

    @Test
    @DisplayName("실패: 권한이 없는 경우")
    void getPaymentDetails_Fail_Unauthorized() {
        // GIVEN
        Long otherBuyerId = 999L;
        when(paymentsRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayments));

        // WHEN & THEN
        assertThatThrownBy(() -> paymentsService.getPaymentDetails(testPaymentId, otherBuyerId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("해당 결제 정보를 조회할 권한이 없습니다");

        verify(paymentsRepository).findById(testPaymentId);
    }

    @Test
    @DisplayName("성공: 결제 상태 업데이트 - PENDING -> PAID")
    void updatePaymentsStatus_Success_PendingToPaid() {
        // GIVEN
        when(paymentsRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayments));

        // WHEN
        PaymentsResponse response = paymentsService.updatePaymentsStatus(testPaymentId, "PAID");

        // THEN
        assertThat(response).isNotNull();
        assertThat(testPayments.getPaymentStatus()).isEqualTo(PaymentsStatus.PAID);
        verify(paymentsRepository).findById(testPaymentId);
    }

    @Test
    @DisplayName("성공: 결제 상태 업데이트 - PENDING -> FAILED")
    void updatePaymentsStatus_Success_PendingToFailed() {
        // GIVEN
        when(paymentsRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayments));

        // WHEN
        PaymentsResponse response = paymentsService.updatePaymentsStatus(testPaymentId, "FAILED");

        // THEN
        assertThat(response).isNotNull();
        assertThat(testPayments.getPaymentStatus()).isEqualTo(PaymentsStatus.FAILED);
    }

    @Test
    @DisplayName("성공: 결제 상태 업데이트 - PAID -> CANCELLED")
    void updatePaymentsStatus_Success_PaidToCancelled() {
        // GIVEN
        testPayments.setPaymentStatus(PaymentsStatus.PAID);
        when(paymentsRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayments));

        // WHEN
        PaymentsResponse response = paymentsService.updatePaymentsStatus(testPaymentId, "CANCELLED");

        // THEN
        assertThat(response).isNotNull();
        assertThat(testPayments.getPaymentStatus()).isEqualTo(PaymentsStatus.CANCELLED);
    }

    @Test
    @DisplayName("실패: 유효하지 않은 상태 값")
    void updatePaymentsStatus_Fail_InvalidStatus() {
        // GIVEN
        // IllegalArgumentException이 먼저 발생하므로 paymentsRepository.findById는 호출되지 않음
        // 따라서 stubbing을 제거해야 함

        // WHEN & THEN
        assertThatThrownBy(() -> paymentsService.updatePaymentsStatus(testPaymentId, "INVALID_STATUS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 결제 상태 값");
    }

    @Test
    @DisplayName("실패: 상태 전이 규칙 위반 - PENDING -> CANCELLED는 가능하지만 PAID -> FAILED는 불가능")
    void updatePaymentsStatus_Fail_InvalidTransition() {
        // GIVEN
        testPayments.setPaymentStatus(PaymentsStatus.PAID);
        when(paymentsRepository.findById(testPaymentId)).thenReturn(Optional.of(testPayments));

        // WHEN & THEN
        assertThatThrownBy(() -> paymentsService.updatePaymentsStatus(testPaymentId, "FAILED"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("현재 상태 (PAID)에서는 FAILED 상태로 변경할 수 없습니다");
    }

    @Test
    @DisplayName("실패: 결제 정보를 찾을 수 없는 경우")
    void updatePaymentsStatus_Fail_PaymentNotFound() {
        // GIVEN
        when(paymentsRepository.findById(testPaymentId)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> paymentsService.updatePaymentsStatus(testPaymentId, "PAID"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ID " + testPaymentId + "인 결제(Payments)를 찾을 수 없습니다");
    }
}

