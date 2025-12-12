package com.company.trade.service;

import com.company.trade.dto.TicketResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException; // 🚨 추가
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.Optional; // 🚨 추가
//import com.company.trade.exception.EntityNotFoundException; // 🚨 Deal Service의 예외 임포트 필요


@Component
@RequiredArgsConstructor
public class TicketServiceApi {

    @Value("${api.ticket-service.url:http://localhost:8082}")
    private String TICKET_SERVICE_URL;

    private final RestTemplate restTemplate;

    // 🚨 [수정 1] 반환 타입을 Optional<TicketResponse>로 변경
    public Optional<TicketResponse> getTicketById(Long ticketId) {
        String url = TICKET_SERVICE_URL + "/tickets/" + ticketId;

        try {
            // RestTemplate 호출. 404가 아니면 객체 반환
            TicketResponse response = restTemplate.getForObject(url, TicketResponse.class);

            // 💡 RestTemplate은 404를 던지지만, 혹시 모를 null 반환 케이스도 Optional로 감쌉니다.
            return Optional.ofNullable(response);

        } catch (HttpClientErrorException.NotFound e) {
            // 🚨 [수정 2] 8082 서비스가 404 (Not Found)를 반환하면
            // HttpClientErrorException.NotFound 예외가 발생합니다.
            // 티켓이 없다는 의미이므로 Optional.empty()를 반환합니다.
            return Optional.empty();

        } catch (HttpClientErrorException e) {
            // 🚨 400, 403 등 다른 HTTP 오류 처리
            throw new RuntimeException("Ticket Service API 호출 중 HTTP 오류 발생: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            // 🚨 기타 연결 또는 서버 오류
            throw new RuntimeException("Ticket Service API 연결 또는 알 수 없는 오류 발생: " + e.getMessage());
        }
    }

//    // 🚨 [새로 추가] 상태 변경 API (PUT /tickets/{ticketId}/status/reserved)
//    public void updateStatusToReserved(Long ticketId) {
//        String url = TICKET_SERVICE_URL + "/tickets/" + ticketId + "/status/reserved";
//
//        try {
//            // PUT 요청은 보통 응답 Body가 없으므로 exchange 대신 put을 사용합니다.
//            restTemplate.put(url, null); // Body가 없으므로 null을 전달
//
//        } catch (HttpClientErrorException e) {
//            // 8082 서비스가 상태 변경을 거부하는 400 등을 반환할 경우
//            throw new RuntimeException("티켓 상태 변경 실패: " + e.getResponseBodyAsString());
//        } catch (Exception e) {
//            throw new RuntimeException("티켓 상태 변경 API 호출 중 오류 발생: " + e.getMessage());
//        }
//    }
}