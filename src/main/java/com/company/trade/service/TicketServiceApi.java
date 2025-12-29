package com.company.trade.service;

import com.company.trade.dto.ApiResponse;
import com.company.trade.dto.TicketResponse;
import com.company.trade.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚨 Slf4j Logger Import
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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
        // UriComponentsBuilder를 사용하여 URL을 안전하게 생성 (슬래시 중복 방지)
        String url = UriComponentsBuilder.fromHttpUrl(TICKET_SERVICE_URL)
                .path("/api/tickets/{ticketId}")
                .buildAndExpand(ticketId)
                .toUriString();
        
        log.info("[API-TICKET-GET-START] 티켓 정보 조회 시작. Ticket ID: {}, URL: {}", ticketId, url);

        try {
            // 💡 [핵심 변경] getForObject 대신 exchange 사용 (Generic Type 처리)
            ResponseEntity<ApiResponse<TicketResponse>> responseEntity =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null, // Request Entity (없음)
                            // 🚨 Generic Type (ApiResponse<TicketResponse>)을 정확히 전달
                            new ParameterizedTypeReference<ApiResponse<TicketResponse>>() {}
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
        } catch (ResourceAccessException e) {
            // 네트워크 연결 실패 (타임아웃, 서버 꺼짐 등) 시 주로 발생하는 예외
            log.error("[API-TICKET-GET-FAIL] 서버 연결 불가. URL={}, 메시지={}", url, e.getMessage(), e);
            throw new RuntimeException("티켓 서비스 서버에 연결할 수 없습니다. 서버 상태를 확인하세요. URL: " + url);
        } catch (Exception e) {
            log.error("[API-TICKET-GET-FAIL] 연결 또는 알 수 없는 오류 발생: Message={}, URL={}", e.getMessage(), url, e);
            throw new RuntimeException("티켓 정보 조회 중 연결 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 티켓 상태를 지정된 새 상태로 변경합니다. (PUT /api/tickets/{id}/status/{newStatus})
     */
    public void updateTicketStatus(Long ticketId, String newStatus, String accessToken) {
        // 1. UriComponentsBuilder를 사용하여 URL을 안전하게 생성 (슬래시 중복 방지)
        String url = UriComponentsBuilder.fromHttpUrl(TICKET_SERVICE_URL)
                .path("/api/tickets/{ticketId}/status/{newStatus}")
                .buildAndExpand(ticketId, newStatus)
                .toUriString();

        log.info("[API-TICKET-PUT-START] 요청 URL: {}", url); // 디버깅을 위해 실제 URL 출력

        try {
            HttpHeaders headers = new HttpHeaders();
            if (accessToken != null) {
                String token = accessToken.startsWith("Bearer ") ? accessToken : "Bearer " + accessToken;
                headers.set(HttpHeaders.AUTHORIZATION, token);
            }

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            // 이미 buildAndExpand를 했으므로 추가 인자 없이 호출
            restTemplate.exchange(url, HttpMethod.PUT, requestEntity, Void.class);


        } catch (HttpClientErrorException.NotFound e) {
            log.warn("[API-TICKET-PUT-FAIL] 404 Not Found. 티켓 ID {} 찾을 수 없음.", ticketId);
            throw new EntityNotFoundException("티켓 서비스에서 티켓 ID(" + ticketId + ")를 찾을 수 없습니다.");

        } catch (HttpClientErrorException e) {
            log.error("[API-TICKET-PUT-FAIL] 4XX 에러. 상태코드={}, 응답={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("티켓 상태 변경 API 오류: " + e.getResponseBodyAsString());

        } catch (ResourceAccessException e) {
            // 네트워크 연결 실패 (타임아웃, 서버 꺼짐 등) 시 주로 발생하는 예외
            log.error("[API-TICKET-PUT-FAIL] 서버 연결 불가. URL={}, 메시지={}", url, e.getMessage());
            throw new RuntimeException("티켓 서비스 서버에 연결할 수 없습니다. 주소를 확인하세요.");

        } catch (Exception e) {
            log.error("[API-TICKET-PUT-FAIL] 알 수 없는 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("티켓 상태 변경 중 예상치 못한 오류 발생: " + e.getMessage());
        }
    }

}