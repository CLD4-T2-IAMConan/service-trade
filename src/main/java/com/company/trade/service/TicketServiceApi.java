package com.company.trade.service;

import com.company.trade.dto.ApiResponse;
import com.company.trade.dto.TicketResponse;
import com.company.trade.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚨 Slf4j Logger Import
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j // 🚨 Slf4j Logger 활성화
public class TicketServiceApi {

    @Value("${api.ticket-service.url:http://localhost:8082}")
    private String TICKET_SERVICE_URL;

    private final RestTemplate restTemplate;

    /**
     * 특정 티켓 ID로 티켓 상세 정보를 조회합니다.
     */
    public Optional<TicketResponse> getTicketById(Long ticketId) {
        String url = TICKET_SERVICE_URL + "/tickets/{ticketId}";

        try {
            // 💡 [핵심 변경] getForObject 대신 exchange 사용 (Generic Type 처리)
            ResponseEntity<ApiResponse<TicketResponse>> responseEntity =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null, // Request Entity (없음)
                            // 🚨 Generic Type (ApiResponse<TicketResponse>)을 정확히 전달
                            new ParameterizedTypeReference<ApiResponse<TicketResponse>>() {},
                            ticketId
                    );


            // 🚨 Wrapper DTO에서 실제 data 필드를 추출하여 반환
            ApiResponse<TicketResponse> apiResponse = responseEntity.getBody();
            if (apiResponse != null && apiResponse.isSuccess()) {
                // data 필드에서 TicketResponse 객체를 추출합니다.
                return Optional.ofNullable(apiResponse.getData());
            }

            // 응답은 성공했지만 success: false일 경우 (로직상 이리로 오면 안 됨)
            log.warn("[API-TICKET-GET] API 호출 성공했으나 success: false 응답. Error: {}", apiResponse.getError());
            return Optional.empty();

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("[API-TICKET-GET-FAIL] 404 Not Found. 티켓 ID {}를 찾을 수 없음.", ticketId);
            return Optional.empty();

        } catch (HttpClientErrorException e) {
            log.error("[API-TICKET-GET-FAIL] HTTP Client Error (4XX). Status={}, ResponseBody={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Ticket Service API 호출 중 HTTP 오류 발생: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[API-TICKET-GET-FAIL] 연결 또는 알 수 없는 오류 발생: Message={}", e.getMessage(), e);
            throw new RuntimeException("Ticket Service API 연결 또는 알 수 없는 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 티켓 상태를 지정된 새 상태로 변경합니다. (PUT /tickets/{id}/status/{newStatus})
     */
    public void updateTicketStatus(Long ticketId, String newStatus) {
        String url = TICKET_SERVICE_URL + "/tickets/{ticketId}/status/{newStatus}";


        try {
            // PUT 요청
            restTemplate.put(url, null, ticketId, newStatus);

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("[API-TICKET-PUT-FAIL] 404 Not Found. 티켓 ID {}를 찾을 수 없음.", ticketId);
            throw new EntityNotFoundException("티켓 서비스에서 티켓 ID(" + ticketId + ")를 찾을 수 없습니다.");

        } catch (HttpClientErrorException e) {
            // 400 Bad Request 등 오류
            log.error("[API-TICKET-PUT-FAIL] HTTP Client Error (4XX). Status={}, ResponseBody={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e); // 🚨 상태 코드 및 응답 본문 로깅

            throw new RuntimeException("티켓 상태 변경 API 오류: " + e.getResponseBodyAsString());

        } catch (Exception e) {
            // 기타 연결 오류
            log.error("[API-TICKET-PUT-FAIL] 연결 오류 발생: Message={}", e.getMessage(), e); // 🚨 메시지 및 스택 트레이스 로깅

            throw new RuntimeException("티켓 상태 변경 API 호출 중 연결 오류 발생: " + e.getMessage());
        }
    }
}