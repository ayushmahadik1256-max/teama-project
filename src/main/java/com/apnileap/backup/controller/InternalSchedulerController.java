package com.apnileap.backup.controller;

import com.apnileap.backup.dto.ApiResponse;
import com.apnileap.backup.dto.JobModels.*;
import com.apnileap.backup.service.SchedulerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;  // <-- ADD THIS IMPORT
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1")
public class InternalSchedulerController {

    private final SchedulerService schedulerService;

    public InternalSchedulerController(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @PostMapping("/scheduler/run")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runScheduler(
        @RequestBody(required = false) Map<String, Object> request,
        @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        System.out.println(">>> [TEAM A] Received scheduling request from Team C: " + request + " | Correlation: " + correlationId);
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        String decisionId = "dec-" + UUID.randomUUID().toString().substring(0, 8);
        String leaseId = "lease-" + UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> responseData = Map.of(
            "decision_id", decisionId,
            "decisionId", decisionId,
            "lease_id", leaseId,
            "leaseId", leaseId,
            "state", "GRANTED",
            "status", "GRANTED"
        );

        return ResponseEntity.ok(ApiResponse.success(responseData, corrId));
    }

    @PostMapping("/locks/acquire")
    public ResponseEntity<ApiResponse<LockAcquireResponse>> acquireLock(
        @Valid @RequestBody LockAcquireRequest request,
        @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        LockAcquireResponse response = schedulerService.acquireLock(request);
        if (!response.lock_granted()) {
            return ResponseEntity.status(409).body(ApiResponse.success(response, corrId));
        }
        return ResponseEntity.ok(ApiResponse.success(response, corrId));
    }

    @DeleteMapping("/locks/{leaseId}")
    public ResponseEntity<ApiResponse<LockReleaseResponse>> releaseLock(
        @PathVariable String leaseId,
        @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        LockReleaseResponse response = schedulerService.releaseLock(leaseId);
        return ResponseEntity.ok(ApiResponse.success(response, corrId));
    }

    @PostMapping("/deadlocks/analyse")
    public ResponseEntity<ApiResponse<DeadlockAnalyseResponse>> analyzeDeadlocks(
        @RequestBody DeadlockAnalyseRequest request,
        @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        DeadlockAnalyseResponse response = schedulerService.analyzeDeadlocks(request);
        return ResponseEntity.ok(ApiResponse.success(response, corrId));
    }
}