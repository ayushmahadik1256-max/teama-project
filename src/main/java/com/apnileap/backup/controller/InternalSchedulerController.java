package com.apnileap.backup.controller;

import com.apnileap.backup.dto.ApiResponse;
import com.apnileap.backup.dto.JobModels.*;
import com.apnileap.backup.service.SchedulerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1")
public class InternalSchedulerController {

    private final SchedulerService schedulerService;

    public InternalSchedulerController(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @PostMapping("/scheduler/run")
    public ResponseEntity<ApiResponse<SchedulingDecisionResponse>> runScheduler(
        @RequestBody SchedulerRunRequest request,
        @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        SchedulingDecisionResponse decision = schedulerService.runScheduler(request);
        return ResponseEntity.ok(ApiResponse.success(decision, corrId));
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
