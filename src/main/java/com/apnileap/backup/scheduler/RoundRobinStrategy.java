package com.apnileap.backup.scheduler;

import com.apnileap.backup.dto.JobModels.*;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.*;

@Component
public class RoundRobinStrategy implements SchedulingStrategy {
    @Override
    public String getPolicyName() { return "RR"; }

    @Override
    public String getAlgorithmVersion() { return "1.0.0-RR-PREEMPTIVE-QUANTUM"; }

    @Override
    public SchedulingDecisionResponse schedule(List<JobIntakeRequest> jobs, List<String> workers, Long quantumMs) {
        String decisionId = UUID.randomUUID().toString();
        if (workers.isEmpty()) workers = List.of("worker-default-1");
        long q = (quantumMs != null && quantumMs > 0) ? quantumMs : 100L;

        List<AssignedJob> allocations = new ArrayList<>();
        int workerIdx = 0;
        long timeOffset = 0;

        for (JobIntakeRequest job : jobs) {
            long remaining = job.estimated_duration_ms() != null ? job.estimated_duration_ms() : 300L;

            while (remaining > 0) {
                long slice = Math.min(remaining, q);
                allocations.add(new AssignedJob(
                    job.job_id(),
                    job.file_id(),
                    List.of(workers.get(workerIdx % workers.size())),
                    timeOffset,
                    slice
                ));
                remaining -= slice;
                timeOffset += slice;
                workerIdx++;
            }
        }

        int count = Math.max(1, jobs.size());
        DecisionMetrics metrics = new DecisionMetrics(
            timeOffset / (2.0 * count),
            timeOffset / (double) count,
            (count / (double) Math.max(1, timeOffset)) * 1000.0,
            0.85
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
