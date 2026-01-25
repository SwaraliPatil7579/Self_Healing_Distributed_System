# Health Monitor Testing Guide

## Start All Services

### Terminal 1 - Health Monitor
```bash
cd health-monitor
mvn spring-boot:run
```

### Terminal 2 - Service A
```bash
cd service-a
mvn spring-boot:run
```

### Terminal 3 - Service B
```bash
cd service-b
mvn spring-boot:run
```

### Terminal 4 - Service C
```bash
cd service-c
mvn spring-boot:run
```

## Test URLs

### Check Health Monitor
http://localhost:8080/monitor/health

### View All Services
http://localhost:8080/monitor/services

### Check Individual Services
- http://localhost:8081/health (Service A)
- http://localhost:8082/health (Service B)
- http://localhost:8083/health (Service C)

## Register Services (PowerShell)
```powershell
# Register Service A
Invoke-RestMethod -Uri "http://localhost:8080/monitor/register" -Method POST -ContentType "application/json" -Body '{"serviceName":"service-a","host":"localhost","port":8081}'

# Register Service B
Invoke-RestMethod -Uri "http://localhost:8080/monitor/register" -Method POST -ContentType "application/json" -Body '{"serviceName":"service-b","host":"localhost","port":8082}'

# Register Service C
Invoke-RestMethod -Uri "http://localhost:8080/monitor/register" -Method POST -ContentType "application/json" -Body '{"serviceName":"service-c","host":"localhost","port":8083}'
```

## What You Should See

1. Health Monitor shows 3 registered services
2. Each service shows HEALTHY status
3. Last heartbeat times are visible
4. All services respond to /health endpoint