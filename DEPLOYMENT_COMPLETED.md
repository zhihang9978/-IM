# Lanxin Communication - Deployment Completion Report

## Executive Summary

All six infrastructure deployment tasks have been successfully completed. The Lanxin Communication system is now deployed with high availability, monitoring, messaging infrastructure, object storage, automated backups, and working database replication.

**Date**: October 16, 2025  
**Deployed by**: Devin AI  
**Deployment Status**: ✅ Complete

---

## 1. Keepalived VIP Configuration ✅

### Status: Completed
Virtual IP (VIP) for automatic failover has been configured on both servers.

### Configuration Details

**Virtual IP (VIP)**: `154.40.45.200/24`  
**Protocol**: VRRP (Virtual Router Redundancy Protocol)  
**Authentication**: Password-based (lanxin2025)

#### Main Server (154.40.45.121)
- **Role**: MASTER
- **Priority**: 100
- **Interface**: eth0
- **Health Check**: `/opt/lanxin/bin/check_backend.sh` (runs every 3 seconds)
- **Config File**: `/etc/keepalived/keepalived.conf`

#### Backup Server (154.40.45.98)
- **Role**: BACKUP
- **Priority**: 90
- **Interface**: eth0
- **Health Check**: `/opt/lanxin/bin/check_backend.sh` (runs every 3 seconds)
- **Config File**: `/etc/keepalived/keepalived.conf`

### Verification

```bash
# Check VIP assignment on main server
ssh root@154.40.45.121 "ip addr show eth0 | grep 154.40.45.200"
# Output: inet 154.40.45.200/24 scope global secondary eth0

# Check Keepalived status
ssh root@154.40.45.121 "systemctl status keepalived"
ssh root@154.40.45.98 "systemctl status keepalived"
```

### Failover Behavior

When the main server (154.40.45.121) fails or the backend health check fails:
1. Keepalived on the main server stops advertising VIP
2. Backup server (154.40.45.98) detects master failure within 3 seconds
3. Backup server automatically acquires VIP 154.40.45.200
4. Users continue accessing services seamlessly through VIP

### Health Check Script

Location: `/opt/lanxin/bin/check_backend.sh`

The script checks:
1. Backend HTTP health endpoint (`http://localhost:8080/health`)
2. Process existence (`lanxin-server`)
3. Response status (must return HTTP 200 with `"status":"ok"`)

---

## 2. Monitoring System Deployment ✅

### Status: Completed
Prometheus + Grafana monitoring stack deployed on dedicated monitoring server.

### Deployment Server
**IP**: 154.37.212.67  
**Hostname**: RainYun-QCPsNPXP

### Components Deployed

#### Prometheus
- **Version**: Latest
- **Port**: 9090
- **Access**: http://154.37.212.67:9090
- **Data Retention**: Persistent volume (`prometheus-data`)
- **Config File**: `/opt/monitoring/prometheus/prometheus.yml`

#### Grafana
- **Version**: Latest  
- **Port**: 3000
- **Access**: http://154.37.212.67:3000
- **Admin Credentials**:
  - Username: `admin`
  - Password: `Admin@2025`
- **Data Source**: Prometheus (auto-configured)
- **Timezone**: Asia/Shanghai
- **Language**: Supports Chinese (中文面板需要在Grafana中手动设置)

#### Node Exporter
- **Deployed on**:
  - Main Server (154.40.45.121:9100)
  - Backup Server (154.40.45.98:9100)
  - Monitoring Server (154.37.212.67:9100)
- **Metrics**: System metrics (CPU, memory, disk, network)

### Monitoring Targets

Prometheus is configured to scrape the following targets:

1. **Prometheus Self-Monitoring**
   - Target: `localhost:9090`
   - Label: `监控服务器`

2. **Main Server Node Metrics**
   - Target: `154.40.45.121:9100`
   - Labels: `instance=主服务器`, `role=master`

3. **Backup Server Node Metrics**
   - Target: `154.40.45.98:9100`
   - Labels: `instance=备份服务器`, `role=backup`

4. **Main Server Backend API** (when metrics endpoint is implemented)
   - Target: `154.40.45.121:8080/metrics`
   - Labels: `instance=主服务器后端`, `service=backend`

5. **Backup Server Backend API** (when metrics endpoint is implemented)
   - Target: `154.40.45.98:8080/metrics`
   - Labels: `instance=备份服务器后端`, `service=backend`

6. **Monitoring Server Node Metrics**
   - Target: `node-exporter:9100` (container network)
   - Labels: `instance=监控服务器`, `role=monitor`

### Docker Deployment

All monitoring services run via Docker Compose:
```bash
# Location
/opt/monitoring/docker-compose.yml

# Management commands
cd /opt/monitoring
docker compose ps          # Check status
docker compose logs -f     # View logs
docker compose restart     # Restart services
```

### Creating Chinese Dashboards in Grafana

1. Access Grafana at http://154.37.212.67:3000
2. Login with admin/Admin@2025
3. Go to Configuration → Preferences
4. Set Language to "中文"
5. Import dashboards:
   - Node Exporter Full (ID: 1860) - 系统监控
   - MySQL Overview (ID: 7362) - 数据库监控
6. Create custom dashboards for Lanxin services

### Alerts Setup (Recommended)

Configure Prometheus alerts for:
- High CPU usage (> 80%)
- High memory usage (> 85%)
- Disk space low (< 10% free)
- Service down (backend unreachable)
- Keepalived failover events

---

## 3. Kafka Message Queue ✅

### Status: Completed
Kafka message queue deployed on main server with ZooKeeper.

### Deployment Details

**Server**: 154.40.45.121 (Main Server)  
**Kafka Port**: 9092  
**ZooKeeper Port**: 2181  
**Docker Image**: wurstmeister/kafka:latest

### Topics Created

1. **lanxin_message**
   - Partitions: 3
   - Replication Factor: 1
   - Purpose: User messages, group messages

2. **lanxin_notification**
   - Partitions: 3
   - Replication Factor: 1
   - Purpose: System notifications, alerts

### Configuration

```yaml
# Docker Compose location
/opt/kafka-compose.yml

# Kafka advertised host
KAFKA_ADVERTISED_HOST_NAME: 154.40.45.121

# Auto-create topics
KAFKA_CREATE_TOPICS: "lanxin_message:3:1,lanxin_notification:3:1"
```

### Verification

```bash
# List topics
ssh root@154.40.45.121 "docker exec kafka kafka-topics.sh --list --bootstrap-server localhost:9092"

# Output:
# lanxin_message
# lanxin_notification

# Check Kafka status
ssh root@154.40.45.121 "docker ps | grep kafka"
```

### Usage in Backend

To integrate Kafka in the Go backend:

```go
// Example Kafka producer
producer, err := kafka.NewProducer(&kafka.ConfigMap{
    "bootstrap.servers": "154.40.45.121:9092",
})

// Send message
producer.Produce(&kafka.Message{
    TopicPartition: kafka.TopicPartition{
        Topic: kafka.StringPointer("lanxin_message"),
        Partition: kafka.PartitionAny,
    },
    Value: []byte(messageJSON),
}, nil)
```

### Management

```bash
# Stop Kafka
ssh root@154.40.45.121 "cd /opt && docker compose -f kafka-compose.yml down"

# Start Kafka
ssh root@154.40.45.121 "cd /opt && docker compose -f kafka-compose.yml up -d"

# View logs
ssh root@154.40.45.121 "docker logs -f kafka"
```

---

## 4. MinIO Object Storage (Self-Hosted COS) ✅

### Status: Completed
MinIO S3-compatible object storage deployed and configured.

### Deployment Details

**Server**: 154.40.45.121 (Main Server)  
**API Port**: 9000  
**Console Port**: 9001  
**Data Directory**: `/data/minio`

### Access Information

**API Endpoint**: http://154.40.45.121:9000  
**Console URL**: http://154.40.45.121:9001  
**Credentials**:
- Access Key: `minioadmin`
- Secret Key: `minioadmin123456`

### Bucket Configuration

**Bucket Name**: `lanxin-files`  
**Access Policy**: Download (public read)  
**Region**: Default

### Backend Integration

The backend configuration has been updated:

```yaml
# /opt/lanxin/config/config.yaml
storage:
  cos:
    secret_id: "minioadmin"
    secret_key: "minioadmin123456"
    bucket: "lanxin-files"
    region: ap-guangzhou
    base_url: "http://154.40.45.121:9000"
```

### Verification

```bash
# Check MinIO status
ssh root@154.40.45.121 "docker ps | grep minio"

# Test bucket access
curl http://154.40.45.121:9000/lanxin-files/

# Test backend configuration
ssh root@154.40.45.121 "systemctl status lanxin"
```

### Usage Examples

**Upload file via MinIO client**:
```bash
docker exec minio mc cp /path/to/file local/lanxin-files/filename
```

**Upload file via backend API**:
The backend's COS client (`apps/backend/pkg/cos/client.go`) is already configured to use MinIO.

### Management

```bash
# Access MinIO console
# Open http://154.40.45.121:9001 in browser
# Login with minioadmin / minioadmin123456

# Restart MinIO
ssh root@154.40.45.121 "docker restart minio"

# View logs
ssh root@154.40.45.121 "docker logs -f minio"
```

---

## 5. Database Backup Strategy ✅

### Status: Completed
Automated MySQL backup scripts deployed with cron scheduling.

### Backup Configuration

**Backup Script**: `/opt/lanxin/bin/backup_mysql.sh`  
**Backup Directory**: `/opt/lanxin/backups/mysql`  
**Schedule**: Daily at 2:00 AM (via cron)  
**Retention**: 7 days (older backups auto-deleted)  
**Compression**: gzip

### Deployed Servers

1. **Main Server (154.40.45.121)**
   - Backs up primary database
   - Cron job: `0 2 * * * /opt/lanxin/bin/backup_mysql.sh`

2. **Backup Server (154.40.45.98)**
   - Backs up replicated database
   - Cron job: `0 2 * * * /opt/lanxin/bin/backup_mysql.sh`

### Backup Process

The script performs:
1. Full database dump using `mysqldump`
2. Includes routines, triggers, events
3. Single transaction (consistent snapshot)
4. Compression with gzip
5. Cleanup of backups older than 7 days
6. Logging to `/opt/lanxin/logs/backup.log`

### Manual Backup

```bash
# Run backup manually
ssh root@154.40.45.121 "export MYSQL_PASSWORD='lanxin@2025' && /opt/lanxin/bin/backup_mysql.sh"

# List backups
ssh root@154.40.45.121 "ls -lh /opt/lanxin/backups/mysql/"

# Example output:
# lanxin_im_20251016_223429.sql.gz
```

### Restore from Backup

```bash
# Decompress backup
gunzip /opt/lanxin/backups/mysql/lanxin_im_YYYYMMDD_HHMMSS.sql.gz

# Restore database
mysql -u lanxin -p lanxin_im < /opt/lanxin/backups/mysql/lanxin_im_YYYYMMDD_HHMMSS.sql
```

### Backup Logs

```bash
# View backup logs
ssh root@154.40.45.121 "tail -f /opt/lanxin/logs/backup.log"

# Example log entry:
# [Thu Oct 16 22:34:29 CST 2025] 数据库备份成功
```

### Recommended Enhancements

1. **Remote Backup Upload**:
   ```bash
   # Uncomment in backup_mysql.sh
   mc cp ${BACKUP_FILE}.gz minio/lanxin-backups/mysql/
   ```

2. **Backup Monitoring**:
   - Configure Prometheus alerts for backup failures
   - Send notifications via email/Slack

3. **Point-in-Time Recovery**:
   - Enable binary log archival
   - Store binlogs alongside full backups

---

## 6. MySQL Replication Status ✅

### Status: Completed and Verified
Master-slave replication is functioning correctly.

### Replication Topology

```
Main Server (154.40.45.121) - MASTER
           ↓
Backup Server (154.40.45.98) - SLAVE
```

### Configuration Details

**Master Server (154.40.45.121)**:
- Binary logging enabled
- Binlog format: ROW
- Server ID: 1
- Current log file: `mysql-bin.000002`
- Current position: 3745

**Slave Server (154.40.45.98)**:
- Server ID: 2
- Replication user: `repl@%`
- Password authentication: `mysql_native_password`
- Relay log: Enabled

### Replication Status

```sql
-- On slave server (154.40.45.98)
SHOW SLAVE STATUS\G

Slave_IO_Running: Yes        ✅
Slave_SQL_Running: Yes       ✅
Seconds_Behind_Master: 0     ✅ (Real-time sync)
Last_IO_Error:               ✅ (No errors)
Last_SQL_Error:              ✅ (No errors)
```

### Issue Resolution

**Problem**: Initial replication failure due to `caching_sha2_password` authentication over insecure connection.

**Solution**: Changed replication user to use `mysql_native_password`:
```sql
ALTER USER 'repl'@'%' IDENTIFIED WITH mysql_native_password BY 'repl@2025';
```

### Verification Commands

```bash
# Check master status
ssh root@154.40.45.121 "mysql -e 'SHOW MASTER STATUS\G'"

# Check slave status
ssh root@154.40.45.98 "mysql -e 'SHOW SLAVE STATUS\G' | grep -E '(Slave_IO_Running|Slave_SQL_Running|Seconds_Behind_Master)'"
```

### Replication Lag Monitoring

The Prometheus monitoring system can be configured to monitor replication lag:

```yaml
# Add to prometheus.yml
- job_name: 'mysql-replication'
  static_configs:
    - targets: ['154.40.45.98:9104']  # MySQL exporter on slave
```

### Failover Process

In case of master failure:

1. **Automatic Traffic Routing**: Keepalived VIP fails over to backup server
2. **Promote Slave to Master**:
   ```sql
   STOP SLAVE;
   RESET MASTER;
   ```
3. **Update Backend Configuration**: Point to new master IP
4. **Restart Backend Service**: `systemctl restart lanxin`

### Monitoring Replication Health

```bash
# Check if replication is running
ssh root@154.40.45.98 "mysql -e 'SHOW SLAVE STATUS\G' | grep Running"

# Expected output:
# Slave_IO_Running: Yes
# Slave_SQL_Running: Yes
```

---

## System Architecture Overview

```
                    ┌─────────────────────────┐
                    │   Monitoring Server     │
                    │   154.37.212.67         │
                    │                         │
                    │  • Prometheus (9090)    │
                    │  • Grafana (3000)       │
                    │  • Node Exporter        │
                    └─────────────────────────┘
                              │
                              │ Monitoring
                    ┌─────────┴─────────┐
                    │                   │
        ┌───────────▼──────────┐   ┌───▼──────────────────┐
        │   Main Server        │   │   Backup Server      │
        │   154.40.45.121      │   │   154.40.45.98       │
        │   (MASTER)           │   │   (SLAVE)            │
        │                      │   │                      │
        │  • Backend (8080)    │   │  • Backend (8080)    │
        │  • MySQL (Master)    │───┼─▶• MySQL (Slave)     │
        │  • Redis (6379)      │   │  • Redis (6379)      │
        │  • Kafka (9092)      │   │  • Keepalived        │
        │  • MinIO (9000/9001) │   │  • Node Exporter     │
        │  • Keepalived (VIP)  │   │  • Daily Backup      │
        │  • Node Exporter     │   │                      │
        │  • Daily Backup      │   │                      │
        └──────────────────────┘   └──────────────────────┘
                    │
                    │ VIP: 154.40.45.200
                    │
        ┌───────────▼───────────────────────┐
        │   Client Applications             │
        │  • Admin Web                      │
        │  • Android App                    │
        │  • API Consumers                  │
        └───────────────────────────────────┘
```

---

## Service Endpoints

### Production Endpoints

| Service | URL | Credentials |
|---------|-----|-------------|
| Backend API (VIP) | http://154.40.45.200:8080 | JWT tokens |
| Backend API (Main) | http://154.40.45.121:8080 | JWT tokens |
| Backend API (Backup) | http://154.40.45.98:8080 | JWT tokens |
| Grafana | http://154.37.212.67:3000 | admin / Admin@2025 |
| Prometheus | http://154.37.212.67:9090 | No auth |
| MinIO Console | http://154.40.45.121:9001 | minioadmin / minioadmin123456 |
| MinIO API | http://154.40.45.121:9000 | S3 credentials |

### Health Checks

| Service | Endpoint | Expected Response |
|---------|----------|-------------------|
| Backend | http://154.40.45.121:8080/health | `{"status":"ok"}` |
| Prometheus | http://154.37.212.67:9090/-/healthy | `Prometheus Server is Healthy.` |
| Grafana | http://154.37.212.67:3000/api/health | `{"database":"ok"}` |

---

## Operational Procedures

### Starting Services

```bash
# Main Server
ssh root@154.40.45.121 "systemctl start lanxin && \
  docker start kafka zookeeper minio && \
  systemctl start keepalived"

# Backup Server
ssh root@154.40.45.98 "systemctl start lanxin && \
  systemctl start keepalived"

# Monitoring Server
ssh root@154.37.212.67 "cd /opt/monitoring && docker compose up -d"
```

### Stopping Services

```bash
# Main Server
ssh root@154.40.45.121 "systemctl stop lanxin && \
  docker stop kafka zookeeper minio && \
  systemctl stop keepalived"

# Backup Server
ssh root@154.40.45.98 "systemctl stop lanxin && \
  systemctl stop keepalived"

# Monitoring Server
ssh root@154.37.212.67 "cd /opt/monitoring && docker compose down"
```

### Viewing Logs

```bash
# Backend logs (Main)
ssh root@154.40.45.121 "journalctl -u lanxin -f"

# Kafka logs
ssh root@154.40.45.121 "docker logs -f kafka"

# MinIO logs
ssh root@154.40.45.121 "docker logs -f minio"

# Monitoring stack logs
ssh root@154.37.212.67 "cd /opt/monitoring && docker compose logs -f"

# Keepalived logs
ssh root@154.40.45.121 "journalctl -u keepalived -f"
```

---

## Security Considerations

### Credentials Summary

**MySQL**:
- Root password: (server-specific)
- Application user: `lanxin` / `lanxin@2025`
- Replication user: `repl` / `repl@2025`

**Redis**:
- Password: (configured in `/opt/lanxin/config/config.yaml`)

**MinIO**:
- Access Key: `minioadmin`
- Secret Key: `minioadmin123456`

**Grafana**:
- Admin: `admin` / `Admin@2025`

**Keepalived**:
- VRRP password: `lanxin2025`

### Recommended Security Hardening

1. **Change Default Passwords**:
   - MinIO admin credentials
   - Grafana admin password
   - Keepalived VRRP password

2. **Enable Firewall**:
   ```bash
   ufw allow 8080/tcp   # Backend API
   ufw allow 9090/tcp   # Prometheus (restrict to admin IPs)
   ufw allow 3000/tcp   # Grafana (restrict to admin IPs)
   ufw allow 9001/tcp   # MinIO Console (restrict to admin IPs)
   ufw enable
   ```

3. **SSL/TLS Configuration**:
   - Deploy SSL certificates for backend API
   - Enable HTTPS for Grafana
   - Use secure MinIO endpoint

4. **Database Security**:
   - Bind MySQL to localhost only
   - Use SSL for replication
   - Rotate replication user password

---

## Testing and Validation

### Infrastructure Tests

```bash
# Test 1: Keepalived VIP
ping 154.40.45.200
curl http://154.40.45.200:8080/health

# Test 2: Monitoring stack
curl http://154.37.212.67:9090/-/healthy
curl http://154.37.212.67:3000/api/health

# Test 3: Kafka
ssh root@154.40.45.121 "docker exec kafka kafka-topics.sh --list --bootstrap-server localhost:9092"

# Test 4: MinIO
curl http://154.40.45.121:9000/minio/health/live

# Test 5: Database backup
ssh root@154.40.45.121 "ls -lh /opt/lanxin/backups/mysql/"

# Test 6: MySQL replication
ssh root@154.40.45.98 "mysql -e 'SHOW SLAVE STATUS\G' | grep Running"
```

### Failover Test

```bash
# Simulate main server failure
ssh root@154.40.45.121 "systemctl stop lanxin"

# Check VIP migration (should move to backup server)
ssh root@154.40.45.98 "ip addr show eth0 | grep 154.40.45.200"

# Test API availability through VIP
curl http://154.40.45.200:8080/health

# Restore main server
ssh root@154.40.45.121 "systemctl start lanxin"
```

---

## Troubleshooting Guide

### Issue: VIP not assigned

```bash
# Check Keepalived status
systemctl status keepalived

# Check logs
journalctl -u keepalived -n 50

# Verify VRRP traffic
tcpdump -i eth0 vrrp
```

### Issue: MySQL replication stopped

```bash
# Check slave status
mysql -e 'SHOW SLAVE STATUS\G'

# Common fixes
mysql -e 'STOP SLAVE; START SLAVE;'

# Skip one error (if safe)
mysql -e 'SET GLOBAL sql_slave_skip_counter=1; START SLAVE;'
```

### Issue: Kafka not responding

```bash
# Check container status
docker ps | grep kafka

# Restart Kafka stack
cd /opt && docker compose -f kafka-compose.yml restart

# Check logs
docker logs kafka --tail 100
```

### Issue: Monitoring targets down

```bash
# Check Prometheus targets
curl http://154.37.212.67:9090/api/v1/targets

# Restart node-exporter on target server
docker restart node-exporter

# Check firewall
ufw status
```

---

## Next Steps

### 1. Application Deployment

Deploy the Lanxin backend application:
```bash
# Build Go binary
cd /home/ubuntu/lanxin-im/apps/backend
go build -o lanxin-server cmd/server/main.go

# Deploy to servers
scp lanxin-server root@154.40.45.121:/opt/lanxin/bin/
scp lanxin-server root@154.40.45.98:/opt/lanxin/bin/
```

### 2. Frontend Deployment

Build and deploy admin web interface:
```bash
# Build React app
cd /home/ubuntu/lanxin-im/apps/admin-web
pnpm build

# Deploy to web server (Nginx)
scp -r dist/* root@154.40.45.121:/var/www/lanxin-admin/
```

### 3. Mobile App Release

Build and distribute Android app:
```bash
cd /home/ubuntu/lanxin-im/apps/android
./gradlew assembleRelease
```

### 4. Monitoring Configuration

- Create Grafana dashboards for Lanxin services
- Configure alert rules in Prometheus
- Set up notification channels (email, Slack, WeChat)

### 5. Performance Tuning

- Configure MySQL query cache and buffer pools
- Optimize Redis persistence (AOF vs RDB)
- Tune Kafka retention and compaction policies

---

## Maintenance Schedule

| Task | Frequency | Command |
|------|-----------|---------|
| Check backup logs | Daily | `tail /opt/lanxin/logs/backup.log` |
| Review Grafana dashboards | Daily | Open http://154.37.212.67:3000 |
| Verify replication lag | Daily | `mysql -e 'SHOW SLAVE STATUS\G'` |
| Test failover | Weekly | Simulate main server failure |
| Update Docker images | Monthly | `docker compose pull && docker compose up -d` |
| Review security logs | Monthly | `journalctl --since "1 month ago"` |
| Rotate credentials | Quarterly | Update all passwords |

---

## Contact and Support

**Project**: Lanxin Communication System  
**Repository**: zhihang9978/-IM  
**Deployment Date**: October 16, 2025  
**Documentation**: https://github.com/zhihang9978/-IM

For issues or questions, refer to:
- Project README: `/home/ubuntu/lanxin-im/README.md`
- Deployment checklist: `/home/ubuntu/lanxin-im/DEPLOYMENT_CHECKLIST_FOR_DEVIN.txt`
- This document: `/home/ubuntu/lanxin-im/DEPLOYMENT_COMPLETED.md`

---

## Appendix: Quick Reference Commands

```bash
# Server access
ssh root@154.40.45.121  # Main server (password: vhISARnUo7sO4QnK)
ssh root@154.40.45.98   # Backup server (password: FKbnW5cd5GLMHXAb)
ssh root@154.37.212.67  # Monitoring server (password: QcTzmHVsWPde6A7o)

# Service status checks
systemctl status lanxin keepalived
docker ps
mysql -e 'SHOW SLAVE STATUS\G'

# View all services
watch -n 5 'echo "=== Main Server ===" && ssh root@154.40.45.121 "docker ps --format \"table {{.Names}}\t{{.Status}}\"" && echo && echo "=== Backup Server ===" && ssh root@154.40.45.98 "docker ps --format \"table {{.Names}}\t{{.Status}}\""'

# Monitoring URLs
# Grafana: http://154.37.212.67:3000 (admin/Admin@2025)
# Prometheus: http://154.37.212.67:9090
# MinIO: http://154.40.45.121:9001 (minioadmin/minioadmin123456)
```

---

**END OF DEPLOYMENT REPORT**
