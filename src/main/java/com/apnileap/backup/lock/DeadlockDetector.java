package com.apnileap.backup.lock;

import com.apnileap.backup.dto.JobModels.*;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class DeadlockDetector {

    public DeadlockAnalyseResponse analyze(List<WaitForEdge> edges) {
        Map<String, List<String>> graph = new HashMap<>();
        Set<String> allNodes = new HashSet<>();

        if (edges != null) {
            for (WaitForEdge edge : edges) {
                allNodes.add(edge.waiting_holder_id());
                allNodes.add(edge.held_by_holder_id());
                graph.computeIfAbsent(edge.waiting_holder_id(), k -> new ArrayList<>())
                     .add(edge.held_by_holder_id());
            }
        }

        List<List<String>> detectedCycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recStack = new HashSet<>();
        List<String> currentPath = new ArrayList<>();

        for (String node : allNodes) {
            if (!visited.contains(node)) {
                findCycles(node, graph, visited, recStack, currentPath, detectedCycles);
            }
        }

        boolean deadlockDetected = !detectedCycles.isEmpty();
        List<String> victims = new ArrayList<>();
        String action;

        if (deadlockDetected) {
            for (List<String> cycle : detectedCycles) {
                if (!cycle.isEmpty()) {
                    victims.add(cycle.get(cycle.size() - 1));
                }
            }
            action = "DEADLOCK_DETECTED: Abort victim transaction(s) and release corresponding file/worker leases to break circular wait cycle.";
        } else {
            action = "NO_DEADLOCK: Resource allocation graph is acyclic. Safe state verified.";
        }

        return new DeadlockAnalyseResponse(deadlockDetected, detectedCycles, action, victims);
    }

    private void findCycles(
        String u, 
        Map<String, List<String>> graph, 
        Set<String> visited, 
        Set<String> recStack, 
        List<String> currentPath, 
        List<List<String>> cycles
    ) {
        visited.add(u);
        recStack.add(u);
        currentPath.add(u);

        List<String> neighbors = graph.getOrDefault(u, Collections.emptyList());
        for (String v : neighbors) {
            if (!visited.contains(v)) {
                findCycles(v, graph, visited, recStack, currentPath, cycles);
            } else if (recStack.contains(v)) {
                int cycleStartIndex = currentPath.indexOf(v);
                if (cycleStartIndex != -1) {
                    List<String> cycle = new ArrayList<>(currentPath.subList(cycleStartIndex, currentPath.size()));
                    cycle.add(v);
                    cycles.add(cycle);
                }
            }
        }

        recStack.remove(u);
        currentPath.remove(currentPath.size() - 1);
    }
}
