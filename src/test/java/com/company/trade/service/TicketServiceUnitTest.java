package com.company.trade.service;

import com.company.trade.entity.Ticket;
import com.company.trade.entity.TicketStatus;
import com.company.trade.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketService 단위 테스트")
class TicketServiceUnitTest {

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketService ticketService;

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
        when(ticketRepository.findById(testTicketId)).thenReturn(Optional.of(testTicket));

        // WHEN
        Ticket result = ticketService.getTicketDetail(testTicketId);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getTicketId()).isEqualTo(testTicketId);
        assertThat(result.getEventName()).isEqualTo("테스트 콘서트");
        assertThat(result.getTicketStatus()).isEqualTo(TicketStatus.AVAILABLE);

        verify(ticketRepository).findById(testTicketId);
    }

    @Test
    @DisplayName("실패: 티켓을 찾을 수 없는 경우")
    void getTicketDetail_Fail_TicketNotFound() {
        // GIVEN
        when(ticketRepository.findById(testTicketId)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> ticketService.getTicketDetail(testTicketId))
                .isInstanceOf(ResponseStatusException.class);

        verify(ticketRepository).findById(testTicketId);
    }
}

