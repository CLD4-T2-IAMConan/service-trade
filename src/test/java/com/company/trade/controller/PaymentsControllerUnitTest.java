package com.company.trade.controller;

import com.company.trade.dto.*;
import com.company.trade.service.PaymentsService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.view.RedirectView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentsController 단위 테스트")
class PaymentsControllerUnitTest {

    @Mock
    private PaymentsService paymentsService;

    @InjectMocks
    private PaymentsController paymentsController;

    private Long testPaymentId;
    private Long testBuyerId;
    private PaymentsDetailResponse paymentsDetailResponse;
    private NicepayPrepareResponse nicepayPrepareResponse;

    @BeforeEach
    void setUp() {
        testPaymentId = 1L;
        testBuyerId = 100L;

        paymentsDetailResponse = PaymentsDetailResponse.builder()
                .build();

        nicepayPrepareResponse = NicepayPrepareResponse.builder()
                .clientId("test-client-id")
                .orderId("ORDER_1")
                .amount(50000L)
                .goodsName("테스트 콘서트")
                .returnUrl("http://localhost:8083/api/payments/nicepay/callback")
                .paymentId("1")
                .build();
    }

    @Test
    @DisplayName("성공: 결제 상세 조회")
    void getPaymentDetails_Success() {
        // GIVEN
        when(paymentsService.getPaymentDetails(testPaymentId, testBuyerId))
                .thenReturn(paymentsDetailResponse);

        // WHEN
        ResponseEntity<?> response = paymentsController.getPaymentDetails(testPaymentId, testBuyerId);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(PaymentsDetailResponse.class);
        verify(paymentsService).getPaymentDetails(testPaymentId, testBuyerId);
    }

    @Test
    @DisplayName("실패: 결제 상세 조회 - EntityNotFoundException")
    void getPaymentDetails_Fail_NotFound() {
        // GIVEN
        when(paymentsService.getPaymentDetails(testPaymentId, testBuyerId))
                .thenThrow(new RuntimeException("결제 정보를 찾을 수 없습니다"));

        // WHEN & THEN
        assertThatThrownBy(() -> paymentsController.getPaymentDetails(testPaymentId, testBuyerId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("결제 정보를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("성공: 결제 준비")
    void preparePayment_Success() {
        // GIVEN
        when(paymentsService.preparePayment(testPaymentId, testBuyerId))
                .thenReturn(nicepayPrepareResponse);

        // WHEN
        ResponseEntity<?> response = paymentsController.preparePayment(testPaymentId, testBuyerId);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(NicepayPrepareResponse.class);
        verify(paymentsService).preparePayment(testPaymentId, testBuyerId);
    }

    @Test
    @DisplayName("실패: 결제 준비 - EntityNotFoundException")
    void preparePayment_Fail_NotFound() {
        // GIVEN
        when(paymentsService.preparePayment(testPaymentId, testBuyerId))
                .thenThrow(new EntityNotFoundException("결제 정보를 찾을 수 없습니다"));

        // WHEN
        ResponseEntity<?> response = paymentsController.preparePayment(testPaymentId, testBuyerId);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("성공: NICEPAY 콜백 처리")
    void nicepayCallback_Success() {
        // GIVEN
        NicepayCallbackRequest request = new NicepayCallbackRequest();
        request.setOrderId("ORDER_1");
        request.setTid("test-tid");
        request.setAuthToken("test-auth-token");

        // WHEN
        RedirectView redirectView = paymentsController.nicepayCallback(request);

        // THEN
        assertThat(redirectView).isNotNull();
        assertThat(redirectView.getUrl()).contains("payments/1/result");
    }

    @Test
    @DisplayName("성공: 결제 완료")
    void completePayment_Success() throws Exception {
        // GIVEN
        String tid = "test-tid";
        String authToken = "test-auth-token";
        doNothing().when(paymentsService).completePayment(anyString(), anyString(), anyString());

        // WHEN
        ResponseEntity<String> response = paymentsController.completePayment(
                String.valueOf(testPaymentId), tid, authToken);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("PAYMENT_APPROVAL_SUCCESS");
        verify(paymentsService).completePayment(tid, authToken, "ORDER_" + testPaymentId);
    }

    @Test
    @DisplayName("실패: 결제 완료 - 예외 발생")
    void completePayment_Fail_Exception() throws Exception {
        // GIVEN
        String tid = "test-tid";
        String authToken = "test-auth-token";
        doThrow(new RuntimeException("결제 승인 실패"))
                .when(paymentsService).completePayment(anyString(), anyString(), anyString());

        // WHEN
        ResponseEntity<String> response = paymentsController.completePayment(
                String.valueOf(testPaymentId), tid, authToken);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains("서버 오류");
    }
}

