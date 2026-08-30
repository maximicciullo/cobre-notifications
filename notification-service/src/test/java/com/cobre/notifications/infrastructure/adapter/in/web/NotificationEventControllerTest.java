package com.cobre.notifications.infrastructure.adapter.in.web;

import com.cobre.notifications.application.port.in.GetNotificationEventUseCase;
import com.cobre.notifications.application.port.in.NotificationEventView;
import com.cobre.notifications.application.port.in.QueryFilter;
import com.cobre.notifications.application.port.in.QueryNotificationEventsUseCase;
import com.cobre.notifications.application.port.in.ReplayNotificationEventUseCase;
import com.cobre.notifications.application.port.out.SubscriptionRepositoryPort;
import com.cobre.notifications.domain.exception.NotificationEventNotFoundException;
import com.cobre.notifications.domain.exception.ReplayNotAllowedException;
import com.cobre.notifications.domain.model.DeliveryStatus;
import com.cobre.notifications.domain.model.Subscription;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationEventController.class)
class NotificationEventControllerTest {

    private static final String VALID_KEY = "valid-key";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueryNotificationEventsUseCase queryUseCase;
    @MockBean
    private GetNotificationEventUseCase getUseCase;
    @MockBean
    private ReplayNotificationEventUseCase replayUseCase;

    // Required because ApiKeyAuthFilter (a real @Component) is on the filter chain in this slice.
    @MockBean
    private SubscriptionRepositoryPort subscriptionRepository;

    private void stubValidApiKey() {
        when(subscriptionRepository.findByApiKey(VALID_KEY)).thenReturn(Optional.of(
                new Subscription("CLIENT001", VALID_KEY, "https://example.com/hook", true, Instant.now(), Instant.now())
        ));
    }

    @Test
    void listWithoutApiKeyIsUnauthorized() throws Exception {
        mockMvc.perform(get("/notification_events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listWithValidApiKeyReturnsEvents() throws Exception {
        stubValidApiKey();
        NotificationEventView view = new NotificationEventView(
                "EVT001", "CLIENT001", "credit_card_payment", "content",
                Instant.parse("2026-08-29T10:00:00Z"), DeliveryStatus.COMPLETED, 0, null, 200, null, null
        );
        when(queryUseCase.query(eq("CLIENT001"), any(QueryFilter.class)))
                .thenReturn(new PageImpl<>(List.of(view)));

        mockMvc.perform(get("/notification_events").header("X-Api-Key", VALID_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].event_id").value("EVT001"))
                .andExpect(jsonPath("$.content[0].delivery_status").value("completed"));
    }

    @Test
    void getOneNotFoundReturns404() throws Exception {
        stubValidApiKey();
        when(getUseCase.getByIdForClient(eq("CLIENT001"), anyString()))
                .thenThrow(new NotificationEventNotFoundException("EVT999"));

        mockMvc.perform(get("/notification_events/EVT999").header("X-Api-Key", VALID_KEY))
                .andExpect(status().isNotFound());
    }

    @Test
    void replaySuccessReturns202() throws Exception {
        stubValidApiKey();
        NotificationEventView view = new NotificationEventView(
                "EVT003", "CLIENT001", "credit_transfer", "content",
                Instant.parse("2026-08-29T10:00:00Z"), DeliveryStatus.PENDING, 0, null, null, null, null
        );
        when(replayUseCase.replayForClient("CLIENT001", "EVT003")).thenReturn(view);

        mockMvc.perform(post("/notification_events/EVT003/replay").header("X-Api-Key", VALID_KEY))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.delivery_status").value("pending"));
    }

    @Test
    void replayNotAllowedReturns409() throws Exception {
        stubValidApiKey();
        when(replayUseCase.replayForClient(eq("CLIENT001"), anyString()))
                .thenThrow(new ReplayNotAllowedException("Only a FAILED delivery can be replayed"));

        mockMvc.perform(post("/notification_events/EVT001/replay").header("X-Api-Key", VALID_KEY))
                .andExpect(status().isConflict());
    }
}
