package com.cobre.notifications.infrastructure.adapter.in.web;

import com.cobre.notifications.application.port.in.GetNotificationEventUseCase;
import com.cobre.notifications.application.port.in.QueryFilter;
import com.cobre.notifications.application.port.in.QueryNotificationEventsUseCase;
import com.cobre.notifications.application.port.in.ReplayNotificationEventUseCase;
import com.cobre.notifications.domain.model.DeliveryStatus;
import com.cobre.notifications.infrastructure.adapter.in.web.dto.NotificationEventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Notification Events", description = "Self-service API — query, inspect, and replay notification events")
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
    @Operation(
            summary = "List the calling client's notification events",
            description = "Always scoped to the authenticated client — filters are optional."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of matching events"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid X-Api-Key"),
    })
    public Page<NotificationEventResponse> list(
            @Parameter(description = "Only events created at/after this instant (ISO-8601, e.g. 2024-03-15T00:00:00Z)")
            @RequestParam(value = "created_from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @Parameter(description = "Only events created at/before this instant (ISO-8601)")
            @RequestParam(value = "created_to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @Parameter(description = "Filter by delivery status")
            @RequestParam(value = "delivery_status", required = false) DeliveryStatus deliveryStatus,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size
    ) {
        String clientId = CurrentClientHolder.get();
        QueryFilter filter = new QueryFilter(createdFrom, createdTo, deliveryStatus, page, size);
        return queryUseCase.query(clientId, filter).map(NotificationEventResponse::from);
    }

    @GetMapping("/{notificationEventId}")
    @Operation(summary = "Get one notification event's delivery details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event found and owned by the caller"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid X-Api-Key"),
            @ApiResponse(responseCode = "404", description = "Event doesn't exist, or belongs to another client"),
    })
    public NotificationEventResponse getOne(
            @Parameter(description = "e.g. EVT001") @PathVariable String notificationEventId
    ) {
        String clientId = CurrentClientHolder.get();
        return NotificationEventResponse.from(getUseCase.getByIdForClient(clientId, notificationEventId));
    }

    @PostMapping("/{notificationEventId}/replay")
    @Operation(
            summary = "Replay a definitively-failed delivery",
            description = "Only allowed when the event's current delivery_status is FAILED. Resets it to " +
                    "PENDING so it re-enters the exact same delivery/retry path as a fresh event."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Accepted — reset to PENDING, will be retried on the next poll"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid X-Api-Key"),
            @ApiResponse(responseCode = "404", description = "Event doesn't exist, or belongs to another client"),
            @ApiResponse(responseCode = "409", description = "Event is not currently FAILED — nothing to replay"),
    })
    public ResponseEntity<NotificationEventResponse> replay(
            @Parameter(description = "e.g. EVT003") @PathVariable String notificationEventId
    ) {
        String clientId = CurrentClientHolder.get();
        var result = replayUseCase.replayForClient(clientId, notificationEventId);
        return ResponseEntity.accepted().body(NotificationEventResponse.from(result));
    }
}
