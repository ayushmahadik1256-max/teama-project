package com.apnileap.backup.controller;

import com.apnileap.backup.dto.ApiResponse;
import com.apnileap.backup.dto.JobModels.TelemetryMetrics;
import com.apnileap.backup.service.SchedulerService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class TelemetryController {

    private final SchedulerService schedulerService;

    public TelemetryController(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @GetMapping("/metrics/backup")
    public ResponseEntity<ApiResponse<TelemetryMetrics>> getMetrics(
        @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        TelemetryMetrics metrics = schedulerService.getMetrics();
        return ResponseEntity.ok(ApiResponse.success(metrics, corrId));
    }

    @GetMapping(value = "/stream/backup", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeStream() {
        return schedulerService.registerStream();
    }
}
