package com.apnileap.backup.scheduler;

import com.apnileap.backup.dto.JobModels.*;
import java.util.List;

public interface SchedulingStrategy {
    String getPolicyName();
    String getAlgorithmVersion();
    SchedulingDecisionResponse schedule(
        List<JobIntakeRequest> jobs, 
        List<String> workers, 
        Long quantumMs
    );
}
