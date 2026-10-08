package com.apnileap.backup.scheduler;

import com.apnileap.backup.dto.JobModels.*;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.*;

@Component
public class FcfsStrategy implements SchedulingStrategy {
    @Override
    public String getPolicyName() { return "FCFS"; }

    @Override
    public String getAlgorithmVersion() { return "1.0.0-FCFS-FIFO"; }

    @Override
    public SchedulingDecisionResponse schedule(List<JobIntakeRequest> jobs, List<String> workers, Long quantumMs) {
        String decisionId = UUID.randomUUID().toString();
        if (workers.isEmpty()) {
            workers = List.of("worker-default-1");
        }

        long[] workerAvailableAt = new long[workers.size()];
        List<AssignedJob> allocations = new ArrayList<>();

        double totalWait = 0;
        double totalTurnaround = 0;

        for (JobIntakeRequest job : jobs) {
            int bestWorkerIdx = 0;
            long earliestTime = workerAvailableAt[0];
            for (int i = 1; i < workers.size(); i++) {
                if (workerAvailableAt[i] < earliestTime) {
                    earliestTime = workerAvailableAt[i];
                    bestWorkerIdx = i;
                }
            }

            long duration = job.estimated_duration_ms() != null ? job.estimated_duration_ms() : 500L;
            long startTime = workerAvailableAt[bestWorkerIdx];
            long completionTime = startTime + duration;
            workerAvailableAt[bestWorkerIdx] = completionTime;

            totalWait += startTime;
            totalTurnaround += completionTime;

            allocations.add(new AssignedJob(
                job.job_id(),
                job.file_id(),
                List.of(workers.get(bestWorkerIdx)),
                startTime,
                duration
            ));
        }

        int count = Math.max(1, jobs.size());
        long maxSpan = Arrays.stream(workerAvailableAt).max().orElse(1L);
        double utilization = (totalTurnaround - totalWait) / (double) (Math.max(1, maxSpan * workers.size()));

        DecisionMetrics metrics = new DecisionMetrics(
            totalWait / count,
            totalTurnaround / count,
            (count / (double) Math.max(1, maxSpan)) * 1000.0,
            Math.min(1.0, utilization)
        );

        return new SchedulingDecisionResponse(
            decisionId,
            getPolicyName(),
            getAlgorithmVersion(),
            Instant.now().toString(),
            allocations,
            metrics
        );
    }
}
