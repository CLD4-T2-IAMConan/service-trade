package com.company.trade.controller;

import com.company.trade.entity.Ticket;
import com.company.trade.entity.TicketStatus;
import com.company.trade.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketController 단위 테스트")
class TicketControllerUnitTest {

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private TicketController ticketController;

    private Long testTicketId;
    private Ticket testTicket;

    @BeforeEach
    void setUp() {
        testTicketId = 1L;
        testTicket = Ticket.builder()
                .ticketId(testTicketId)
                .eventName("테스트 콘서트")
                .ticketStatus(TicketStatus.AVAILABLE)
                .build();
    }

    @Test
    @DisplayName("성공: 티켓 상세 조회")
    void getTicketDetail_Success() {
        // GIVEN
        when(ticketService.getTicketDetail(testTicketId)).thenReturn(testTicket);

        // WHEN
        ResponseEntity<Ticket> response = ticketController.getTicketDetail(testTicketId);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTicketId()).isEqualTo(testTicketId);
        assertThat(response.getBody().getEventName()).isEqualTo("테스트 콘서트");
        verify(ticketService).getTicketDetail(testTicketId);
    }

    @Test
    @DisplayName("실패: 티켓 상세 조회 - 티켓을 찾을 수 없는 경우")
    void getTicketDetail_Fail_NotFound() {
        // GIVEN
        when(ticketService.getTicketDetail(testTicketId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        // WHEN & THEN
        assertThatThrownBy(() -> ticketController.getTicketDetail(testTicketId))
                .isInstanceOf(ResponseStatusException.class);

        verify(ticketService).getTicketDetail(testTicketId);
    }
}

