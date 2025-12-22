# Configuration Guide

## Overview

This guide covers how to configure, set up, and run the PartyTime Server. It includes development setup, production configuration, and all available configuration properties.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Development Setup](#development-setup)
- [Configuration Properties](#configuration-properties)
- [Database Configuration](#database-configuration)
- [Running the Application](#running-the-application)
- [Production Configuration](#production-configuration)
- [Environment Variables](#environment-variables)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| Java JDK | 25+ | Runtime environment |
| Gradle | 8.11.1+ | Build tool (wrapper included) |
| PostgreSQL | 13+ | Database |

### Optional Software

| Software | Purpose |
|----------|---------|
| Docker | Containerized database |
| Git | Version control |
| IntelliJ IDEA / Eclipse | IDE |
| Postman / curl | API testing |

---

## Development Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd PartyTime-Server-Java
```

### 2. Set Up PostgreSQL Database

#### Option A: Local PostgreSQL Installation

**Install PostgreSQL** (varies by OS):

```bash
# Ubuntu/Debian
sudo apt-get install postgresql

# macOS (Homebrew)
brew install postgresql

# Windows
# Download installer from postgresql.org
```

**Create Database**:

```bash
# Connect to PostgreSQL
psql -U postgres

# Create database and user
CREATE DATABASE partytime;
CREATE USER partytime WITH PASSWORD 'partytime123';
GRANT ALL PRIVILEGES ON DATABASE partytime TO partytime;
\q
```

**Update Connection Settings**:

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/partytime
spring.datasource.username=partytime
spring.datasource.password=partytime123
```

---

#### Option B: Docker PostgreSQL

**Create docker-compose.yml** (in project root):

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:16
    container_name: partytime-postgres
    environment:
      POSTGRES_DB: partytime
      POSTGRES_USER: partytime
      POSTGRES_PASSWORD: partytime123
    ports:
      - "7070:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

**Start Database**:

```bash
docker-compose up -d
```

**Connection Settings** (already configured):

```properties
spring.datasource.url=jdbc:postgresql://localhost:7070/partytime
spring.datasource.username=partytime
spring.datasource.password=partytime123
```

---

### 3. Build the Application

```bash
# Clean and build
./gradlew clean build

# Build without tests (faster)
./gradlew clean build -x test
```

**Windows**:
```bash
gradlew.bat clean build
```

---

### 4. Run the Application

```bash
# Using Gradle wrapper
./gradlew bootRun

# Or run the built JAR
java -jar build/libs/PartyTime-Server-Java-0.0.1-SNAPSHOT.jar
```

**Application starts on**: `http://localhost:8000`

**Verify it's running**:

```bash
curl http://localhost:8000/api/party
# Expected: [] (empty array)
```

---

## Configuration Properties

### Application Properties File

**Location**: `src/main/resources/application.properties`

**Full Configuration**:

```properties
# Server Configuration
server.port=8000

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:7070/partytime
spring.datasource.username=partytime
spring.datasource.password=partytime123

# JPA / Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Drop Party Configuration
party.duplicate-window-millis=500
party.timeout-minutes=5
party.cleanup-interval-millis=120000
```

---

### Server Configuration

#### server.port

**Description**: HTTP port the application listens on

**Default**: `8000`

**Example**:
```properties
server.port=8080
```

**Environment Variable**:
```bash
SERVER_PORT=8080 ./gradlew bootRun
```

---

### Database Configuration

#### spring.datasource.url

**Description**: JDBC connection URL for PostgreSQL

**Format**: `jdbc:postgresql://<host>:<port>/<database>`

**Default**: `jdbc:postgresql://localhost:7070/partytime`

**Examples**:

```properties
# Local PostgreSQL on standard port
spring.datasource.url=jdbc:postgresql://localhost:5432/partytime

# Remote database
spring.datasource.url=jdbc:postgresql://db.example.com:5432/partytime

# Docker container by name
spring.datasource.url=jdbc:postgresql://postgres-container:5432/partytime
```

---

#### spring.datasource.username

**Description**: Database username

**Default**: `partytime`

**Security Note**: Do NOT commit production credentials to version control

---

#### spring.datasource.password

**Description**: Database password

**Default**: `partytime123`

**Security Note**: Use environment variables in production

---

### JPA / Hibernate Configuration

#### spring.jpa.hibernate.ddl-auto

**Description**: How Hibernate manages database schema

**Options**:

| Value | Description | Use Case |
|-------|-------------|----------|
| `none` | No schema management | Manual migrations |
| `validate` | Validate schema matches entities | Production (recommended) |
| `update` | Update schema to match entities | Development |
| `create` | Drop and recreate schema on startup | Testing |
| `create-drop` | Create on startup, drop on shutdown | Testing |

**Default**: `update`

**Production Recommendation**: `validate`

**Example**:
```properties
spring.jpa.hibernate.ddl-auto=validate
```

---

#### spring.jpa.show-sql

**Description**: Log SQL statements to console

**Default**: `false`

**Development**: Set to `true` for debugging

```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

**Output Example**:
```sql
Hibernate:
    select
        dp1_0.id,
        dp1_0.avg_drop,
        dp1_0.created_at,
        dp1_0.is_active,
        dp1_0.last_drop_at,
        dp1_0.world
    from
        drop_party dp1_0
    where
        dp1_0.is_active=?
```

---

### Drop Party Configuration

#### party.duplicate-window-millis

**Description**: Time window (in milliseconds) for duplicate drop detection

**Default**: `500` (0.5 seconds)

**Type**: Integer

**Purpose**: Drops within this window are considered duplicates and ignored

**Example**:
```properties
# 1 second window
party.duplicate-window-millis=1000

# No duplicate detection
party.duplicate-window-millis=0
```

**Code Reference**: `src/main/java/rip/jade/partytimeserverjava/service/DropService.java:26`

---

#### party.timeout-minutes

**Description**: Inactivity period before party is marked inactive

**Default**: `5` (5 minutes)

**Type**: Long

**Purpose**: Parties with no drops for this duration are deactivated

**Example**:
```properties
# 10 minute timeout
party.timeout-minutes=10

# 2 minute timeout (more aggressive cleanup)
party.timeout-minutes=2
```

**Code Reference**: `src/main/java/rip/jade/partytimeserverjava/service/DropService.java:27`

---

#### party.cleanup-interval-millis

**Description**: How often the scheduled cleanup task runs

**Default**: `120000` (2 minutes)

**Type**: Long

**Purpose**: Background job frequency for deactivating timed-out parties

**Example**:
```properties
# Run every 1 minute
party.cleanup-interval-millis=60000

# Run every 5 minutes
party.cleanup-interval-millis=300000
```

**Code Reference**: `src/main/java/rip/jade/partytimeserverjava/service/DropService.java:144`

**Note**: This is a fixed delay, not fixed rate (waits until previous execution completes)

---

## Database Configuration

### Connection Pooling

Spring Boot uses **HikariCP** connection pool by default (included in `spring-boot-starter-data-jpa`).

**Default Settings** (auto-configured):
- Maximum pool size: 10 connections
- Minimum idle: 10 connections
- Connection timeout: 30 seconds

**Custom Configuration**:

```properties
# Maximum pool size
spring.datasource.hikari.maximum-pool-size=20

# Minimum idle connections
spring.datasource.hikari.minimum-idle=5

# Connection timeout (milliseconds)
spring.datasource.hikari.connection-timeout=30000

# Maximum lifetime of connection (milliseconds)
spring.datasource.hikari.max-lifetime=1800000

# Idle timeout (milliseconds)
spring.datasource.hikari.idle-timeout=600000
```

---

### Database Initialization

#### Automatic Schema Creation

With `spring.jpa.hibernate.ddl-auto=update`:
- Tables created automatically on first run
- Indexes created automatically
- Schema updates applied on entity changes

**First Run**:
```sql
-- Tables created automatically
CREATE TABLE drop_party (
    id BIGSERIAL PRIMARY KEY,
    world INTEGER NOT NULL,
    avg_drop INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_drop_at TIMESTAMP NOT NULL
);

CREATE TABLE drop (
    id BIGSERIAL PRIMARY KEY,
    item_id INTEGER NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    value INTEGER NOT NULL,
    drop_party_id BIGINT NOT NULL,
    FOREIGN KEY (drop_party_id) REFERENCES drop_party(id)
);

-- Indexes created automatically
CREATE INDEX idx_world_active ON drop_party(world, is_active);
CREATE INDEX idx_active_last_drop ON drop_party(is_active, last_drop_at);
```

---

#### Manual Schema Creation

For production, use `spring.jpa.hibernate.ddl-auto=validate` and create schema manually.

**SQL Script** (`schema.sql`):

```sql
-- Create tables
CREATE TABLE IF NOT EXISTS drop_party (
    id BIGSERIAL PRIMARY KEY,
    world INTEGER NOT NULL,
    avg_drop INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_drop_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS drop (
    id BIGSERIAL PRIMARY KEY,
    item_id INTEGER NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    value INTEGER NOT NULL,
    drop_party_id BIGINT NOT NULL,
    CONSTRAINT fk_drop_party FOREIGN KEY (drop_party_id)
        REFERENCES drop_party(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_world_active
    ON drop_party(world, is_active);

CREATE INDEX IF NOT EXISTS idx_active_last_drop
    ON drop_party(is_active, last_drop_at);
```

**Execute**:
```bash
psql -U partytime -d partytime -f schema.sql
```

---

## Running the Application

### Development Mode

**Using Gradle**:
```bash
./gradlew bootRun
```

**With custom configuration**:
```bash
./gradlew bootRun --args='--server.port=9000 --spring.profiles.active=dev'
```

**With environment variables**:
```bash
SERVER_PORT=9000 ./gradlew bootRun
```

---

### Production Mode

**Build JAR**:
```bash
./gradlew clean build
```

**Run JAR**:
```bash
java -jar build/libs/PartyTime-Server-Java-0.0.1-SNAPSHOT.jar
```

**With custom config**:
```bash
java -jar build/libs/PartyTime-Server-Java-0.0.1-SNAPSHOT.jar \
  --server.port=8080 \
  --spring.datasource.url=jdbc:postgresql://prod-db:5432/partytime
```

**Background process**:
```bash
nohup java -jar build/libs/PartyTime-Server-Java-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
```

---

### Using Systemd (Linux)

**Create service file**: `/etc/systemd/system/partytime.service`

```ini
[Unit]
Description=PartyTime Server
After=network.target postgresql.service

[Service]
Type=simple
User=partytime
WorkingDirectory=/opt/partytime
ExecStart=/usr/bin/java -jar /opt/partytime/PartyTime-Server-Java.jar
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=partytime

Environment="SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/partytime"
Environment="SPRING_DATASOURCE_USERNAME=partytime"
Environment="SPRING_DATASOURCE_PASSWORD=secret"

[Install]
WantedBy=multi-user.target
```

**Commands**:
```bash
# Reload systemd
sudo systemctl daemon-reload

# Start service
sudo systemctl start partytime

# Enable on boot
sudo systemctl enable partytime

# Check status
sudo systemctl status partytime

# View logs
sudo journalctl -u partytime -f
```

---

## Production Configuration

### Environment-Specific Configuration

Create profile-specific property files:

```
application.properties           # Common settings
application-dev.properties       # Development overrides
application-prod.properties      # Production overrides
```

**application-prod.properties**:

```properties
# Production database
spring.datasource.url=jdbc:postgresql://prod-db.example.com:5432/partytime
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# Schema validation only (no auto-updates)
spring.jpa.hibernate.ddl-auto=validate

# No SQL logging
spring.jpa.show-sql=false

# Connection pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=10

# Production party settings
party.duplicate-window-millis=500
party.timeout-minutes=5
party.cleanup-interval-millis=120000
```

**Activate profile**:
```bash
java -jar app.jar --spring.profiles.active=prod
```

---

### Security Best Practices

#### 1. Never Commit Secrets

**Bad**:
```properties
spring.datasource.password=super-secret-password
```

**Good**:
```properties
spring.datasource.password=${DB_PASSWORD}
```

#### 2. Use Environment Variables

```bash
export DB_USERNAME=partytime
export DB_PASSWORD=super-secret-password
java -jar app.jar
```

#### 3. External Configuration File

```bash
java -jar app.jar --spring.config.location=/etc/partytime/application.properties
```

#### 4. Secret Management

Use dedicated secret management:
- **AWS Secrets Manager**
- **HashiCorp Vault**
- **Kubernetes Secrets**

---

### Performance Tuning

#### JVM Settings

```bash
java -Xms512m -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/partytime/heap_dump.hprof \
  -jar app.jar
```

**Parameters**:
- `-Xms512m`: Initial heap size
- `-Xmx2g`: Maximum heap size
- `-XX:+UseG1GC`: Use G1 garbage collector
- `-XX:MaxGCPauseMillis=200`: Target GC pause time

---

#### Database Connection Pool

```properties
# For high-traffic applications
spring.datasource.hikari.maximum-pool-size=50
spring.datasource.hikari.minimum-idle=20
```

**Formula**: Max pool size ≈ (CPU cores × 2) + disk spindles

---

### Logging Configuration

**application-prod.properties**:

```properties
# Log level
logging.level.root=INFO
logging.level.rip.jade.partytimeserverjava=INFO
logging.level.org.springframework.web=WARN
logging.level.org.hibernate=WARN

# Log file
logging.file.name=/var/log/partytime/application.log
logging.file.max-size=10MB
logging.file.max-history=30

# Log pattern
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

---

## Environment Variables

### Supported Variables

All `application.properties` can be overridden via environment variables using uppercase and underscores:

| Property | Environment Variable |
|----------|---------------------|
| `server.port` | `SERVER_PORT` |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` |
| `party.duplicate-window-millis` | `PARTY_DUPLICATE_WINDOW_MILLIS` |
| `party.timeout-minutes` | `PARTY_TIMEOUT_MINUTES` |
| `party.cleanup-interval-millis` | `PARTY_CLEANUP_INTERVAL_MILLIS` |

### Example .env File

**`.env`** (for Docker Compose):

```env
SERVER_PORT=8000
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/partytime
SPRING_DATASOURCE_USERNAME=partytime
SPRING_DATASOURCE_PASSWORD=partytime123
SPRING_JPA_HIBERNATE_DDL_AUTO=update
PARTY_DUPLICATE_WINDOW_MILLIS=500
PARTY_TIMEOUT_MINUTES=5
PARTY_CLEANUP_INTERVAL_MILLIS=120000
```

---

## Troubleshooting

### Common Issues

#### 1. Application Won't Start - Port Already in Use

**Error**:
```
Web server failed to start. Port 8000 was already in use.
```

**Solution**:
```bash
# Find process using port 8000
lsof -i :8000
# or
netstat -tulpn | grep 8000

# Kill the process or change port
SERVER_PORT=8080 ./gradlew bootRun
```

---

#### 2. Database Connection Failed

**Error**:
```
Connection to localhost:7070 refused.
```

**Checklist**:
1. Is PostgreSQL running?
   ```bash
   # Docker
   docker ps | grep postgres

   # Local
   sudo systemctl status postgresql
   ```

2. Is the port correct?
   ```bash
   # Check PostgreSQL port
   psql -U postgres -c "SHOW port;"
   ```

3. Are credentials correct?
   ```bash
   # Test connection
   psql -h localhost -p 7070 -U partytime -d partytime
   ```

4. Check firewall rules

---

#### 3. Schema Validation Failed

**Error**:
```
Schema-validation: missing table [drop_party]
```

**Cause**: Using `ddl-auto=validate` but tables don't exist

**Solution**:
1. Change to `ddl-auto=update` for first run
2. Or manually create schema (see [Database Initialization](#database-initialization))

---

#### 4. Hibernate N+1 Query Problem

**Symptom**: Slow GET /api/party with many queries

**Cause**: Lazy loading drops for each party

**Solution**: Use eager fetch query

```java
// In DropPartyRepository
@Query("SELECT dp FROM DropParty dp LEFT JOIN FETCH dp.drops WHERE dp.isActive = true")
List<DropParty> findActivePartiesWithDrops();
```

---

#### 5. Out of Memory Error

**Error**:
```
java.lang.OutOfMemoryError: Java heap space
```

**Solution**: Increase heap size
```bash
java -Xmx2g -jar app.jar
```

---

### Debugging

**Enable SQL Logging**:
```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

**Enable HTTP Request Logging**:
```properties
logging.level.org.springframework.web=DEBUG
```

**Enable All Debug Logging**:
```bash
java -jar app.jar --debug
```

---

### Health Check

**Endpoint** (requires Spring Boot Actuator):

Add to `build.gradle`:
```gradle
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

**application.properties**:
```properties
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
```

**Check health**:
```bash
curl http://localhost:8000/actuator/health
```

**Response**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    }
  }
}
```

---

## Quick Reference

### Start Application (Development)

```bash
# 1. Start database (Docker)
docker-compose up -d

# 2. Run application
./gradlew bootRun

# 3. Test endpoint
curl http://localhost:8000/api/party
```

---

### Build for Production

```bash
# 1. Clean build
./gradlew clean build

# 2. Copy JAR
cp build/libs/PartyTime-Server-Java-0.0.1-SNAPSHOT.jar /opt/partytime/

# 3. Create config
cat > /etc/partytime/application.properties <<EOF
spring.datasource.url=jdbc:postgresql://prod-db:5432/partytime
spring.datasource.username=\${DB_USERNAME}
spring.datasource.password=\${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
EOF

# 4. Run
cd /opt/partytime
java -jar PartyTime-Server-Java-0.0.1-SNAPSHOT.jar \
  --spring.config.location=/etc/partytime/application.properties
```

---

## Additional Resources

- [Spring Boot Configuration Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [ARCHITECTURE.md](ARCHITECTURE.md) - System architecture
- [API_DOCUMENTATION.md](API_DOCUMENTATION.md) - API reference
