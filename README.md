# 医院信息管理系统 (HIS)

[![Java](https://img.shields.io/badge/Java-17%2B-orange?style=flat-square&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

---

## 项目简介

HIS (医院信息管理系统) 是一套基于 Spring Boot 3 构建的企业级医疗信息管理后端服务。系统提供完整的 RESTful API，数字化和优化医院业务流程，包括患者挂号、门诊就诊、电子病历、处方管理、收费结算和药房发药等核心功能。

### 核心特性

- **安全优先**: 基于 JWT 的认证机制，防止水平权限越权访问
- **审计合规**: 180天日志保留期，符合 HIPAA/等保三级要求
- **工作流驱动**: 5个专门工作站（医生、护士、药师、收费员、管理员）
- **质量保证**: 77%整体测试覆盖率，严格的 CI/CD 流程
- **生产就绪**: 内置监控、指标和性能优化

### 架构说明

前后端分离架构，提供纯 RESTful API，支持独立前端开发（React/Vue等），通过安全的后端服务进行数据交互。

---

## 目录

### 第一部分：基础信息

- [项目简介](#项目简介) | [系统架构](#系统架构) | [安全架构](#安全架构) | [技术栈](#技术栈)

### 第二部分：运维核心

- [部署指南](#部署指南)
  - 环境要求 | 生产配置 | 健康检查

- [监控与故障排查](#监控与故障排查)
  - 应用监控 | 关键监控指标 (JVM / 性能 / 业务)
  - 结构化日志 | 常见问题与解决方案 (6个典型案例)
  - 告警配置 (邮件 / Prometheus)

- [性能调优](#性能调优)
  - JVM 调优 (内存 / GC) | 数据库调优 (连接池 / 查询 / 索引)
  - 应用调优 (缓存 / 并发) | 容量规划

### 第三部分：开发支持

- [快速开始](#快速开始) | [开发指南](#开发指南)

### 第四部分：其他

- [许可与支持](#许可与支持)

---

## 系统架构

系统采用分层架构，按工作站组织 API：

```
┌─────────────────────────────────────────────────────────────┐
│                     前端应用程序                             │
│              (React/Vue - 独立部署)                          │
└──────────────────────┬──────────────────────────────────────┘
                       │ RESTful API (JWT 认证)
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                   API 网关层                                 │
│  ┌─────────┬─────────┬─────────┬─────────┬──────────────┐  │
│  │ /auth   │/doctor  │/nurse   │/pharmacist│/cashier     │  │
│  │ 认证    │ 医生    │ 护士    │ 药师      │ 收费员      │  │
│  └─────────┴─────────┴─────────┴─────────┴──────────────┘  │
└──────────────────────┬──────────────────────────────────────┘
                       │
       ┌───────────────┼───────────────┐
       ▼               ▼               ▼
┌──────────────┐ ┌──────────┐ ┌──────────────┐
│  控制器层    │→│ 服务层   │→│ 数据访问层   │
│  Controller  │ │ Service  │ │ Repository   │
└──────────────┘ └──────────┘ └──────────────┘
                                         │
                                         ▼
                                ┌────────────────┐
                                │  PostgreSQL 14 │
                                │  + Flyway      │
                                └────────────────┘
```

### 支撑基础设施

- Spring Security 6 (JWT 认证)
- Micrometer + Prometheus (指标监控)
- Knife4j (API 文档)
- 审计日志 (Trace ID 追踪)

---

## 安全架构

### 认证与授权

- **JWT 无状态认证**: Spring Security 6，24小时令牌有效期
- **基于角色的访问控制 (RBAC)**: 5种角色，精细化权限管理
  - `ADMIN` - 超级管理员
  - `DOCTOR` - 医生工作站
  - `NURSE` - 护士工作站
  - `PHARMACIST` - 药师工作站
  - `CASHIER` - 收费员工作站
- **方法级安全**: 所有控制器使用 `@PreAuthorize` 注解
- **防水平权限越权**: 用户身份从 JWT 提取，不信任请求参数

### 数据保护

- **敏感数据脱敏**: 自动脱敏手机号、身份证、地址
  - 手机号: `138****8000`
  - 身份证: `110101********1234`
  - 地址: 保留省市，详细地址脱敏

### 审计与合规

- **全面审计日志**:
  - 180天保留期（通过 `audit.log.retention.days` 配置）
  - Trace ID 分布式追踪
  - 异步日志记录，保证性能
  - 分类记录业务/系统/安全事件
- **合规性**: 符合 HIPAA、中国等保三级标准

---

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| **Java** | 17+ | 编程语言 |
| **Spring Boot** | 3.2.1 | 应用框架 |
| **Spring Security** | 6.x | 认证与授权 |
| **PostgreSQL** | 14+ | 主数据库 |
| **Flyway** | 9.22.3 | 数据库迁移 |

---

## 部署指南

### 环境要求

- **操作系统**: Ubuntu 20.04+ / CentOS 8+ / Windows Server 2019+
- **Java**: OpenJDK 17 或 Oracle JDK 17
- **PostgreSQL**: 14.x，UTF-8 编码
- **内存**: 最低 2GB RAM（推荐 4GB）
- **磁盘**: 10GB 可用空间

### 生产环境配置

#### 1. 环境变量

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_HOST=your-db-host
export DB_PASSWORD=secure-password
export JWT_SECRET=your-256-bit-secret-key
```

#### 2. 生产配置 (`application-prod.yml`)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/his_prod
    username: his_user
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      connection-timeout: 30000
      minimum-idle: 5
      idle-timeout: 600000
      max-lifetime: 1800000

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000  # 24小时

audit:
  log:
    retention:
      days: 180  # 6个月（HIPAA 合规）
```

#### 3. 构建可部署制品

```bash
# 克隆仓库
git clone https://github.com/Bernardyao/HMS-backend.git
cd his

# 构建优化的 JAR
./gradlew clean build -x test

# 输出：build/libs/his-0.0.1-SNAPSHOT.jar (~70MB)
```

#### 4. Systemd 服务（Linux）

```bash
# 创建服务文件
sudo nano /etc/systemd/system/his.service
```

```ini
[Unit]
Description=HIS Application
After=network.target postgresql.service

[Service]
Type=simple
User=his
Group=his
WorkingDirectory=/opt/his
Environment="SPRING_PROFILES_ACTIVE=prod"
EnvironmentFile=/opt/his/.env
ExecStart=/usr/bin/java -Xmx2g -Xms2g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/his/ \
     -jar /opt/his/his-0.0.1-SNAPSHOT.jar
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=his-app

[Install]
WantedBy=multi-user.target
```

```bash
# 创建运行用户
sudo useradd -r -s /bin/false his

# 创建部署目录
sudo mkdir -p /opt/his /var/log/his
sudo chown -R his:his /opt/his /var/log/his

# 复制 JAR 文件
sudo cp build/libs/his-0.0.1-SNAPSHOT.jar /opt/his/
sudo chown his:his /opt/his/his-0.0.1-SNAPSHOT.jar

# 创建环境变量文件
sudo nano /opt/his/.env
```

```bash
# .env 文件内容
DB_HOST=localhost:5432
DB_PASSWORD=your-secure-password
JWT_SECRET=your-256-bit-secret-key-minimum-length
```

```bash
# 设置权限
sudo chown his:his /opt/his/.env
sudo chmod 640 /opt/his/.env

# 启用并启动服务
sudo systemctl daemon-reload
sudo systemctl enable his
sudo systemctl start his
sudo systemctl status his
```

#### 5. Nginx 反向代理

```bash
sudo nano /etc/nginx/sites-available/his
```

```nginx
upstream his_backend {
    server localhost:8080;
}

# HTTP 重定向到 HTTPS（生产环境推荐）
server {
    listen 80;
    server_name his.yourhospital.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name his.yourhospital.com;

    # SSL 证书配置
    ssl_certificate /etc/letsencrypt/live/his.yourhospital.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/his.yourhospital.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_prefer_server_ciphers on;
    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384;

    # 安全头
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";

    client_max_body_size 10M;

    location / {
        proxy_pass http://his_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 健康检查端点无需认证
    location /actuator/health {
        proxy_pass http://his_backend;
        proxy_set_header Host $host;
        access_log off;
    }
}
```

```bash
# 启用站点
sudo ln -s /etc/nginx/sites-available/his /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

#### 6. 数据库迁移

```bash
# Flyway 会在应用启动时自动迁移
# 手动验证迁移状态：
flyway -configFiles=/etc/flyway/flyway.conf info

# 手动执行迁移：
flyway -configFiles=/etc/flyway/flyway.conf migrate
```

#### 7. 数据库备份策略

**自动化备份脚本** (`/opt/his/scripts/backup.sh`)：

```bash
#!/bin/bash

BACKUP_DIR="/var/backups/his"
DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="his_prod"
DB_USER="his_user"

# 创建备份目录
mkdir -p $BACKUP_DIR

# 全量备份
pg_dump -U $DB_USER -h localhost -d $DB_NAME -F c -b -v -f $BACKUP_DIR/his_backup_$DATE.dump

# 压缩备份
gzip $BACKUP_DIR/his_backup_$DATE.dump

# 删除30天前的备份
find $BACKUP_DIR -name "*.gz" -mtime +30 -delete

# 记录日志
echo "Backup completed at $DATE" >> /var/log/his/backup.log
```

```bash
# 设置定时任务（每天凌晨2点）
crontab -e
# 添加以下行
0 2 * * * /opt/his/scripts/backup.sh
```

**数据恢复**：

```bash
# 停止应用
sudo systemctl stop his

# 恢复数据库
gunzip his_backup_YYYYMMDD_HHMMSS.dump.gz
pg_restore -U his_user -h localhost -d his_prod -v his_backup_YYYYMMDD_HHMMSS.dump

# 启动应用
sudo systemctl start his
```

#### 8. 应用升级流程

```bash
# 1. 备份当前版本
sudo cp /opt/his/his-0.0.1-SNAPSHOT.jar /opt/his/his-0.0.1-SNAPSHOT.jar.backup

# 2. 备份数据库
/opt/his/scripts/backup.sh

# 3. 下载新版本
git pull origin main
./gradlew clean build -x test

# 4. 停止服务
sudo systemctl stop his

# 5. 部署新版本
sudo cp build/libs/his-0.0.1-SNAPSHOT.jar /opt/his/

# 6. 启动服务
sudo systemctl start his

# 7. 验证健康状态
curl -f http://localhost:8080/actuator/health || exit 1

# 8. 如回滚
sudo systemctl stop his
sudo cp /opt/his/his-0.0.1-SNAPSHOT.jar.backup /opt/his/his-0.0.1-SNAPSHOT.jar
sudo systemctl start his
```

### 健康检查

```bash
# 应用整体健康状态
curl http://localhost:8080/actuator/health

# 数据库连接状态
curl http://localhost:8080/actuator/health/db

# 序列生成器健康状态
curl http://localhost:8080/actuator/health/sequenceGenerator

# 检查服务状态
sudo systemctl status his

# 查看应用日志
sudo journalctl -u his -f
```

---

## 监控与故障排查

### 应用监控

**Spring Boot Actuator 端点**:

| 端点 | 说明 | 访问权限 |
|----------|-------------|--------|
| `/actuator/health` | 应用健康状态 | 公开 |
| `/actuator/health/db` | 数据库健康状态 | 公开 |
| `/actuator/metrics` | 所有指标 | 需认证 |
| `/actuator/prometheus` | Prometheus 抓取 | 需认证 |
| `/actuator/info` | 应用信息 | 公开 |
| `/actuator/loggers` | 日志级别管理 | 需认证 |

### 关键监控指标

#### JVM 指标

```bash
# JVM 堆内存使用量
curl http://localhost:8080/actuator/metrics/jvm.memory.used?tag=area:heap

# JVM 堆内存最大值
curl http://localhost:8080/actuator/metrics/jvm.memory.max?tag=area:heap

# GC 次数和耗时
curl http://localhost:8080/actuator/metrics/jvm.gc.pause
```

**告警阈值**:
- 堆内存使用率 > 80%: 警告
- 堆内存使用率 > 90%: 严重
- GC 耗时 > 1000ms: 警告

#### 应用性能指标

```bash
# HTTP 请求计数
curl http://localhost:8080/actuator/metrics/http.server.requests

# HTTP 请求延迟（P50, P95, P99）
curl http://localhost:8080/actuator/metrics/http.server.requests?tag=uri:/api/doctor/patients&statistic=percentile

# 活跃数据库连接数
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active

# 数据库连接等待时间
curl http://localhost:8080/actuator/metrics/hikaricp.connections.creation
```

**告警阈值**:
- HTTP P95 延迟 > 500ms: 警告
- HTTP P95 延迟 > 1000ms: 严重
- 活跃连接数 > 15: 警告
- 活跃连接数 = 20: 严重（连接池耗尽）

#### 业务指标

```bash
# 序列生成时间
curl http://localhost:8080/actuator/metrics/sequence.generation.duration

# 收费创建时间
curl http://localhost:8080/actuator/metrics/charge.creation.duration
```

### 结构化日志

**日志级别**:

- **ERROR**: 需要立即关注的应用错误
- **WARN**: 业务规则违规（如库存不足）、性能问题
- **INFO**: 业务操作（如用户登录、处方创建）
- **DEBUG**: 详细执行流程（仅开发/测试环境）

**Trace ID 追踪**:

每个请求获得唯一的 Trace ID 用于分布式追踪：

```
2025-01-04 10:23:45.123 INFO [his-app,trace-id-123,span-id-456] 12345 --- [nio-8080-exec-1] c.h.controller.DoctorController : 开始接诊挂号单 456
```

**日志位置**:

- **开发环境**: 控制台 stdout
- **生产环境**: `/var/log/his/`
  - `his-application.log`（当天）
  - `his-application-2025-01-03.log.gz`（已归档）

**查看日志**:

```bash
# 实时查看应用日志
sudo journalctl -u his -f

# 查看特定时间段日志
sudo journalctl -u his --since "2025-01-04 10:00:00" --until "2025-01-04 11:00:00"

# 过滤错误日志
sudo journalctl -u his -p err

# 搜索特定 Trace ID
sudo journalctl -u his | grep trace-id-123
```

### 常见问题与解决方案

#### 问题 1: 应用无法启动

**症状**:
```bash
sudo systemctl start his
# 服务启动失败
```

**排查步骤**:

1. **查看服务状态**:
   ```bash
   sudo systemctl status his
   ```

2. **查看详细日志**:
   ```bash
   sudo journalctl -u his -n 50 --no-pager
   ```

3. **常见原因**:
   - 端口 8080 被占用: `sudo netstat -tulpn | grep 8080`
   - 数据库连接失败: 检查 `/opt/his/.env` 配置
   - JVM 内存不足: 调整 `-Xmx` 参数
   - JAR 文件权限问题: `ls -l /opt/his/his-*.jar`

**解决方案**:

```bash
# 端口被占用
sudo lsof -ti:8080 | xargs kill -9

# 数据库连接失败
sudo -u postgres psql -c "ALTER USER his_user PASSWORD 'new_password';"

# JVM 内存不足
# 编辑服务文件，增加内存
sudo nano /etc/systemd/system/his.service
# 修改 ExecStart 中的 -Xmx2g 为 -Xmx4g
sudo systemctl daemon-reload
sudo systemctl restart his
```

#### 问题 2: PostgreSQL "连接被拒绝"

**症状**:
```
org.postgresql.util.PSQLException: Connection to localhost:5432 refused
```

**排查步骤**:

1. **验证 PostgreSQL 运行状态**:
   ```bash
   sudo systemctl status postgresql
   ```

2. **检查端口监听**:
   ```bash
   sudo netstat -tulpn | grep 5432
   ```

3. **测试连接**:
   ```bash
   psql -U his_user -h localhost -d his_prod
   ```

**解决方案**:

```bash
# 启动 PostgreSQL
sudo systemctl start postgresql
sudo systemctl enable postgresql

# 检查防火墙
sudo ufw allow 5432/tcp
# 或 CentOS/RHEL
sudo firewall-cmd --permanent --add-port=5432/tcp
sudo firewall-cmd --reload

# 检查 pg_hba.conf 配置
sudo nano /etc/postgresql/14/main/pg_hba.conf
# 确保有如下行：
# host    his_prod    his_user    127.0.0.1/32    scram-sha-256

# 重启 PostgreSQL
sudo systemctl restart postgresql
```

#### 问题 3: 响应缓慢，性能下降

**症状**: P95 延迟超过 1000ms

**排查步骤**:

1. **检查系统资源**:
   ```bash
   # CPU 使用率
   top

   # 内存使用
   free -h

   # 磁盘 I/O
   iostat -x 1

   # 数据库连接数
   sudo -u postgres psql -c "SELECT count(*) FROM pg_stat_activity;"
   ```

2. **检查应用指标**:
   ```bash
   # HTTP 请求延迟
   curl http://localhost:8080/actuator/metrics/http.server.requests

   # 数据库连接池
   curl http://localhost:8080/actuator/metrics/hikaricp.connections.active

   # JVM GC 情况
   curl http://localhost:8080/actuator/metrics/jvm.gc.pause
   ```

3. **慢查询分析**:
   ```sql
   -- 查看当前运行的查询
   SELECT pid, now() - query_start as duration, query
   FROM pg_stat_activity
   WHERE state = 'active'
   ORDER BY duration DESC;

   -- 查看慢查询（需要启用 pg_stat_statements）
   SELECT query, calls, total_time, mean_time
   FROM pg_stat_statements
   ORDER BY mean_time DESC
   LIMIT 10;
   ```

**解决方案**:

```bash
# 1. 重启应用释放资源
sudo systemctl restart his

# 2. 调整数据库连接池
sudo nano /opt/his/application-prod.yml
# 增加 spring.datasource.hikari.maximum-pool-size: 30

# 3. JVM 参数优化
sudo nano /etc/systemd/system/his.service
# 调整 GC 参数:
# -XX:MaxGCPauseMillis=100
# -XX:+UseStringDeduplication

# 4. 数据库优化
sudo -u postgres psql -d his_prod
-- 分析慢查询表
ANALYZE registration;
ANALYZE prescription;
ANALYZE medicine;

-- 重建索引
REINDEX TABLE CONCURRENTLY medicine;
```

#### 问题 4: "库存不足"错误

**症状**:
```
BusinessException: 药品库存不足
```

**解决方案**:

```bash
# 1. 查看当前库存
sudo -u postgres psql -d his_prod -c "SELECT id, name, stock FROM medicine WHERE stock < 100;"

# 2. 调整库存（记录操作）
sudo -u postgres psql -d his_prod
BEGIN;
UPDATE medicine SET stock = stock + 100 WHERE id = ?;
-- 记录审计日志
INSERT INTO audit_log (operation_type, table_name, record_id, details)
VALUES ('UPDATE', 'medicine', ?, '库存调整: +100');
COMMIT;

# 3. 设置库存预警
sudo -u postgres psql -d his_prod -c "
CREATE OR REPLACE FUNCTION low_stock_alert() RETURNS trigger AS $$
BEGIN
  IF NEW.stock < 100 THEN
    RAISE NOTICE '库存预警: 药品 % 库存为 %', NEW.name, NEW.stock;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS stock_alert_trigger ON medicine;
CREATE TRIGGER stock_alert_trigger
AFTER UPDATE ON medicine
FOR EACH ROW
EXECUTE FUNCTION low_stock_alert();
"
```

#### 问题 5: 审计日志清理失败

**症状**: 审计日志表过大，影响性能

**排查步骤**:

```bash
# 检查审计日志表大小
sudo -u postgres psql -d his_prod -c "
SELECT pg_size_pretty(pg_total_relation_size('audit_log')) as size;
"

# 检查定时任务
sudo systemctl list-timers | grep audit
```

**解决方案**:

```bash
# 1. 手动清理旧日志
sudo -u postgres psql -d his_prod -c "
DELETE FROM audit_log
WHERE operation_time < NOW() - INTERVAL '180 days';
VACUUM FULL audit_log;
ANALYZE audit_log;
"

# 2. 验证保留配置
grep -r "audit.log.retention.days" /opt/his/application-*.yml

# 3. 创建定时清理任务
sudo nano /etc/systemd/system/his-audit-cleanup.service
```

```ini
[Unit]
Description=HIS Audit Log Cleanup
After=network.target postgresql.service

[Service]
Type=oneshot
User=postgres
ExecStart=/usr/bin/psql -d his_prod -c "DELETE FROM audit_log WHERE operation_time < NOW() - INTERVAL '180 days'; VACUUM FULL audit_log;"
```

```bash
sudo nano /etc/systemd/system/his-audit-cleanup.timer
```

```ini
[Unit]
Description=Weekly HIS Audit Log Cleanup
Requires=his-audit-cleanup.service

[Timer]
OnCalendar=weekly
Persistent=true

[Install]
WantedBy=timers.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable his-audit-cleanup.timer
sudo systemctl start his-audit-cleanup.timer
```

#### 问题 6: JWT 令牌过期频繁

**症状**: 用户频繁被要求重新登录

**解决方案**:

```bash
# 1. 检查当前配置
grep "jwt.expiration" /opt/his/application-*.yml

# 2. 调整令牌有效期（如从24小时延长到7天）
sudo nano /opt/his/application-prod.yml
```

```yaml
jwt:
  secret: ${JWT_SECRET}
  expiration: 604800000  # 7天（毫秒）
```

```bash
sudo systemctl restart his
```

### 告警配置

#### 邮件告警

编辑 `application-prod.yml`:

```yaml
monitoring:
  alert:
    email:
      enabled: true
      to: ops@yourhospital.com
      from: his-alerts@yourhospital.com
    error:
      threshold: 100  # 每100个错误告警一次
    health:
      enabled: true
      interval: 60  # 每60秒检查一次
```

#### Prometheus + Grafana 监控

**Prometheus 配置** (`/etc/prometheus/prometheus.yml`):

```yaml
scrape_configs:
  - job_name: 'his'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
    scrape_interval: 15s
```

**关键告警规则** (`/etc/prometheus/alerts.yml`):

```yaml
groups:
  - name: his_alerts
    interval: 30s
    rules:
      # 应用健康检查
      - alert: HISApplicationDown
        expr: up{job="his"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "HIS 应用实例 down"
          description: "{{ $labels.instance }} 应用已宕机超过1分钟"

      # 高错误率
      - alert: HISHighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "HIS 错误率过高"
          description: "5xx 错误率超过 5%"

      # 高延迟
      - alert: HISHighLatency
        expr: histogram_quantile(0.95, http_server_requests_seconds_bucket) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "HIS 响应延迟过高"
          description: "P95 延迟超过 1 秒"

      # 数据库连接池耗尽
      - alert: HISDatabasePoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "数据库连接池即将耗尽"
          description: "活跃连接数占比超过 90%"

      # JVM 内存使用过高
      - alert: HISHighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "JVM 堆内存使用过高"
          description: "堆内存使用率超过 90%"
```

---

## 性能调优

### JVM 调优

#### 内存配置

**小型诊所**（2核，2GB RAM，约100并发）:

```bash
java -Xmx1g -Xms1g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/his/ \
     -jar his-0.0.1-SNAPSHOT.jar
```

**中型医院**（4核，4GB RAM，约500并发）:

```bash
java -Xmx2g -Xms2g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+UseStringDeduplication \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/his/ \
     -XX:+PrintGCDetails \
     -XX:+PrintGCDateStamps \
     -Xloggc:/var/log/his/gc.log \
     -jar his-0.0.1-SNAPSHOT.jar
```

**大型医院**（8+核，8GB+ RAM，1000+并发）:

```bash
java -Xmx4g -Xms4g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=100 \
     -XX:G1HeapRegionSize=16m \
     -XX:+UseStringDeduplication \
     -XX:+OptimizeStringConcat \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/his/ \
     -XX:+PrintGCDetails \
     -XX:+PrintGCDateStamps \
     -Xloggc:/var/log/his/gc.log \
     -XX:+UseGCLogFileRotation \
     -XX:NumberOfGCLogFiles=10 \
     -XX:GCLogFileSize=100M \
     -jar his-0.0.1-SNAPSHOT.jar
```

#### GC 调优

**监控 GC 性能**:

```bash
# 查看当前 GC 统计
curl http://localhost:8080/actuator/metrics/jvm.gc.pause

# 分析 GC 日志
wget https://github.com/heewa/gceasy-loader/releases/download/v1.0.0/gceasy-loader.jar
java -jar gceasy-loader.jar /var/log/his/gc.log
```

**常见 GC 问题**:

1. **频繁 Full GC**:
   - 原因: 堆内存不足
   - 解决: 增加 `-Xmx` 值

2. **GC 耗时过长**:
   - 原因: 堆内存过大或对象过多
   - 解决: 调整 `-XX:MaxGCPauseMillis` 和 `-XX:G1HeapRegionSize`

3. **内存泄漏**:
   - 症状: 持续 Full GC，内存不释放
   - 排查: `jmap -histo:live <pid> > histogram.txt`
   - 分析 Heap Dump: VisualVM, Eclipse MAT

### 数据库调优

#### 连接池优化

**高并发场景**（500+ 并发）:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30        # 增加最大连接数
      minimum-idle: 10             # 增加最小空闲连接
      connection-timeout: 30000    # 30秒
      idle-timeout: 300000         # 5分钟
      max-lifetime: 1800000        # 30分钟
      connection-test-query: SELECT 1
      validation-timeout: 3000
```

#### 查询优化

**慢查询分析**:

```sql
-- 启用 pg_stat_statements
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- 查看最慢的查询
SELECT
  query,
  calls,
  total_exec_time / 1000 as total_seconds,
  mean_exec_time / 1000 as mean_seconds,
  stddev_exec_time / 1000 as stddev_seconds
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 20;

-- 查看执行次数最多的查询
SELECT
  query,
  calls,
  total_exec_time / 1000 as total_seconds
FROM pg_stat_statements
ORDER BY calls DESC
LIMIT 20;
```

**索引优化**:

```sql
-- 查看索引使用情况
SELECT
  schemaname,
  tablename,
  indexname,
  idx_scan as index_scans,
  idx_tup_read as tuples_read,
  idx_tup_fetch as tuples_fetched
FROM pg_stat_user_indexes
ORDER BY idx_scan ASC;

-- 查找未使用的索引
SELECT
  schemaname,
  tablename,
  indexname
FROM pg_stat_user_indexes
WHERE idx_scan = 0
AND indexname NOT LIKE '%_pkey';

-- 查看表大小
SELECT
  tablename,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

**表维护**:

```bash
# 定期清理和优化表
sudo -u postgres psql -d his_prod

-- 清理死行
VACUUM;

-- 完全清理并重建表（会锁表）
VACUUM FULL registration;
VACUUM FULL prescription;
VACUUM FULL medicine;

-- 更新统计信息
ANALYZE registration;
ANALYZE prescription;
ANALYZE medicine;

-- 重建索引
REINDEX TABLE CONCURRENTLY medicine;
REINDEX TABLE CONCURRENTLY prescription;
```

### 应用调优

#### 缓存策略

**启用 Spring Cache** (如需要):

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 600000  # 10分钟
```

#### 并发优化

**PostgreSQL 序列优化**:

```sql
-- 设置序列缓存值（减少锁竞争）
ALTER SEQUENCE reg_no_seq CACHE 100;
ALTER SEQUENCE presc_no_seq CACHE 100;
ALTER SEQUENCE charge_no_seq CACHE 100;
```

#### 异步处理

**审计日志异步写入** (已实现):

```yaml
audit:
  log:
    async: true
    queue-capacity: 1000
    thread-pool-size: 4
```

### 容量规划

#### 服务器规格建议

| 规模 | 并发用户 | CPU | 内存 | 磁盘 | 数据库 |
|------|---------|-----|------|------|--------|
| 小型诊所 | 100 | 2核 | 2GB | 50GB | 与应用同服务器 |
| 中型医院 | 500 | 4核 | 4GB | 100GB | 独立服务器 |
| 大型医院 | 1000+ | 8+核 | 8GB+ | 500GB | 独立服务器 + 只读副本 |

#### 性能基准

**标准配置**（4核，4GB RAM，PostgreSQL 14）:

- 登录：~50ms (P95)
- 患者挂号：~80ms (P95)
- 开具处方：~120ms (P95)
- 支付处理：~100ms (P95)
- 药品搜索：~30ms (P95)

**压力测试**:

```bash
# 使用 Apache Bench 进行压测
ab -n 10000 -c 100 -H "Authorization: Bearer <token>" \
   http://localhost:8080/api/doctor/patients

# 使用 wrk 进行压测
wrk -t4 -c100 -d30s --latency \
     -H "Authorization: Bearer <token>" \
     http://localhost:8080/api/doctor/patients
```

---

## 快速开始

### 前置要求

- **JDK 17+**
- **PostgreSQL 14+**
- **Gradle 8.2+** (已包含 Wrapper)

### 开发环境搭建

```bash
# 1. 克隆仓库
git clone https://github.com/Bernardyao/HMS-backend.git
cd his

# 2. 创建数据库
createdb -U postgres his_project

# 3. 加载测试数据（可选）
psql -U postgres -d his_project -f sql/test_data_sysuser.sql
psql -U postgres -d his_project -f sql/his_design_bigserial.sql

# 4. 配置数据库连接
编辑 src/main/resources/application-dev.yml
```

### 默认测试账号

| 用户名 | 密码 | 角色 |
|----------|----------|------|
| admin | admin123 | 管理员 |
| doctor001 | doctor001 | 医生 |
| nurse001 | nurse001 | 护士 |
| pharmacist001 | pharmacist001 | 药师 |
| cashier001 | cashier001 | 收费员 |

### 启动应用

```bash
# 运行测试
./gradlew test

# 启动应用
./gradlew bootRun

# 或构建 JAR 后运行
./gradlew build
java -jar build/libs/his-0.0.1-SNAPSHOT.jar
```

**应用访问地址**: `http://localhost:8080`

---

## 开发指南

### 项目结构

```
his/
├── src/main/java/com/his/
│   ├── controller/          # REST API 端点
│   ├── service/            # 业务逻辑层
│   ├── repository/         # 数据访问层
│   ├── entity/             # JPA 实体
│   ├── dto/                # 数据传输对象
│   ├── common/             # 工具类
│   └── config/             # Spring 配置
├── src/main/resources/
│   ├── db/migration/       # Flyway 迁移脚本
│   └── application*.yml    # 配置文件
└── build.gradle            # 构建配置
```

### 测试覆盖率

- 整体: 77% 行覆盖率
- 服务层: 80% 行覆盖率
- 控制器层: 64% 行覆盖率

---

## 许可与支持

### 许可证

本项目采用 MIT 许可证。

### 支持

- **GitHub Issues**: 报告问题和功能请求
- **电子邮件**: bernardyao624@gmail.com

---

**最后更新**: 2025年1月4日
**版本**: 1.0.0

---

© 2025 HIS Development Team. All rights reserved.
