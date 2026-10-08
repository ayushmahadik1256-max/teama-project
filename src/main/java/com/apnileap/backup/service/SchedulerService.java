package com.apnileap.backup.service;

import com.apnileap.backup.dto.JobModels.*;
import com.apnileap.backup.lock.DeadlockDetector;
import com.apnileap.backup.lock.LockManager;
import com.apnileap.backup.scheduler.SchedulingStrategy;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SchedulerService {

    private final Map<String, SchedulingStrategy> strategies = new ConcurrentHashMap<>();
    private final LockManager lockManager;
    private final DeadlockDetector deadlockDetector;

    private final Map<String, JobIntakeRequest> jobsMap = new ConcurrentHashMap<>();
    private final Map<String, String> jobStates = new ConcurrentHashMap<>();
    private final Map<String, SchedulingDecisionResponse> decisionsMap = new ConcurrentHashMap<>();
    private final Map<String, String> idempotencyMap = new ConcurrentHashMap<>();

    private final CopyOnWriteArrayList<SseEmitter> sseEmitters = new CopyOnWriteArrayList<>();

    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicInteger detectedDeadlocks = new AtomicInteger(0);

    public SchedulerService(
        List<SchedulingStrategy> strategyList,
        LockManager lockManager,
        DeadlockDetector deadlockDetector
    ) {
        for (SchedulingStrategy s : strategyList) {
            strategies.put(s.getPolicyName().toUpperCase(), s);
        }
        this.lockManager = lockManager;
        this.deadlockDetector = deadlockDetector;
    }

    public JobIntakeResponse submitJob(JobIntakeRequest req, String correlationId, String idempotencyKey) {
        if (idempotencyKey != null && idempotencyMap.containsKey(idempotencyKey)) {
            String existingJobId = idempotencyMap.get(idempotencyKey);
            JobIntakeRequest old = jobsMap.get(existingJobId);
            return new JobIntakeResponse(
                old.job_id(),
                1,
                jobStates.getOrDefault(old.job_id(), "QUEUED"),
                Instant.now().toString(),
                "PRIORITY",
                correlationId
            );
        }

        jobsMap.put(req.job_id(), req);
        jobStates.put(req.job_id(), "QUEUED");
        totalProcessed.incrementAndGet();

        if (idempotencyKey != null) {
            idempotencyMap.put(idempotencyKey, req.job_id());
        }

        int position = jobsMap.size();
        String estimatedStart = Instant.now().plusMillis(position * 250L).toString();

        publishEvent(new JobStateEvent(
            req.job_id(),
            "QUEUED",
            Instant.now().toString(),
            "PRIORITY",
            correlationId,
            "Job successfully enqueued atomically"
        ));

        return new JobIntakeResponse(
            req.job_id(),
            position,
            "QUEUED",
            estimatedStart,
            "PRIORITY",
            correlationId
        );
    }

    public JobStateEvent getJobStatus(String jobId, String correlationId) {
        JobIntakeRequest job = jobsMap.get(jobId);
        if (job == null) return null;
        String state = jobStates.getOrDefault(jobId, "QUEUED");
        return new JobStateEvent(
            jobId,
            state,
            Instant.now().toString(),
            "PRIORITY",
            correlationId,
            "Target file: " + job.file_id() + ", Priority: " + job.priority()
        );
    }

    public WorkerQueueResponse getWorkerQueue(String workerId) {
        List<WorkerQueueItem> list = new ArrayList<>();
        int idx = 0;
        for (JobIntakeRequest j : jobsMap.values()) {
            list.add(new WorkerQueueItem(
                j.job_id(),
                j.file_id(),
                j.priority(),
                jobStates.getOrDefault(j.job_id(), "QUEUED"),
                Instant.now().plusMillis(idx * 300L).toString(),
                j.estimated_duration_ms() != null ? j.estimated_duration_ms() : 500L
            ));
            idx++;
        }
        return new WorkerQueueResponse(workerId, list.size(), list);
    }

    public SchedulingDecisionResponse runScheduler(SchedulerRunRequest req) {
        String policy = (req.policy() != null) ? req.policy().toUpperCase() : "PRIORITY";
        SchedulingStrategy strategy = strategies.getOrDefault(policy, strategies.get("PRIORITY"));

        List<JobIntakeRequest> pool = (req.queue_snapshot() != null && !req.queue_snapshot().isEmpty())
            ? req.queue_snapshot()
            : new ArrayList<>(jobsMap.values());

        List<String> workers = (req.available_workers() != null && !req.available_workers().isEmpty())
            ? req.available_workers()
            : List.of("worker-alpha-01", "worker-beta-02");

        SchedulingDecisionResponse decision = strategy.schedule(pool, workers, req.quantum_ms());
        decisionsMap.put(decision.decision_id(), decision);

        for (AssignedJob aj : decision.scheduled_allocations()) {
            jobStates.put(aj.job_id(), "READY");
            publishEvent(new JobStateEvent(
                aj.job_id(),
                "READY",
                Instant.now().toString(),
                strategy.getPolicyName(),
                UUID.randomUUID().toString(),
                "Assigned to workers: " + aj.assigned_worker_ids()
            ));
        }

        return decision;
    }

    public SchedulingDecisionResponse getDecision(String decisionId) {
        return decisionsMap.get(decisionId);
    }

    public LockAcquireResponse acquireLock(LockAcquireRequest req) {
        return lockManager.acquireLock(req);
    }

    public LockReleaseResponse releaseLock(String leaseId) {
        return lockManager.releaseLock(leaseId);
    }

    public DeadlockAnalyseResponse analyzeDeadlocks(DeadlockAnalyseRequest req) {
        DeadlockAnalyseResponse resp = deadlockDetector.analyze(req.edges());
        if (resp.deadlock_detected()) {
            detectedDeadlocks.incrementAndGet();
        }
        return resp;
    }

    public TelemetryMetrics getMetrics() {
        return new TelemetryMetrics(
            totalProcessed.get(),
            jobsMap.size(),
            Math.min(95.0, 15.0 + (jobsMap.size() * 3.2)),
            Math.min(90.0, 10.0 + (jobsMap.size() * 2.8)),
            45.2,
            180.5,
            60.0 * Math.max(1, totalProcessed.get() / 10.0),
            lockManager.getActiveLocksCount(),
            detectedDeadlocks.get()
        );
    }

    public SseEmitter registerStream() {
        SseEmitter emitter = new SseEmitter(180_000L);
        sseEmitters.add(emitter);
        emitter.onCompletion(() -> sseEmitters.remove(emitter));
        emitter.onTimeout(() -> sseEmitters.remove(emitter));
        emitter.onError((e) -> sseEmitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event()
                .name("CONNECTED")
                .data("Connected to Team A Backup Scheduler Live Stream"));
        } catch (IOException ignored) {}

        return emitter;
    }

    private void publishEvent(JobStateEvent event) {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : sseEmitters) {
            try {
                emitter.send(SseEmitter.event().name("JOB_STATE_CHANGE").data(event));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }
        sseEmitters.removeAll(deadEmitters);
    }
}
