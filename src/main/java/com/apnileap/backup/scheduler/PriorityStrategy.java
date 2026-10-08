package com.apnileap.backup.scheduler;

import com.apnileap.backup.dto.JobModels.*;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.*;

@Component
public class PriorityStrategy implements SchedulingStrategy {
    @Override
    public String getPolicyName() { return "PRIORITY"; }

    @Override
    public String getAlgorithmVersion() { return "1.0.0-PRIORITY-HEAP"; }

    @Override
    public SchedulingDecisionResponse schedule(List<JobIntakeRequest> jobs, List<String> workers, Long quantumMs) {
        String decisionId = UUID.randomUUID().toString();
        if (workers.isEmpty()) workers = List.of("worker-default-1");

        List<JobIntakeRequest> prioritized = new ArrayList<>(jobs);
        prioritized.sort((a, b) -> Integer.compare(b.priority(), a.priority()));

        long[] workerTime = new long[workers.size()];
        List<AssignedJob> allocations = new ArrayList<>();
        double totalWait = 0, totalTurnaround = 0;

        for (JobIntakeRequest job : prioritized) {
            int workerIdx = 0;
            long minTime = workerTime[0];
            for (int i = 1; i < workers.size(); i++) {
                if (workerTime[i] < minTime) {
                    minTime = workerTime[i];
                    workerIdx = i;
                }
            }

            long duration = job.estimated_duration_ms() != null ? job.estimated_duration_ms() : 500L;
            long start = workerTime[workerIdx];
            long end = start + duration;
            workerTime[workerIdx] = end;

            totalWait += start;
            totalTurnaround += end;

            allocations.add(new AssignedJob(
                job.job_id(),
                job.file_id(),
                List.of(workers.get(workerIdx)),
                start,
                duration
            ));
        }

        int count = Math.max(1, jobs.size());
        long maxSpan = Arrays.stream(workerTime).max().orElse(1L);
        DecisionMetrics metrics = new DecisionMetrics(
            totalWait / count,
            totalTurnaround / count,
            (count / (double) Math.max(1, maxSpan)) * 1000.0,
            Math.min(1.0, (totalTurnaround - totalWait) / (double) (Math.max(1, maxSpan * workers.size())))
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
