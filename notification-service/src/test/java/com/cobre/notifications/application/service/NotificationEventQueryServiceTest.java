package com.cobre.notifications.application.service;

import com.cobre.notifications.application.port.in.NotificationEventView;
import com.cobre.notifications.application.port.in.QueryFilter;
import com.cobre.notifications.application.port.out.NotificationQueryRepositoryPort;
import com.cobre.notifications.domain.exception.NotificationEventNotFoundException;
import com.cobre.notifications.domain.model.DeliveryStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationEventQueryServiceTest {

    private final NotificationQueryRepositoryPort queryRepository = mock(NotificationQueryRepositoryPort.class);
    private final NotificationEventQueryService service = new NotificationEventQueryService(queryRepository);

    private NotificationEventView view() {
        return new NotificationEventView(
                "EVT001", "CLIENT001", "credit_card_payment", "content",
                Instant.parse("2026-08-29T10:00:00Z"), DeliveryStatus.COMPLETED, 0, null, 200, null, null
        );
    }

    @Test
    void queryDelegatesToRepositoryForTheGivenClient() {
        QueryFilter filter = new QueryFilter(null, null, null, 0, 20);
        Page<NotificationEventView> page = new PageImpl<>(List.of(view()));
        when(queryRepository.search("CLIENT001", filter)).thenReturn(page);

        Page<NotificationEventView> result = service.query("CLIENT001", filter);

        assertThat(result.getContent()).containsExactly(view());
    }

    @Test
    void getByIdForClientReturnsTheOwnedEvent() {
        when(queryRepository.findView("CLIENT001", "EVT001")).thenReturn(Optional.of(view()));

        NotificationEventView result = service.getByIdForClient("CLIENT001", "EVT001");

        assertThat(result.eventId()).isEqualTo("EVT001");
    }

    @Test
    void getByIdForClientThrowsWhenNotOwnedOrMissing() {
        when(queryRepository.findView("CLIENT_ATTACKER", "EVT001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByIdForClient("CLIENT_ATTACKER", "EVT001"))
                .isInstanceOf(NotificationEventNotFoundException.class);
    }
}
