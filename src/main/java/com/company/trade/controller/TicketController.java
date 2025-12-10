package com.company.trade.controller;


import com.company.trade.entity.Ticket;
import com.company.trade.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets") // 기본 경로 설정: /api/tickets
public class TicketController {

    // 💡 CORS 설정 (WebConfig에 전역 설정이 되어있으므로 여기서는 @CrossOrigin 주석 처리)
    //@CrossOrigin(origins = "http://localhost:3000")

    private final TicketService ticketService;

    // 의존성 주입 (Dependency Injection)
    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * 티켓 상세 정보를 조회하는 API 엔드포인트
     * GET http://localhost:8080/api/tickets/{id}
     * @param id URL 경로 변수 (ticket_id)
     * @return 200 OK와 함께 티켓 상세 정보 (JSON) 반환
     */
    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getTicketDetail(@PathVariable("id") Long id) {

        // 1. 서비스 레이어 호출 (데이터 조회 및 예외 처리)
        Ticket ticket = ticketService.getTicketDetail(id);

        // 2. HTTP 응답 반환
        return ResponseEntity.ok(ticket);
    }
}