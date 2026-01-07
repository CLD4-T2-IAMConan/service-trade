package com.company.trade.controller;

import com.company.trade.dto.*;
import com.company.trade.service.DealService;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DealController 단위 테스트")
class DealControllerUnitTest {

    @Mock
    private DealService dealService;

    @InjectMocks
    private DealController dealController;

    private Long testDealId;
    private Long testBuyerId;
    private Long testSellerId;
    private DealRequest dealRequest;
    private DealResponse dealResponse;

    @BeforeEach
    void setUp() {
        testDealId = 1L;
        testBuyerId = 100L;
        testSellerId = 200L;

        dealRequest = DealRequest.builder()
                .ticketId(1L)
                .quantity(1)
                .expireAt(LocalDateTime.now().plusHours(1))
                .buyerId(testBuyerId)
                .build();

        dealResponse = DealResponse.builder()
                .dealId(testDealId)
                .ticketId(1L)
                .buyerId(testBuyerId)
                .sellerId(testSellerId)
                .quantity(1)
                .dealAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("성공: 거래 요청 생성")
    void createDealRequest_Success() {
        // GIVEN
        when(dealService.createDealRequest(any(DealRequest.class), anyLong()))
                .thenReturn(dealResponse);

        // WHEN
        ResponseEntity<?> response = dealController.createDealRequest(dealRequest);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isInstanceOf(DealResponse.class);
        verify(dealService).createDealRequest(any(DealRequest.class), eq(testBuyerId));
    }

    @Test
    @DisplayName("실패: 거래 요청 생성 실패 - RuntimeException")
    void createDealRequest_Fail_RuntimeException() {
        // GIVEN
        when(dealService.createDealRequest(any(DealRequest.class), anyLong()))
                .thenThrow(new RuntimeException("거래 요청 실패"));

        // WHEN
        ResponseEntity<?> response = dealController.createDealRequest(dealRequest);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(dealService).createDealRequest(any(DealRequest.class), eq(testBuyerId));
    }

    @Test
    @DisplayName("성공: 거래 상세 조회")
    void getDealDetail_Success() {
        // GIVEN
        DealDetailResponse detailResponse = DealDetailResponse.builder()
                .dealId(testDealId)
                .build();

        when(dealService.getDealDetail(testDealId)).thenReturn(detailResponse);

        // WHEN
        ResponseEntity<ApiResponse<DealDetailResponse>> response =
                dealController.getDealDetail(testDealId);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        verify(dealService).getDealDetail(testDealId);
    }

    @Test
    @DisplayName("실패: 거래 상세 조회 - EntityNotFoundException")
    void getDealDetail_Fail_NotFound() {
        // GIVEN
        when(dealService.getDealDetail(testDealId))
                .thenThrow(new EntityNotFoundException("거래를 찾을 수 없습니다"));

        // WHEN
        ResponseEntity<ApiResponse<DealDetailResponse>> response =
                dealController.getDealDetail(testDealId);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    @Test
    @DisplayName("성공: 거래 거절")
    void rejectDealRequest_Success() {
        // GIVEN
        DealRejectRequest request = new DealRejectRequest();
        request.setCurrentUserId(testSellerId);
        request.setCancelReason("개인 사정");

        doNothing().when(dealService).rejectDeal(anyLong(), anyLong(), anyString());

        // WHEN
        ResponseEntity<String> response = dealController.rejectDealRequest(testDealId, request);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("성공적으로 거절되었습니다");
        verify(dealService).rejectDeal(testDealId, testSellerId, "개인 사정");
    }

    @Test
    @DisplayName("실패: 거래 거절 - EntityNotFoundException")
    void rejectDealRequest_Fail_NotFound() {
        // GIVEN
        DealRejectRequest request = new DealRejectRequest();
        request.setCurrentUserId(testSellerId);
        request.setCancelReason("개인 사정");

        doThrow(new EntityNotFoundException("거래를 찾을 수 없습니다"))
                .when(dealService).rejectDeal(anyLong(), anyLong(), anyString());

        // WHEN
        ResponseEntity<String> response = dealController.rejectDealRequest(testDealId, request);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("성공: 거래 수락")
    void acceptDealRequest_Success() {
        // GIVEN
        DealRejectRequest request = new DealRejectRequest();
        request.setCurrentUserId(testSellerId);

        doNothing().when(dealService).acceptDeal(anyLong(), anyLong());

        // WHEN
        ResponseEntity<?> response = dealController.acceptDealRequest(testDealId, request);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).toString().contains("성공적으로 수락되었습니다");
        verify(dealService).acceptDeal(testDealId, testSellerId);
    }

    @Test
    @DisplayName("실패: 거래 수락 - 유효하지 않은 판매자 ID")
    void acceptDealRequest_Fail_InvalidSellerId() {
        // GIVEN
        DealRejectRequest request = new DealRejectRequest();
        request.setCurrentUserId(null);

        // WHEN
        ResponseEntity<?> response = dealController.acceptDealRequest(testDealId, request);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(dealService, never()).acceptDeal(anyLong(), anyLong());
    }

    @Test
    @DisplayName("성공: 거래 상태 업데이트")
    void updateDealStatus_Success() {
        // GIVEN
        when(dealService.updateDealStatus(testDealId, "ACCEPTED"))
                .thenReturn(dealResponse);

        // WHEN
        ResponseEntity<?> response = dealController.updateDealStatus(testDealId, "ACCEPTED");

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(DealResponse.class);
        verify(dealService).updateDealStatus(testDealId, "ACCEPTED");
    }

    @Test
    @DisplayName("실패: 거래 상태 업데이트 - 유효하지 않은 상태")
    void updateDealStatus_Fail_InvalidStatus() {
        // GIVEN
        when(dealService.updateDealStatus(testDealId, "INVALID"))
                .thenThrow(new IllegalArgumentException("유효하지 않은 상태"));

        // WHEN
        ResponseEntity<?> response = dealController.updateDealStatus(testDealId, "INVALID");

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("성공: 거래 취소")
    void cancelDeal_Success() {
        // GIVEN
        doNothing().when(dealService).cancelDeal(anyLong(), anyLong());

        // WHEN
        ResponseEntity<?> response = dealController.cancelDeal(testDealId, testBuyerId);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(dealService).cancelDeal(testDealId, testBuyerId);
    }

    @Test
    @DisplayName("성공: 거래 확정")
    void confirmDeal_Success() {
        // GIVEN
        ConfirmDealRequest request = new ConfirmDealRequest();
        request.setCurrentUserId(testBuyerId);

        doNothing().when(dealService).confirmDeal(anyLong(), anyLong());

        // WHEN
        ResponseEntity<ApiResponse<Void>> response = dealController.confirmDeal(testDealId, request);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        verify(dealService).confirmDeal(testDealId, testBuyerId);
    }
}

