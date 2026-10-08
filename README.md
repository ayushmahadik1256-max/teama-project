# Team A — Backup Job Scheduler (OS Engine)
Part of the APNILEAP Smart File Backup System.

## Architecture & Endpoints Overview
This service provides:
1. `POST /api/v1/backup/jobs` — Enqueue backup job (with Idempotency-Key support)
2. `GET /api/v1/backup/jobs/{jobId}` — Read lifecycle state and allocation result
3. `GET /api/v1/workers/{workerId}/queue` — Worker queue status & start estimates
4. `POST /internal/v1/scheduler/run` — Run pluggable policies (FCFS, SJF, RR, PRIORITY)
5. `GET /api/v1/backup/decisions/{decisionId}` — Fetch scheduling decision and metrics
6. `POST /internal/v1/locks/acquire` — Acquire worker/file lease (Exclusive / Shared)
7. `DELETE /internal/v1/locks/{leaseId}` — Release reservation
8. `POST /internal/v1/deadlocks/analyse` — Wait-for graph cycle detection & victim selection
9. `GET /api/v1/metrics/backup` — Algorithm and runtime metrics
10. `GET /api/v1/stream/backup` — Real-time Server-Sent Events (SSE) feed

## How to Run Locally and Bind to an IP Address

### 1. Prerequisites
- Java 17 or higher
- Maven 3.8+

### 2. Finding Your Local IP Address
- **Windows**: Open Command Prompt / PowerShell and run:
  ```cmd
  ipconfig
  ```
  Look for your IPv4 Address (e.g., `192.168.1.15`).

- **macOS / Linux**: Open Terminal and run:
  ```bash
  ifconfig -a
  # OR
  ip a
  ```
  Look for your active network interface (e.g., `eth0`, `en0`, `wlan0`) IP (e.g., `192.168.1.15`).

### 3. Running with Spring Boot
By default in `src/main/resources/application.properties`, we have configured:
```properties
server.port=8081
server.address=0.0.0.0
```
`0.0.0.0` ensures the server listens on **all** local network interfaces (localhost `127.0.0.1` as well as your LAN IP address `192.168.x.x`).

#### Option A: Run via Maven
```bash
mvn clean spring-boot:run
```

#### Option B: Build JAR and Run with specific IP override
```bash
mvn clean package -DskipTests
java -jar target/team-a-scheduler-1.0.0.jar --server.port=8081 --server.address=0.0.0.0
```

### 4. Testing Endpoints via Local IP
Replace `192.168.1.15` with your machine's IP address:

```bash
# 1. Enqueue Job
curl -X POST http://192.168.1.15:8081/api/v1/backup/jobs \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: 7a8b-9c0d" \
  -H "Idempotency-Key: idem-1001" \
  -d '{
    "job_id": "job-101",
    "user_id": "user-202",
    "file_id": "file-303",
    "backup_type": "FULL",
    "source_path": "/var/data/finance.db",
    "priority": 5,
    "submitted_at": "2026-09-30T10:00:00Z"
  }'

# 2. Check Metrics
curl -X GET http://192.168.1.15:8081/api/v1/metrics/backup

# 3. Acquire File Lease
curl -X POST http://192.168.1.15:8081/internal/v1/locks/acquire \
  -H "Content-Type: application/json" \
  -d '{
    "file_id": "file-303",
    "holder_id": "holder-tx-1",
    "lock_mode": "EXCLUSIVE",
    "lease_timeout_ms": 60000
  }'
```
