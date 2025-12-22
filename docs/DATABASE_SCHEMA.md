# Database Schema Documentation

## Overview

The PartyTime Server uses PostgreSQL as its database with two primary entities: `DropParty` and `Drop`. The schema is designed to efficiently track and aggregate drop party events across different game worlds.

## Entity Relationship Diagram

```
┌─────────────────────────────┐
│       DropParty             │
├─────────────────────────────┤
│ id (PK)          BIGSERIAL  │
│ world            INTEGER    │
│ avg_drop         INTEGER    │
│ is_active        BOOLEAN    │
│ created_at       TIMESTAMP  │
│ last_drop_at     TIMESTAMP  │
└─────────────────────────────┘
          │
          │ 1
          │
          │ cascade ALL
          │ orphanRemoval
          │
          │ *
          ▼
┌─────────────────────────────┐
│         Drop                │
├─────────────────────────────┤
│ id (PK)          BIGSERIAL  │
│ item_id          INTEGER    │
│ item_name        VARCHAR    │
│ quantity         INTEGER    │
│ value            INTEGER    │
│ drop_party_id(FK) BIGINT   │
└─────────────────────────────┘
```

## Tables

### DropParty Table

**Table Name**: `drop_party`

**Description**: Represents a drop party session on a specific game world. Multiple drops are aggregated into a single party.

**Columns**:

| Column Name   | Type      | Nullable | Default | Description |
|--------------|-----------|----------|---------|-------------|
| id           | BIGSERIAL | NO       | AUTO    | Primary key, auto-generated |
| world        | INTEGER   | NO       | -       | Game world number (0-999) |
| avg_drop     | INTEGER   | NO       | -       | Average drop value in GP (gold pieces) |
| is_active    | BOOLEAN   | NO       | -       | Whether the party is currently active |
| created_at   | TIMESTAMP | NO       | NOW()   | When the party was first created |
| last_drop_at | TIMESTAMP | NO       | NOW()   | Timestamp of the most recent drop added |

**Indexes**:

```sql
-- Composite index for finding active parties by world
CREATE INDEX idx_world_active ON drop_party(world, is_active);

-- Composite index for cleanup operations
CREATE INDEX idx_active_last_drop ON drop_party(is_active, last_drop_at);
```

**Entity Class**: `src/main/java/rip/jade/partytimeserverjava/entity/DropParty.java:7`

**Relationships**:
- One-to-Many with `Drop` (cascade ALL operations, orphan removal enabled)
- When a DropParty is deleted, all associated Drops are automatically deleted
- When a Drop is removed from the collection, it's automatically deleted from database

---

### Drop Table

**Table Name**: `drop`

**Description**: Represents an individual item drop reported by game clients. Each drop belongs to exactly one DropParty.

**Columns**:

| Column Name    | Type      | Nullable | Default | Description |
|---------------|-----------|----------|---------|-------------|
| id            | BIGSERIAL | NO       | AUTO    | Primary key, auto-generated |
| item_id       | INTEGER   | NO       | -       | Game item identifier |
| item_name     | VARCHAR   | NO       | -       | Human-readable item name |
| quantity      | INTEGER   | NO       | -       | Number of items dropped |
| value         | INTEGER   | NO       | -       | Value per item in GP |
| drop_party_id | BIGINT    | NO       | -       | Foreign key to DropParty |

**Foreign Keys**:

```sql
ALTER TABLE drop
  ADD CONSTRAINT fk_drop_party
  FOREIGN KEY (drop_party_id)
  REFERENCES drop_party(id)
  ON DELETE CASCADE;
```

**Entity Class**: `src/main/java/rip/jade/partytimeserverjava/entity/Drop.java:9`

**Relationships**:
- Many-to-One with `DropParty` (lazy loaded)
- Cannot exist without a parent DropParty (required relationship)

---

## Database Operations

### Common Queries

#### Find Active Parties on a Specific World

```java
// Repository method
Optional<DropParty> findByWorldAndIsActiveTrue(Integer world);
```

```sql
SELECT * FROM drop_party
WHERE world = ? AND is_active = true;
```

#### Get All Drops for a Party

```java
// Repository method
List<Drop> findByDropPartyId(Long dropPartyId);
```

```sql
SELECT * FROM drop
WHERE drop_party_id = ?;
```

#### Find High-Value Parties

```java
// Repository method
List<DropParty> findByAvgDropGreaterThan(Integer minValue);
```

```sql
SELECT * FROM drop_party
WHERE avg_drop > ?;
```

#### Cleanup Inactive Parties

```sql
UPDATE drop_party
SET is_active = false
WHERE is_active = true
  AND last_drop_at < (NOW() - INTERVAL '5 minutes');
```

#### Calculate Total Party Value

```java
// Custom query in DropPartyRepository:39
@Query("SELECT SUM(d.value * d.quantity) FROM Drop d WHERE d.dropParty = :dropParty")
Integer getTotalValue(@Param("dropParty") DropParty dropParty);
```

```sql
SELECT SUM(value * quantity)
FROM drop
WHERE drop_party_id = ?;
```

---

## Data Integrity Rules

### Cascading Operations

**DropParty → Drop Cascade**:
- **DELETE**: When a DropParty is deleted, all associated Drops are automatically deleted
- **PERSIST**: When a DropParty is saved, all new Drops in its collection are saved
- **MERGE**: When a DropParty is updated, all Drops are updated
- **REFRESH**: When a DropParty is refreshed, all Drops are refreshed
- **REMOVE**: Explicitly deleting a DropParty deletes all Drops
- **ORPHAN REMOVAL**: If a Drop is removed from the DropParty.drops collection, it's deleted from the database

### Constraints

**DropParty Constraints**:
- `world` must be a valid integer (application layer validates 0-999)
- `avg_drop` must be non-null (calculated on every drop addition)
- `is_active` must be non-null (defaults to true on creation)
- `created_at` and `last_drop_at` are auto-managed by JPA

**Drop Constraints**:
- All fields are required (NOT NULL)
- `drop_party_id` must reference an existing DropParty
- `value` must be >= 15000 GP (application layer validation)
- `quantity` must be >= 1 (application layer validation)

---

## Performance Considerations

### Indexes

The schema includes strategic indexes to optimize common queries:

1. **idx_world_active** (world, is_active):
   - Optimizes finding active parties on a specific world
   - Used heavily by the drop submission flow
   - Composite index allows efficient WHERE clauses on both columns

2. **idx_active_last_drop** (is_active, last_drop_at):
   - Optimizes the cleanup process
   - Allows efficient identification of timed-out parties
   - Used by scheduled cleanup job every 2 minutes

### Lazy Loading

Drops are lazy-loaded by default when fetching a DropParty:
- Reduces initial query overhead when only party metadata is needed
- Use `findByIdWithDrops()` for eager loading when drops are required

```java
// Eager fetch with JOIN
@Query("SELECT dp FROM DropParty dp LEFT JOIN FETCH dp.drops WHERE dp.id = :id")
Optional<DropParty> findByIdWithDrops(@Param("id") Long id);
```

---

## Schema Management

### Hibernate DDL Auto

**Configuration**: `application.properties`

```properties
spring.jpa.hibernate.ddl-auto=update
```

**Behavior**:
- Automatically creates tables if they don't exist
- Updates existing tables to match entity definitions
- **Does NOT drop columns** (safe for production)
- Indexes defined in `@Table` annotations are auto-created

### Migration Strategy

For production deployments, consider:
1. Switching to `spring.jpa.hibernate.ddl-auto=validate`
2. Using a migration tool like Flyway or Liquibase
3. Version-controlling schema changes

---

## Data Types and Sizes

### Storage Estimates

**Per DropParty Record**:
- Fixed: ~41 bytes (id, world, avg_drop, is_active, timestamps)
- Total: ~41 bytes per party

**Per Drop Record**:
- Fixed: ~28 bytes (id, item_id, quantity, value, drop_party_id)
- Variable: item_name length (average ~20 bytes)
- Total: ~48 bytes per drop

**Example Storage**:
- 1,000 active parties with 20 drops each = ~1 MB
- 10,000 parties with 50 drops each = ~25 MB

The schema is designed for efficient storage and fast queries even with millions of drops.

---

## Lifecycle Management

### Party Creation
1. Client submits first drop on a world with no active party
2. New DropParty created with `is_active = true`
3. Drop added to party's collection
4. Both entities saved in single transaction

### Party Updates
1. Additional drops added to existing active party
2. `avg_drop` recalculated: `SUM(value * quantity) / drop_count`
3. `last_drop_at` updated to current timestamp
4. Changes persisted atomically

### Party Deactivation
1. Triggered when `last_drop_at` > 5 minutes ago
2. `is_active` set to false
3. Party remains in database for historical queries
4. Drops are preserved (not deleted)

### Cleanup Process
- Runs every 2 minutes via scheduled task
- Marks inactive parties where `last_drop_at` + 5 minutes < NOW()
- Can also be triggered manually via API

---

## Future Schema Considerations

Based on TODO.md, potential additions:

1. **User/Authentication Tables**:
   - API keys for client authentication
   - JWT tokens for admin access
   - Role-based access control

2. **Audit/History Tables**:
   - Track party lifecycle events
   - Log API requests for analytics

3. **Analytics Tables**:
   - Pre-aggregated statistics by world
   - Historical trends and patterns

4. **Additional Indexes**:
   - item_id index on Drop table for item-specific queries
   - created_at index for time-based queries
