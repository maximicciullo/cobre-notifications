package com.cobre.notifications.infrastructure.adapter.in.web;

import com.cobre.notifications.application.port.in.GetNotificationEventUseCase;
import com.cobre.notifications.application.port.in.QueryFilter;
import com.cobre.notifications.application.port.in.QueryNotificationEventsUseCase;
import com.cobre.notifications.application.port.in.ReplayNotificationEventUseCase;
import com.cobre.notifications.domain.model.DeliveryStatus;
import com.cobre.notifications.infrastructure.adapter.in.web.dto.NotificationEventResponse;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/notification_events")
public class NotificationEventController {

    private final QueryNotificationEventsUseCase queryUseCase;
    private final GetNotificationEventUseCase getUseCase;
    private final ReplayNotificationEventUseCase replayUseCase;

    public NotificationEventController(
            QueryNotificationEventsUseCase queryUseCase,
            GetNotificationEventUseCase getUseCase,
            ReplayNotificationEventUseCase replayUseCase
    ) {
        this.queryUseCase = queryUseCase;
        this.getUseCase = getUseCase;
        this.replayUseCase = replayUseCase;
    }

    @GetMapping
    public Page<NotificationEventResponse> list(
            @RequestParam(value = "created_from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(value = "created_to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(value = "delivery_status", required = false) DeliveryStatus deliveryStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String clientId = CurrentClientHolder.get();
        QueryFilter filter = new QueryFilter(createdFrom, createdTo, deliveryStatus, page, size);
        return queryUseCase.query(clientId, filter).map(NotificationEventResponse::from);
    }

    @GetMapping("/{notificationEventId}")
    public NotificationEventResponse getOne(@PathVariable String notificationEventId) {
        String clientId = CurrentClientHolder.get();
        return NotificationEventResponse.from(getUseCase.getByIdForClient(clientId, notificationEventId));
    }

    @PostMapping("/{notificationEventId}/replay")
    public ResponseEntity<NotificationEventResponse> replay(@PathVariable String notificationEventId) {
        String clientId = CurrentClientHolder.get();
        var result = replayUseCase.replayForClient(clientId, notificationEventId);
        return ResponseEntity.accepted().body(NotificationEventResponse.from(result));
    }
}
