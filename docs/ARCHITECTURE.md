# Architecture Documentation

## Overview

PartyTime Server is a Spring Boot REST API application designed to track and aggregate MMORPG drop party events in real-time. The application follows a classic layered architecture pattern with clear separation of concerns.

---

## Table of Contents

- [System Architecture](#system-architecture)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Layered Architecture](#layered-architecture)
- [Design Patterns](#design-patterns)
- [Dependencies](#dependencies)
- [Deployment Architecture](#deployment-architecture)

---

## System Architecture

### High-Level System Diagram

```
┌─────────────────────────────────────────────────────┐
│                   Game Clients                       │
│  (Players running drop detection plugin/script)     │
└──────────────────┬──────────────────────────────────┘
                   │
                   │ HTTP POST /api/drops
                   │ {world, itemId, itemName, qty, value}
                   │
                   ▼
┌──────────────────────────────────────────────────────┐
│             PartyTime Server (Port 8000)             │
│                                                      │
│  ┌────────────────────────────────────────────────┐ │
│  │         Presentation Layer                     │ │
│  │  ┌──────────────┐    ┌──────────────┐         │ │
│  │  │ Drops        │    │ Party        │         │ │
│  │  │ Controller   │    │ Controller   │         │ │
│  │  └──────────────┘    └──────────────┘         │ │
│  └──────────────┬──────────────┬──────────────────┘ │
│                 │              │                    │
│  ┌──────────────┴──────────────┴──────────────────┐ │
│  │            Service Layer                       │ │
│  │  ┌──────────────────────────────────┐         │ │
│  │  │      DropService                 │         │ │
│  │  │  - handleDrop()                  │         │ │
│  │  │  - cleanupInactiveParties()      │         │ │
│  │  │  - calculateAverageDrop()        │         │ │
│  │  └──────────────────────────────────┘         │ │
│  └──────────────┬──────────────────────────────────┘ │
│                 │                                    │
│  ┌──────────────┴──────────────────────────────────┐ │
│  │         Repository Layer (Spring Data JPA)     │ │
│  │  ┌───────────────┐    ┌──────────────────┐    │ │
│  │  │ DropParty     │    │ Drop             │    │ │
│  │  │ Repository    │    │ Repository       │    │ │
│  │  └───────────────┘    └──────────────────┘    │ │
│  └──────────────┬──────────────────────────────────┘ │
│                 │                                    │
│  ┌──────────────┴──────────────────────────────────┐ │
│  │              Entity Layer                       │ │
│  │  ┌───────────────┐    ┌──────────────────┐    │ │
│  │  │ DropParty     │    │ Drop             │    │ │
│  │  │ Entity        │───>│ Entity           │    │ │
│  │  └───────────────┘    └──────────────────┘    │ │
│  └──────────────┬──────────────────────────────────┘ │
└─────────────────┼────────────────────────────────────┘
                  │
                  │ JDBC (PostgreSQL Driver)
                  │
                  ▼
┌──────────────────────────────────────────────────────┐
│         PostgreSQL Database (Port 7070)              │
│                                                      │
│  ┌────────────────┐       ┌────────────────┐       │
│  │  drop_party    │       │    drop        │       │
│  │  table         │<──────│    table       │       │
│  └────────────────┘  1:N  └────────────────┘       │
└──────────────────────────────────────────────────────┘
                  │
                  │ (Optional)
                  ▼
┌──────────────────────────────────────────────────────┐
│           Web Dashboard (Future)                     │
│  - View active parties                               │
│  - Statistics and analytics                          │
│  - Admin controls                                    │
└──────────────────────────────────────────────────────┘
```

---

## Technology Stack

### Core Framework

| Technology | Version | Purpose |
|-----------|---------|---------|
| Spring Boot | 4.0.0 | Application framework |
| Java | 25 | Programming language |
| Spring Web MVC | - | REST API endpoints |
| Spring Data JPA | - | Database abstraction |
| Hibernate | - | ORM implementation |

### Database

| Technology | Version | Purpose |
|-----------|---------|---------|
| PostgreSQL | - | Relational database |
| JDBC Driver | - | Database connectivity |

### Build & Development

| Technology | Version | Purpose |
|-----------|---------|---------|
| Gradle | 8.11.1 | Build automation |
| Lombok | - | Boilerplate reduction |

### Validation

| Technology | Version | Purpose |
|-----------|---------|---------|
| Jakarta Bean Validation | - | Request validation |
| Hibernate Validator | - | Validation implementation |

---

## Project Structure

```
PartyTime-Server-Java/
├── gradle/                          # Gradle wrapper files
├── src/
│   ├── main/
│   │   ├── java/rip/jade/partytimeserverjava/
│   │   │   ├── PartyTimeServerJavaApplication.java    # Entry point
│   │   │   ├── Controller/
│   │   │   │   ├── DropsController.java               # POST /api/drops
│   │   │   │   └── PartyController.java               # GET /api/party
│   │   │   ├── dto/
│   │   │   │   ├── DropRequest.java                   # Input validation
│   │   │   │   ├── DropResponse.java                  # Drop response
│   │   │   │   └── DropPartyResponse.java             # Party response
│   │   │   ├── entity/
│   │   │   │   ├── Drop.java                          # Drop entity (table)
│   │   │   │   └── DropParty.java                     # DropParty entity
│   │   │   ├── repository/
│   │   │   │   ├── DropRepository.java                # Drop data access
│   │   │   │   └── DropPartyRepository.java           # Party data access
│   │   │   └── service/
│   │   │       └── DropService.java                   # Business logic
│   │   └── resources/
│   │       └── application.properties                 # Configuration
│   └── test/                        # Test files (TODO)
├── docs/                            # Documentation
│   ├── ARCHITECTURE.md              # This file
│   ├── DATABASE_SCHEMA.md           # Database documentation
│   ├── API_DOCUMENTATION.md         # API reference
│   ├── APPLICATION_FLOWS.md         # Business logic flows
│   └── CONFIGURATION.md             # Setup guide
├── build.gradle                     # Build configuration
├── settings.gradle                  # Project settings
├── gradlew                          # Gradle wrapper script
└── TODO.md                          # Development roadmap
```

---

## Layered Architecture

The application follows a strict layered architecture with unidirectional dependencies:

```
┌─────────────────────────────────────┐
│      Presentation Layer             │  Controllers
│  - REST endpoints                   │  - Handle HTTP requests
│  - Request/response DTOs            │  - Input validation
│  - HTTP status codes                │  - Response formatting
└────────────┬────────────────────────┘
             │ uses
             ▼
┌─────────────────────────────────────┐
│       Service Layer                 │  Business Logic
│  - Business logic                   │  - Transaction management
│  - Transaction boundaries           │  - Domain rules
│  - Domain rules                     │  - Orchestration
└────────────┬────────────────────────┘
             │ uses
             ▼
┌─────────────────────────────────────┐
│      Repository Layer               │  Data Access
│  - Database queries                 │  - CRUD operations
│  - Custom queries                   │  - Query methods
│  - Spring Data JPA                  │  - No business logic
└────────────┬────────────────────────┘
             │ maps
             ▼
┌─────────────────────────────────────┐
│       Entity Layer                  │  Domain Model
│  - JPA entities                     │  - Database mapping
│  - Table mappings                   │  - Relationships
│  - Relationships                    │  - Constraints
└─────────────────────────────────────┘
```

### Layer Responsibilities

#### 1. Presentation Layer (Controllers)

**Location**: `src/main/java/rip/jade/partytimeserverjava/Controller/`

**Responsibilities**:
- Expose REST API endpoints
- Validate incoming requests
- Map DTOs to/from domain objects
- Return appropriate HTTP status codes
- Handle HTTP-specific concerns

**Key Classes**:
- `DropsController` - Drop submission endpoint
- `PartyController` - Party query endpoints

**Design Principle**: Thin controllers - delegate business logic to service layer

---

#### 2. Service Layer

**Location**: `src/main/java/rip/jade/partytimeserverjava/service/`

**Responsibilities**:
- Implement business logic
- Define transaction boundaries
- Coordinate between repositories
- Enforce domain rules
- Handle complex operations

**Key Classes**:
- `DropService` - Core business logic for drops and parties

**Annotations**:
- `@Service` - Spring service bean
- `@Transactional` - Transaction management
- `@Scheduled` - Background tasks

---

#### 3. Repository Layer

**Location**: `src/main/java/rip/jade/partytimeserverjava/repository/`

**Responsibilities**:
- Provide data access abstraction
- Define query methods
- Custom JPQL/SQL queries
- No business logic

**Key Interfaces**:
- `DropRepository` - Drop entity queries
- `DropPartyRepository` - DropParty entity queries

**Technology**: Spring Data JPA (automatic implementation)

---

#### 4. Entity Layer

**Location**: `src/main/java/rip/jade/partytimeserverjava/entity/`

**Responsibilities**:
- Map Java objects to database tables
- Define relationships
- Specify constraints and indexes
- Represent domain model

**Key Classes**:
- `Drop` - Represents individual drop
- `DropParty` - Represents party session

**Annotations**: JPA annotations (`@Entity`, `@Table`, `@Id`, etc.)

---

#### 5. DTO Layer

**Location**: `src/main/java/rip/jade/partytimeserverjava/dto/`

**Responsibilities**:
- Define API contracts
- Input validation rules
- Decouple API from domain model
- Version control for API changes

**Key Classes**:
- `DropRequest` - Input for drop submission
- `DropResponse` - Output for drop submission
- `DropPartyResponse` - Output for party queries

**Technology**: Java records with validation annotations

---

## Design Patterns

### 1. Repository Pattern

**Implementation**: Spring Data JPA

**Purpose**: Abstraction over data access

**Example**:
```java
public interface DropPartyRepository extends JpaRepository<DropParty, Long> {
    Optional<DropParty> findByWorldAndIsActiveTrue(Integer world);
    List<DropParty> findByIsActiveTrue();
}
```

**Benefits**:
- Decouples business logic from data access
- Testable (can mock repositories)
- Automatic CRUD implementation

---

### 2. Data Transfer Object (DTO) Pattern

**Implementation**: Java records

**Purpose**: Separate API contracts from domain model

**Example**:
```java
public record DropRequest(
    @Min(0) @Max(999) Integer world,
    @Min(1) Integer itemId,
    @NotBlank String itemName,
    @Min(1) Integer quantity,
    @Min(15000) Integer value
)
```

**Benefits**:
- API versioning flexibility
- Validation at boundary
- Prevents over-exposure of domain model

---

### 3. Service Layer Pattern

**Implementation**: `@Service` beans

**Purpose**: Centralize business logic

**Example**:
```java
@Service
public class DropService {
    @Transactional
    public DropResponse handleDrop(DropRequest request) {
        // Business logic here
    }
}
```

**Benefits**:
- Single source of truth for business rules
- Transaction management
- Reusable across controllers

---

### 4. Active Record (via JPA)

**Implementation**: JPA entities

**Purpose**: Domain objects with persistence

**Example**:
```java
@Entity
@Table(name = "drop_party")
public class DropParty {
    // Fields, getters, setters
    // Persistence handled by JPA
}
```

**Benefits**:
- Natural object-relational mapping
- Lazy loading
- Cascade operations

---

### 5. Dependency Injection

**Implementation**: Spring IoC container

**Purpose**: Loose coupling, testability

**Example**:
```java
@RestController
public class DropsController {
    private final DropService dropService;

    public DropsController(DropService dropService) {
        this.dropService = dropService;  // Injected by Spring
    }
}
```

**Benefits**:
- Testable (can inject mocks)
- Flexible configuration
- Manages object lifecycle

---

## Dependencies

### Dependency Graph

```
┌─────────────────┐
│  Controllers    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  DTOs           │
└─────────────────┘
         │
         ▼
┌─────────────────┐
│  Service        │
└────────┬────────┘
         │
         ├──────────────┐
         ▼              ▼
┌─────────────────┐  ┌─────────────────┐
│  Repositories   │  │  Entities       │
└────────┬────────┘  └─────────────────┘
         │
         ▼
┌─────────────────┐
│  Entities       │
└─────────────────┘
```

### Build Dependencies

**Source**: `build.gradle`

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    compileOnly 'org.projectlombok:lombok'
    runtimeOnly 'org.postgresql:postgresql'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

**Dependency Tree**:
- `spring-boot-starter-web` → Spring MVC, Tomcat, Jackson
- `spring-boot-starter-data-jpa` → Hibernate, JDBC, Transaction support
- `spring-boot-starter-validation` → Hibernate Validator, Jakarta Validation
- `postgresql` → PostgreSQL JDBC driver
- `lombok` → Code generation (getters, setters, builders)

---

## Deployment Architecture

### Development Environment

```
┌──────────────────────────────────┐
│  Developer Machine               │
│                                  │
│  ┌────────────────────────────┐ │
│  │  Java 25 Runtime           │ │
│  │  Gradle Build              │ │
│  └────────────────────────────┘ │
│             │                    │
│             ▼                    │
│  ┌────────────────────────────┐ │
│  │  PartyTime Server          │ │
│  │  localhost:8000            │ │
│  └────────────────────────────┘ │
│             │                    │
│             ▼                    │
│  ┌────────────────────────────┐ │
│  │  PostgreSQL                │ │
│  │  localhost:7070            │ │
│  └────────────────────────────┘ │
└──────────────────────────────────┘
```

**Start Application**:
```bash
./gradlew bootRun
```

**Build JAR**:
```bash
./gradlew clean build
```

---

### Production Deployment (Recommended)

```
                    ┌─────────────────┐
                    │  Load Balancer  │
                    │  (Nginx/HAProxy)│
                    └────────┬────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
         ▼                   ▼                   ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│  App Instance 1 │ │  App Instance 2 │ │  App Instance 3 │
│  Port 8000      │ │  Port 8000      │ │  Port 8000      │
└────────┬────────┘ └────────┬────────┘ └────────┬────────┘
         │                   │                   │
         └───────────────────┼───────────────────┘
                             │
                             ▼
                ┌─────────────────────────┐
                │  PostgreSQL (Primary)   │
                │  Port 5432              │
                └────────┬────────────────┘
                         │
                         ▼
                ┌─────────────────────────┐
                │  PostgreSQL (Replica)   │
                │  Port 5432              │
                │  (Read-only)            │
                └─────────────────────────┘
```

**Deployment Checklist**:
1. Build production JAR: `./gradlew clean build`
2. Set `spring.jpa.hibernate.ddl-auto=validate` (not `update`)
3. Configure external PostgreSQL
4. Set environment variables for secrets
5. Use process manager (systemd, Docker, Kubernetes)
6. Configure reverse proxy (Nginx)
7. Enable HTTPS/TLS
8. Set up monitoring (Prometheus, Grafana)
9. Configure logging (centralized)
10. Implement backup strategy

---

### Containerization (Docker)

**Dockerfile** (example):
```dockerfile
FROM eclipse-temurin:25-jdk-alpine
WORKDIR /app
COPY build/libs/partytime-server.jar app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**docker-compose.yml** (example):
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8000:8000"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/partytime
      - SPRING_DATASOURCE_USERNAME=partytime
      - SPRING_DATASOURCE_PASSWORD=secret
    depends_on:
      - db

  db:
    image: postgres:16
    environment:
      - POSTGRES_DB=partytime
      - POSTGRES_USER=partytime
      - POSTGRES_PASSWORD=secret
    ports:
      - "7070:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

---

## Scalability Considerations

### Horizontal Scaling

**Current State**: Supports horizontal scaling with caveats

**Requirements**:
1. **Stateless application**: ✓ (no in-memory state)
2. **Shared database**: ✓ (PostgreSQL shared by all instances)
3. **Session management**: ✓ (no sessions - REST API)

**Challenges**:
- Duplicate detection window (500ms) relies on database state
- Scheduled cleanup runs on all instances (needs coordination)

**Solutions**:
- Use distributed lock (Redis, PostgreSQL advisory locks)
- Designate single instance for scheduled tasks
- Use message queue for drop events (Kafka, RabbitMQ)

---

### Vertical Scaling

**Database**: Primary bottleneck at scale

**Optimizations**:
- Connection pooling (HikariCP - enabled by default)
- Read replicas for GET /api/party queries
- Database indexing (already implemented)
- Query optimization

**Application**:
- JVM tuning (heap size, GC settings)
- Thread pool configuration

---

### Caching Strategy (Future)

**Candidates for caching**:
1. Active parties list (GET /api/party)
   - Cache for 5-10 seconds
   - Invalidate on new drop

2. Average drop calculations
   - Cache until next drop added

**Technology**: Redis, Caffeine (in-memory)

---

## Security Architecture

### Current State

**Authentication**: ❌ None

**Authorization**: ❌ None

**Encryption**: ❌ HTTP only (no HTTPS)

---

### Planned Security (from TODO.md)

```
┌─────────────────┐
│  Game Client    │
│  (API Key)      │
└────────┬────────┘
         │ X-API-Key: abc123
         │
         ▼
┌──────────────────────────────┐
│  API Gateway / Reverse Proxy │
│  - Rate limiting             │
│  - API key validation        │
│  - Request logging           │
└────────┬─────────────────────┘
         │
         ▼
┌──────────────────────────────┐
│  Spring Security Filter      │
│  - Validate API key          │
│  - Check rate limits         │
│  - Assign roles              │
└────────┬─────────────────────┘
         │
         ▼
┌──────────────────────────────┐
│  Controller                  │
│  @PreAuthorize("ROLE_CLIENT")│
└──────────────────────────────┘
```

**Planned Components**:
1. API Key authentication for game clients
2. JWT tokens for web dashboard
3. Role-based access control (RBAC)
4. Rate limiting per client/IP
5. HTTPS/TLS encryption

---

## Monitoring & Observability

### Current State

**Logging**: Default Spring Boot logging

**Metrics**: None

**Tracing**: None

---

### Recommended Additions

**Metrics** (Spring Boot Actuator + Prometheus):
- Request rate and latency
- Database query performance
- Active party count
- Drop submission rate
- Cleanup task duration

**Logging** (Logback + ELK/Loki):
- Structured JSON logging
- Correlation IDs for request tracing
- Error tracking and alerting

**Tracing** (OpenTelemetry):
- Distributed tracing across layers
- Performance bottleneck identification

---

## Code Quality & Testing

### Current State

**Unit Tests**: ❌ None

**Integration Tests**: ❌ None

**Code Coverage**: 0%

---

### Recommended Testing Strategy

**Unit Tests**:
- Service layer logic (duplicate detection, avg calculation)
- DTO validation rules
- Repository query methods

**Integration Tests**:
- Controller endpoints (MockMvc)
- Database operations (TestContainers)
- Full request/response flow

**E2E Tests**:
- Real database + application
- Multiple concurrent clients
- Timeout and cleanup scenarios

---

## Future Architectural Enhancements

Based on TODO.md and scalability needs:

### 1. Event-Driven Architecture

```
Drop Submitted → Event Bus (Kafka) → Multiple Consumers
                                    ├─> Database Writer
                                    ├─> WebSocket Publisher
                                    ├─> Analytics Aggregator
                                    └─> Notification Service
```

### 2. CQRS (Command Query Responsibility Segregation)

- **Commands**: POST /api/drops → Write database
- **Queries**: GET /api/party → Read from cache/read replica

### 3. Microservices (if needed at scale)

- **Drop Service**: Handle drop submissions
- **Party Service**: Query active parties
- **Analytics Service**: Statistics and trends
- **Auth Service**: Authentication and authorization

### 4. API Gateway

- Centralized routing
- Authentication
- Rate limiting
- Request/response transformation

---

## Conclusion

The PartyTime Server is built on solid architectural foundations using Spring Boot best practices. The layered architecture provides clear separation of concerns, making the codebase maintainable and testable. While the current implementation is production-ready for moderate loads, the architecture can be enhanced with caching, event-driven patterns, and microservices as scale requirements grow.

For detailed information on specific components, see:
- [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md) - Database design
- [API_DOCUMENTATION.md](API_DOCUMENTATION.md) - API reference
- [APPLICATION_FLOWS.md](APPLICATION_FLOWS.md) - Business logic flows
- [CONFIGURATION.md](CONFIGURATION.md) - Setup and configuration
