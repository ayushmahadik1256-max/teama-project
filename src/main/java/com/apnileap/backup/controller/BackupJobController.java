package com.apnileap.backup.controller;

import com.apnileap.backup.dto.ApiResponse;
import com.apnileap.backup.dto.JobModels.*;
import com.apnileap.backup.service.SchedulerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class BackupJobController {

    private final SchedulerService schedulerService;

    public BackupJobController(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @PostMapping("/backup/jobs")
    public ResponseEntity<ApiResponse<JobIntakeResponse>> submitJob(
        @Valid @RequestBody JobIntakeRequest request,
        @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        JobIntakeResponse response = schedulerService.submitJob(request, corrId, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(response, corrId));
    }

    @GetMapping("/backup/jobs/{jobId}")
    public ResponseEntity<ApiResponse<JobStateEvent>> getJob(
        @PathVariable String jobId,
        @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        JobStateEvent event = schedulerService.getJobStatus(jobId, corrId);
        if (event == null) {
            return ResponseEntity.status(404).body(ApiResponse.failure("JOB_NOT_FOUND", "Job ID not found: " + jobId, null, corrId));
        }
        return ResponseEntity.ok(ApiResponse.success(event, corrId));
    }

    @GetMapping("/workers/{workerId}/queue")
    public ResponseEntity<ApiResponse<WorkerQueueResponse>> getWorkerQueue(
        @PathVariable String workerId,
        @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        WorkerQueueResponse queueResp = schedulerService.getWorkerQueue(workerId);
        return ResponseEntity.ok(ApiResponse.success(queueResp, corrId));
    }

    @GetMapping("/backup/decisions/{decisionId}")
    public ResponseEntity<ApiResponse<SchedulingDecisionResponse>> getDecision(
        @PathVariable String decisionId,
        @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId
    ) {
        String corrId = (correlationId != null) ? correlationId : UUID.randomUUID().toString();
        SchedulingDecisionResponse decision = schedulerService.getDecision(decisionId);
        if (decision == null) {
            return ResponseEntity.status(404).body(ApiResponse.failure("DECISION_NOT_FOUND", "No decision found for ID: " + decisionId, null, corrId));
        }
        return ResponseEntity.ok(ApiResponse.success(decision, corrId));
    }
}
