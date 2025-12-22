# PartyTime Server Documentation

Welcome to the PartyTime Server documentation. This directory contains comprehensive documentation for the PartyTime Server application - a Spring Boot REST API for tracking MMORPG drop party events in real-time.

---

## Documentation Index

### Getting Started

Start here if you're new to the project:

1. **[CONFIGURATION.md](CONFIGURATION.md)** - Setup and configuration guide
   - Prerequisites and installation
   - Development environment setup
   - Running the application
   - Configuration properties reference
   - Production deployment guide

### Core Documentation

Understand the system architecture and design:

2. **[ARCHITECTURE.md](ARCHITECTURE.md)** - System architecture overview
   - Technology stack
   - Layered architecture
   - Design patterns
   - Scalability considerations
   - Security architecture
   - Future enhancements

3. **[DATABASE_SCHEMA.md](DATABASE_SCHEMA.md)** - Database design and schema
   - Entity relationship diagram
   - Table structures and columns
   - Indexes and constraints
   - Common queries
   - Performance considerations
   - Lifecycle management

### API & Business Logic

Learn how the application works:

4. **[API_DOCUMENTATION.md](API_DOCUMENTATION.md)** - REST API reference
   - All available endpoints
   - Request/response formats
   - Validation rules
   - Error handling
   - Testing examples
   - Security considerations

5. **[APPLICATION_FLOWS.md](APPLICATION_FLOWS.md)** - Business logic flows
   - Drop submission flow
   - Party lifecycle
   - Duplicate detection
   - Cleanup process
   - Transaction boundaries
   - Performance optimization

---

## Quick Links

### For Developers

- **First Time Setup**: [CONFIGURATION.md - Development Setup](CONFIGURATION.md#development-setup)
- **Project Structure**: [ARCHITECTURE.md - Project Structure](ARCHITECTURE.md#project-structure)
- **Running the App**: [CONFIGURATION.md - Running the Application](CONFIGURATION.md#running-the-application)
- **API Testing**: [API_DOCUMENTATION.md - Testing the API](API_DOCUMENTATION.md#testing-the-api)

### For Operations

- **Production Deployment**: [CONFIGURATION.md - Production Configuration](CONFIGURATION.md#production-configuration)
- **Environment Variables**: [CONFIGURATION.md - Environment Variables](CONFIGURATION.md#environment-variables)
- **Troubleshooting**: [CONFIGURATION.md - Troubleshooting](CONFIGURATION.md#troubleshooting)
- **Monitoring**: [ARCHITECTURE.md - Monitoring & Observability](ARCHITECTURE.md#monitoring--observability)

### For Architects

- **System Design**: [ARCHITECTURE.md - System Architecture](ARCHITECTURE.md#system-architecture)
- **Scalability**: [ARCHITECTURE.md - Scalability Considerations](ARCHITECTURE.md#scalability-considerations)
- **Database Design**: [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md)
- **Future Architecture**: [ARCHITECTURE.md - Future Enhancements](ARCHITECTURE.md#future-architectural-enhancements)

### For API Consumers

- **API Endpoints**: [API_DOCUMENTATION.md](API_DOCUMENTATION.md)
- **Request Examples**: [API_DOCUMENTATION.md - Testing the API](API_DOCUMENTATION.md#testing-the-api)
- **Error Responses**: [API_DOCUMENTATION.md - Error Responses](API_DOCUMENTATION.md#error-responses)

---

## What is PartyTime Server?

PartyTime Server is a Spring Boot application designed to track "drop parties" in MMORPG games (specifically OSRS - Old School RuneScape). When players drop valuable items in game worlds, multiple clients report these drops to the server, which aggregates them into party sessions.

### Key Features

- **Real-time Drop Tracking**: Accept drop reports from game clients
- **Party Aggregation**: Group drops by world into party sessions
- **Duplicate Detection**: Filter out duplicate reports within 500ms window
- **Automatic Cleanup**: Deactivate parties with no activity for 5+ minutes
- **Active Party Queries**: Retrieve all currently active drop parties
- **Average Value Calculation**: Calculate average drop value per party

### Technology Stack

- **Framework**: Spring Boot 4.0.0
- **Language**: Java 25
- **Database**: PostgreSQL
- **Build Tool**: Gradle 8.11.1
- **Architecture**: Layered (Controller → Service → Repository → Entity)

---

## Project Status

### Current Version: 0.0.1-SNAPSHOT

**Status**: Development

**Completed Features**:
- ✅ Core drop submission API
- ✅ Party query API
- ✅ Duplicate detection
- ✅ Automatic cleanup
- ✅ Database schema with indexes
- ✅ Input validation
- ✅ Transaction management

**Planned Features** (see `../TODO.md`):
- ⏳ Authentication (API keys, JWT)
- ⏳ Authorization (RBAC)
- ⏳ Pagination on queries
- ⏳ Unit and integration tests
- ⏳ Swagger/OpenAPI documentation
- ⏳ Global exception handler
- ⏳ Metrics and monitoring
- ⏳ Rate limiting

---

## API Overview

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/drops` | Submit a new drop report |
| GET | `/api/party` | Get all active parties |
| POST | `/api/party/cleanup` | Manually trigger cleanup |

### Example: Submit a Drop

```bash
curl -X POST http://localhost:8000/api/drops \
  -H "Content-Type: application/json" \
  -d '{
    "world": 301,
    "itemId": 13576,
    "itemName": "Dragon Warhammer",
    "quantity": 1,
    "value": 50000000
  }'
```

**Response**:
```json
{
  "status": "party_created",
  "world": 301,
  "dropPartyId": 1,
  "message": "New drop party created for world 301"
}
```

### Example: Get Active Parties

```bash
curl http://localhost:8000/api/party
```

**Response**:
```json
[
  {
    "id": 1,
    "world": 301,
    "avgDrop": 50000000,
    "isActive": true,
    "createdAt": "2025-12-21T10:00:00Z",
    "lastDropAt": "2025-12-21T10:00:00Z",
    "dropCount": 1
  }
]
```

For complete API documentation, see [API_DOCUMENTATION.md](API_DOCUMENTATION.md).

---

## Database Schema

### Entity Relationship

```
DropParty (1) ←──── (Many) Drop
   |                      |
   |- id                  |- id
   |- world               |- item_id
   |- avg_drop            |- item_name
   |- is_active           |- quantity
   |- created_at          |- value
   |- last_drop_at        |- drop_party_id (FK)
```

For complete schema documentation, see [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md).

---

## Application Flow

### Drop Submission Flow

```
Client → Controller → Validate Request
                    ↓
                Service → Check for active party
                    ↓
            Party exists?
                ↙     ↘
              Yes      No
               ↓        ↓
        Check timeout   Create party
               ↓        ↓
        Check duplicate Add drop
               ↓
           Add drop
               ↓
        Update avg_drop
               ↓
         Save to DB
               ↓
        Return response
```

For detailed flow diagrams, see [APPLICATION_FLOWS.md](APPLICATION_FLOWS.md).

---

## Configuration

### Key Configuration Properties

```properties
# Server
server.port=8000

# Database
spring.datasource.url=jdbc:postgresql://localhost:7070/partytime
spring.datasource.username=partytime
spring.datasource.password=partytime123

# Party Settings
party.duplicate-window-millis=500      # 0.5 second duplicate window
party.timeout-minutes=5                # 5 minute party timeout
party.cleanup-interval-millis=120000   # 2 minute cleanup interval
```

For complete configuration guide, see [CONFIGURATION.md](CONFIGURATION.md).

---

## Quick Start

### 1. Prerequisites

- Java 25+
- PostgreSQL (or Docker)
- Gradle 8.11.1+ (wrapper included)

### 2. Setup Database

**Using Docker**:
```bash
docker run -d \
  --name partytime-postgres \
  -e POSTGRES_DB=partytime \
  -e POSTGRES_USER=partytime \
  -e POSTGRES_PASSWORD=partytime123 \
  -p 7070:5432 \
  postgres:16
```

### 3. Run Application

```bash
./gradlew bootRun
```

Application starts on `http://localhost:8000`

### 4. Test API

```bash
# Get active parties (should be empty initially)
curl http://localhost:8000/api/party

# Submit a test drop
curl -X POST http://localhost:8000/api/drops \
  -H "Content-Type: application/json" \
  -d '{
    "world": 301,
    "itemId": 1,
    "itemName": "Test Item",
    "quantity": 1,
    "value": 15000
  }'

# Check active parties again
curl http://localhost:8000/api/party
```

For detailed setup instructions, see [CONFIGURATION.md - Development Setup](CONFIGURATION.md#development-setup).

---

## Known Issues

### Missing Features

See `../TODO.md` for complete list of planned enhancements:
- No authentication/authorization
- No error handling (GlobalExceptionHandler)
- No unit tests
- No pagination
- No API documentation (Swagger)

---

## Contributing

### Code Structure Guidelines

When modifying the codebase:

1. **Controllers**: Only handle HTTP concerns, delegate to services
2. **Services**: Implement business logic, use `@Transactional`
3. **Repositories**: Only data access, no business logic
4. **Entities**: JPA mappings, minimal logic
5. **DTOs**: API contracts, validation annotations

### Before Submitting Changes

- [ ] Run tests: `./gradlew test` (when implemented)
- [ ] Build successfully: `./gradlew clean build`
- [ ] Update relevant documentation
- [ ] Follow existing code style
- [ ] Add appropriate validation

---

## Support

### Getting Help

1. Check [CONFIGURATION.md - Troubleshooting](CONFIGURATION.md#troubleshooting)
2. Review relevant documentation files
3. Check `../TODO.md` for known limitations
4. Search existing issues

### Reporting Issues

When reporting issues, include:
- Application version
- Java version
- PostgreSQL version
- Full error message and stack trace
- Steps to reproduce
- Configuration (redact secrets)

---

## License

[Include license information here]

---

## Changelog

### Version 0.0.1-SNAPSHOT (Current)

**Initial Release**:
- Core drop submission and party tracking functionality
- PostgreSQL database integration
- Automatic cleanup scheduler
- Input validation
- Basic API endpoints

---

## Further Reading

### External Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA Guide](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Jakarta Bean Validation](https://beanvalidation.org/2.0/)

### Related Documentation

- Project root `../TODO.md` - Development roadmap
- Project root `../README.md` - Project overview (if exists)
- `build.gradle` - Build configuration and dependencies
- `application.properties` - Current configuration

---

**Last Updated**: 2025-12-21

**Documentation Version**: 1.0.0
