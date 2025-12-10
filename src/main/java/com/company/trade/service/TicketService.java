package com.company.trade.service;

import com.company.trade.entity.Ticket;
import com.company.trade.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    // 의존성 주입 (Dependency Injection)
    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    /**
     * ID를 사용하여 티켓 상세 정보를 조회합니다.
     * @param ticketId 조회할 티켓의 ID
     * @return 티켓 객체
     * @throws ResponseStatusException ID에 해당하는 티켓이 없을 경우 404 NOT FOUND 예외 발생
     */
    public Ticket getTicketDetail(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket not found with ID: " + ticketId
                ));
    }
}