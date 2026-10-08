package com.apnileap.backup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public class JobModels {

    public record JobIntakeRequest(
        @NotBlank(message = "job_id is required") String job_id,
        @NotBlank(message = "user_id is required") String user_id,
        @NotBlank(message = "file_id is required") String file_id,
        @NotBlank(message = "backup_type is required") String backup_type,
        @NotBlank(message = "source_path is required") String source_path,
        @NotNull(message = "priority is required") Integer priority,
        String submitted_at,
        String preferred_window,
        Long estimated_duration_ms
    ) {}

    public record JobIntakeResponse(
        String job_id,
        int queue_position,
        String state,
        String estimated_start,
        String accepted_policy,
        String correlation_id
    ) {}

    public record WorkerQueueItem(
        String job_id,
        String file_id,
        int priority,
        String state,
        String estimated_start,
        long estimated_duration_ms
    ) {}

    public record WorkerQueueResponse(
        String worker_id,
        int queue_depth,
        List<WorkerQueueItem> queued_jobs
    ) {}

    public record SchedulerRunRequest(
        List<JobIntakeRequest> queue_snapshot,
        List<String> available_workers,
        String policy, // FCFS, SJF, RR, PRIORITY
        Long quantum_ms,
        Map<String, Object> policy_parameters
    ) {}

    public record DecisionMetrics(
        double avg_waiting_time_ms,
        double avg_turnaround_time_ms,
        double throughput_jobs_per_sec,
        double worker_utilization_ratio
    ) {}

    public record SchedulingDecisionResponse(
        String decision_id,
        String policy,
        String algorithm_version,
        String scheduled_start,
        List<AssignedJob> scheduled_allocations,
        DecisionMetrics decision_metrics
    ) {}

    public record AssignedJob(
        String job_id,
        String file_id,
        List<String> assigned_worker_ids,
        long start_offset_ms,
        long execution_time_ms
    ) {}

    public record LockAcquireRequest(
        @NotBlank String file_id,
        @NotBlank String holder_id,
        @NotBlank String lock_mode, // EXCLUSIVE, SHARED
        Long lease_timeout_ms
    ) {}

    public record LockAcquireResponse(
        boolean lock_granted,
        String lease_id,
        String file_id,
        String holder_id,
        String lock_mode,
        long expires_at_epoch_ms,
        String conflict_reason
    ) {}

    public record LockReleaseResponse(
        boolean released,
        String lease_id,
        String message
    ) {}

    public record WaitForEdge(
        String waiting_holder_id,
        String resource_id,
        String held_by_holder_id
    ) {}

    public record DeadlockAnalyseRequest(
        List<WaitForEdge> edges
    ) {}

    public record DeadlockAnalyseResponse(
        boolean deadlock_detected,
        List<List<String>> deadlock_cycles,
        String prevention_or_recovery_action,
        List<String> recommended_victim_holders
    ) {}

    public record TelemetryMetrics(
        long total_jobs_processed,
        int current_queue_depth,
        double cpu_utilization_pct,
        double io_utilization_pct,
        double avg_wait_time_ms,
        double avg_turnaround_time_ms,
        double throughput_per_minute,
        int active_locks_count,
        int detected_deadlocks_count
    ) {}

    public record JobStateEvent(
        String job_id,
        String state,
        String timestamp,
        String policy,
        String correlation_id,
        String details
    ) {}
}
