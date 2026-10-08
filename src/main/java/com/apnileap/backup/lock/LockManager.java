package com.apnileap.backup.lock;

import com.apnileap.backup.dto.JobModels.*;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LockManager {

    private record ActiveLease(
        String leaseId,
        String fileId,
        String holderId,
        String lockMode,
        long expiresAtEpochMs
    ) {}

    private final Map<String, ActiveLease> activeLocks = new ConcurrentHashMap<>();
    private final Map<String, String> leaseToFileMap = new ConcurrentHashMap<>();

    public synchronized LockAcquireResponse acquireLock(LockAcquireRequest request) {
        cleanupExpiredLocks();

        String fileId = request.file_id();
        String holderId = request.holder_id();
        String mode = request.lock_mode().toUpperCase();
        long timeoutMs = (request.lease_timeout_ms() != null && request.lease_timeout_ms() > 0) 
            ? request.lease_timeout_ms() : 30_000L;

        ActiveLease existing = activeLocks.get(fileId);
        if (existing != null) {
            if ("EXCLUSIVE".equals(existing.lockMode()) || "EXCLUSIVE".equals(mode)) {
                if (!existing.holderId().equals(holderId)) {
                    return new LockAcquireResponse(
                        false,
                        null,
                        fileId,
                        holderId,
                        mode,
                        0,
                        "File is already locked in " + existing.lockMode() + " mode by holder " + existing.holderId()
                    );
                }
            }
        }

        String leaseId = "lease-" + UUID.randomUUID();
        long expiresAt = System.currentTimeMillis() + timeoutMs;
        ActiveLease lease = new ActiveLease(leaseId, fileId, holderId, mode, expiresAt);

        activeLocks.put(fileId, lease);
        leaseToFileMap.put(leaseId, fileId);

        return new LockAcquireResponse(true, leaseId, fileId, holderId, mode, expiresAt, null);
    }

    public synchronized LockReleaseResponse releaseLock(String leaseId) {
        String fileId = leaseToFileMap.remove(leaseId);
        if (fileId != null) {
            activeLocks.remove(fileId);
            return new LockReleaseResponse(true, leaseId, "Lock lease successfully released");
        }
        return new LockReleaseResponse(false, leaseId, "Lease ID not found or already expired");
    }

    public synchronized int getActiveLocksCount() {
        cleanupExpiredLocks();
        return activeLocks.size();
    }

    private void cleanupExpiredLocks() {
        long now = System.currentTimeMillis();
        List<String> expiredLeases = new ArrayList<>();
        for (ActiveLease lease : activeLocks.values()) {
            if (lease.expiresAtEpochMs() < now) {
                expiredLeases.add(lease.leaseId());
            }
        }
        for (String id : expiredLeases) {
            releaseLock(id);
        }
    }
}
