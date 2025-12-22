# Application Flows

## Overview

This document describes the key business logic flows in the PartyTime Server, including detailed sequence diagrams and decision trees for drop submission, party lifecycle management, and cleanup operations.

---

## Table of Contents

- [Drop Submission Flow](#drop-submission-flow)
- [Party Lifecycle](#party-lifecycle)
- [Duplicate Detection](#duplicate-detection)
- [Average Drop Calculation](#average-drop-calculation)
- [Cleanup Process](#cleanup-process)
- [Query Flow](#query-flow)

---

## Drop Submission Flow

### High-Level Flow

```
┌──────────┐
│  Client  │
└────┬─────┘
     │ POST /api/drops
     │ {world, itemId, itemName, quantity, value}
     ▼
┌─────────────────────┐
│  DropsController    │
│  .drops()           │
└─────────┬───────────┘
          │ @Valid DropRequest
          │ Validation
          ▼
     ┌────────┐
     │ Valid? │
     └───┬────┘
         │
    ┌────┴────┐
    │         │
   Yes       No
    │         │
    │         └──────> 400 Bad Request
    │                 (validation errors)
    ▼
┌─────────────────────┐
│   DropService       │
│   .handleDrop()     │
└─────────┬───────────┘
          │ @Transactional
          │
          ▼
   ┌──────────────────────┐
   │ Find active party    │
   │ on this world?       │
   └──────┬───────────────┘
          │
     ┌────┴────┐
     │         │
    Yes       No
     │         │
     │         └─────────────────┐
     │                           ▼
     │                    ┌──────────────┐
     │                    │ Create new   │
     │                    │ DropParty    │
     │                    │ is_active=T  │
     │                    └──────┬───────┘
     │                           │
     ▼                           │
┌──────────────────┐             │
│ Check timeout    │             │
│ last_drop + 5min │             │
│ < now?           │             │
└────┬─────────────┘             │
     │                           │
 ┌───┴───┐                       │
 │       │                       │
Yes     No                       │
 │       │                       │
 │       └─────────┐             │
 │                 │             │
 ▼                 │             │
┌──────────────┐   │             │
│ End old      │   │             │
│ party        │   │             │
│ is_active=F  │   │             │
└──────┬───────┘   │             │
       │           │             │
       │           │             │
       ▼           │             │
┌──────────────┐   │             │
│ Create new   │   │             │
│ party        │   │             │
└──────┬───────┘   │             │
       │           │             │
       └───────────┤             │
                   │             │
                   ▼             │
            ┌──────────────┐    │
            │ Check        │    │
            │ duplicate    │◄───┘
            │ within 500ms?│
            └──┬───────────┘
               │
          ┌────┴────┐
          │         │
         Yes       No
          │         │
          │         ▼
          │    ┌─────────────┐
          │    │ Create Drop │
          │    │ entity      │
          │    └─────┬───────┘
          │          │
          │          ▼
          │    ┌─────────────┐
          │    │ Add to      │
          │    │ party.drops │
          │    └─────┬───────┘
          │          │
          │          ▼
          │    ┌─────────────┐
          │    │ Recalculate │
          │    │ avg_drop    │
          │    └─────┬───────┘
          │          │
          │          ▼
          │    ┌─────────────┐
          │    │ Update      │
          │    │ last_drop_at│
          │    └─────┬───────┘
          │          │
          │          ▼
          │    ┌─────────────┐
          │    │ Save to DB  │
          │    └─────┬───────┘
          │          │
          ▼          ▼
   ┌────────────────────────┐
   │  Return DropResponse   │
   │  - party_created       │
   │  - drop_added          │
   │  - duplicate           │
   └────────────────────────┘
```

### Detailed Sequence Diagram

```
Client          Controller       Service          Repository        Database
  │                 │               │                 │                │
  │ POST /drops     │               │                 │                │
  ├────────────────>│               │                 │                │
  │                 │               │                 │                │
  │                 │ validate      │                 │                │
  │                 │ request       │                 │                │
  │                 │───────┐       │                 │                │
  │                 │       │       │                 │                │
  │                 │<──────┘       │                 │                │
  │                 │               │                 │                │
  │                 │ handleDrop()  │                 │                │
  │                 ├──────────────>│                 │                │
  │                 │               │                 │                │
  │                 │               │ findByWorldAnd  │                │
  │                 │               │ IsActiveTrue()  │                │
  │                 │               ├────────────────>│                │
  │                 │               │                 │ SELECT * FROM  │
  │                 │               │                 │ drop_party...  │
  │                 │               │                 ├───────────────>│
  │                 │               │                 │                │
  │                 │               │                 │ DropParty      │
  │                 │               │                 │ or null        │
  │                 │               │                 │<───────────────┤
  │                 │               │ Optional<DP>    │                │
  │                 │               │<────────────────┤                │
  │                 │               │                 │                │
  │                 │               │ [if exists]     │                │
  │                 │               │ check timeout   │                │
  │                 │               │───────┐         │                │
  │                 │               │       │         │                │
  │                 │               │<──────┘         │                │
  │                 │               │                 │                │
  │                 │               │ [if not timeout]│                │
  │                 │               │ check duplicate │                │
  │                 │               │───────┐         │                │
  │                 │               │       │         │                │
  │                 │               │<──────┘         │                │
  │                 │               │                 │                │
  │                 │               │ [if not dup]    │                │
  │                 │               │ create Drop     │                │
  │                 │               │───────┐         │                │
  │                 │               │       │         │                │
  │                 │               │<──────┘         │                │
  │                 │               │                 │                │
  │                 │               │ add to party    │                │
  │                 │               │───────┐         │                │
  │                 │               │       │         │                │
  │                 │               │<──────┘         │                │
  │                 │               │                 │                │
  │                 │               │ calcAvgDrop()   │                │
  │                 │               │───────┐         │                │
  │                 │               │       │         │                │
  │                 │               │<──────┘         │                │
  │                 │               │                 │                │
  │                 │               │ save()          │                │
  │                 │               ├────────────────>│                │
  │                 │               │                 │ INSERT/UPDATE  │
  │                 │               │                 ├───────────────>│
  │                 │               │                 │                │
  │                 │               │                 │ ✓              │
  │                 │               │                 │<───────────────┤
  │                 │               │ DropParty       │                │
  │                 │               │<────────────────┤                │
  │                 │               │                 │                │
  │                 │ DropResponse  │                 │                │
  │                 │<──────────────┤                 │                │
  │                 │               │                 │                │
  │ 200 OK          │               │                 │                │
  │ DropResponse    │               │                 │                │
  │<────────────────┤               │                 │                │
  │                 │               │                 │                │
```

### Code References

**Entry Point**: `src/main/java/rip/jade/partytimeserverjava/Controller/DropsController.java:13`

```java
@PostMapping
public ResponseEntity<DropResponse> drops(@Valid @RequestBody DropRequest request) {
    DropResponse response = dropService.handleDrop(request);
    return ResponseEntity.ok(response);
}
```

**Core Logic**: `src/main/java/rip/jade/partytimeserverjava/service/DropService.java:35`

```java
@Transactional
public DropResponse handleDrop(DropRequest request) {
    // Implementation handles all scenarios
}
```

---

## Party Lifecycle

### State Diagram

```
                    ┌─────────────┐
                    │   [START]   │
                    └──────┬──────┘
                           │
                           │ First drop
                           │ on world
                           ▼
                  ┌─────────────────┐
                  │  PARTY CREATED  │
                  │  is_active=true │
                  │  created_at=NOW │
                  │  last_drop=NOW  │
                  │  avg_drop=calc  │
                  │  dropCount=1    │
                  └────────┬────────┘
                           │
                           │ Additional
                           │ drops
                           ▼
    ┌─────────────────────────────────────────┐
    │          PARTY ACTIVE                   │
    │          is_active=true                 │
    │          dropCount++                    │
    │          avg_drop recalculated          │
    │          last_drop_at updated           │
    └────────┬──────────────────────┬─────────┘
             │                      │
             │                      │ No drops for
             │ Continues            │ 5+ minutes
             │ receiving            │
             │ drops                ▼
             │            ┌──────────────────┐
             │            │ PARTY INACTIVE   │
             │            │ is_active=false  │
             │            │ (via cleanup)    │
             │            └──────────────────┘
             │                      │
             │                      │ Drops &
             │                      │ party data
             │                      │ preserved
             │                      ▼
             │            ┌──────────────────┐
             └───────────>│   HISTORICAL     │
                          │   (in database)  │
                          └──────────────────┘
```

### Party Creation

**Trigger**: First drop on a world with no active party

**Steps**:
1. Create new `DropParty` entity
2. Set `is_active = true`
3. Set `created_at = NOW()`
4. Set `last_drop_at = NOW()`
5. Create first `Drop` entity
6. Add drop to party's collection
7. Calculate initial `avg_drop` (equals first drop value)
8. Save to database (transaction committed)

**Code**: `src/main/java/rip/jade/partytimeserverjava/service/DropService.java:101`

### Party Update

**Trigger**: Additional drop on a world with existing active party

**Conditions**:
- Party must exist
- Party must be active (`is_active = true`)
- Not timed out (last drop within 5 minutes)
- Not a duplicate (not within 500ms window)

**Steps**:
1. Create new `Drop` entity
2. Add to existing party's `drops` collection
3. Recalculate `avg_drop` using all drops
4. Update `last_drop_at = NOW()`
5. Save to database

**Code**: `src/main/java/rip/jade/partytimeserverjava/service/DropService.java:46-90`

### Party Deactivation

**Trigger**: No drops received for 5+ minutes

**Methods**:
1. **Automatic** (scheduled): Runs every 2 minutes
2. **Real-time**: Checked when new drop arrives on same world
3. **Manual**: Via POST /api/party/cleanup

**Steps**:
1. Check if `last_drop_at + 5 minutes < NOW()`
2. Set `is_active = false`
3. Save to database
4. Party and all drops remain in database

**Code**: `src/main/java/rip/jade/partytimeserverjava/service/DropService.java:149`

---

## Duplicate Detection

### Purpose

Prevent multiple clients from reporting the same drop event when they witness it simultaneously.

### Algorithm

```
┌─────────────────────────────────┐
│ New drop submitted              │
│ on world W at time T            │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ Find active party on world W    │
└────────────┬────────────────────┘
             │
             ▼
        ┌─────────┐
        │ Exists? │
        └────┬────┘
             │
         ┌───┴───┐
        Yes     No
         │       │
         │       └────> Not duplicate
         │              (new party)
         ▼
┌─────────────────────────────────┐
│ Get party.lastDropAt            │
│ time L                          │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ Calculate:                      │
│ timeDiff = T - L                │
└────────────┬────────────────────┘
             │
             ▼
    ┌────────────────────┐
    │ timeDiff < 500ms?  │
    └────┬───────────────┘
         │
    ┌────┴────┐
   Yes       No
    │         │
    │         └────> Not duplicate
    │                (add drop)
    ▼
┌─────────────────────────────────┐
│ DUPLICATE DETECTED              │
│ Return "duplicate" status       │
│ Do NOT add drop                 │
└─────────────────────────────────┘
```

### Configuration

**Property**: `party.duplicate-window-millis`

**Default**: `500` (0.5 seconds)

**Location**: `application.properties`

### Example Timeline

```
Time         Event                           Result
─────────────────────────────────────────────────────
10:00:00.000 Client A reports drop          party_created
10:00:00.200 Client B reports same drop     duplicate (200ms < 500ms)
10:00:00.400 Client C reports same drop     duplicate (400ms < 500ms)
10:00:00.600 Client D reports new drop      drop_added (600ms > 500ms)
```

### Code Reference

`src/main/java/rip/jade/partytimeserverjava/service/DropService.java:84`

---

## Average Drop Calculation

### Formula

```
avg_drop = SUM(drop.value * drop.quantity) / COUNT(drops)
```

### Calculation Steps

```
┌─────────────────────────────────┐
│ New drop added to party         │
│ value = V                       │
│ quantity = Q                    │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ Get all drops in party          │
│ drops = party.getDrops()        │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ Calculate total value:          │
│ totalValue = 0                  │
│ for each drop:                  │
│   totalValue +=                 │
│     drop.value * drop.quantity  │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ Calculate average:              │
│ avgDrop = totalValue / dropCount│
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ Update party.avgDrop            │
│ Save to database                │
└─────────────────────────────────┘
```

### Example

```
Party has 3 drops:
  Drop 1: value = 50,000,000 GP, quantity = 1
  Drop 2: value = 30,000,000 GP, quantity = 1
  Drop 3: value = 10,000,000 GP, quantity = 2

Total value = (50M * 1) + (30M * 1) + (10M * 2)
            = 50M + 30M + 20M
            = 100,000,000 GP

Drop count = 3

Average = 100,000,000 / 3
        = 33,333,333 GP
```

### Code Reference

`src/main/java/rip/jade/partytimeserverjava/service/DropService.java:123`

```java
private Integer calculateAverageDrop(DropParty party) {
    List<Drop> drops = party.getDrops();
    if (drops.isEmpty()) {
        return 0;
    }

    int totalValue = drops.stream()
        .mapToInt(drop -> drop.getValue() * drop.getQuantity())
        .sum();

    return totalValue / drops.size();
}
```

---

## Cleanup Process

### Overview

The cleanup process identifies and deactivates parties that haven't received drops for 5+ minutes.

### Trigger Methods

1. **Scheduled Automatic Cleanup**
   - Runs every 2 minutes
   - Configured via `@Scheduled` annotation
   - Independent of API requests

2. **Real-time Timeout Check**
   - Runs when new drop submitted to same world
   - Checks existing party before adding drop
   - Creates new party if timed out

3. **Manual Cleanup**
   - Triggered via POST /api/party/cleanup
   - Same logic as scheduled cleanup
   - Returns count of parties cleaned

### Flow Diagram

```
┌─────────────────────┐
│ CLEANUP TRIGGERED   │
│ - Scheduled (2 min) │
│ - Manual API call   │
│ - New drop check    │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────────────┐
│ Find all active parties     │
│ WHERE is_active = true      │
└─────────┬───────────────────┘
          │
          ▼
┌─────────────────────────────┐
│ For each active party:      │
└─────────┬───────────────────┘
          │
          ▼
    ┌─────────────────────────────┐
    │ Calculate timeout:          │
    │ timeSince = NOW - lastDrop  │
    └──────────┬──────────────────┘
               │
               ▼
      ┌────────────────────┐
      │ timeSince > 5 min? │
      └─────┬──────────────┘
            │
       ┌────┴────┐
      Yes       No
       │         │
       │         └───> Keep active
       │               (skip)
       ▼
┌──────────────────┐
│ Set is_active    │
│ = false          │
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ Save to DB       │
└──────┬───────────┘
       │
       └───> Count++
              │
              ▼
       ┌─────────────┐
       │ Return count│
       └─────────────┘
```

### Configuration

**Timeout Duration**:
- Property: `party.timeout-minutes`
- Default: `5` minutes
- Location: `application.properties`

**Cleanup Interval**:
- Property: `party.cleanup-interval-millis`
- Default: `120000` (2 minutes)
- Location: `application.properties`

### Scheduled Cleanup Details

**Annotation**: `@Scheduled(fixedDelayString = "${party.cleanup-interval-millis}")`

**Behavior**:
- Fixed delay (not fixed rate)
- Waits 2 minutes after previous execution completes
- Runs in separate thread
- Enabled by `@EnableScheduling` in main application

**Code**: `src/main/java/rip/jade/partytimeserverjava/service/DropService.java:144`

```java
@Scheduled(fixedDelayString = "${party.cleanup-interval-millis}")
public void cleanupInactiveParties() {
    List<DropParty> activeParties = dropPartyRepository.findByIsActiveTrue();
    Instant cutoffTime = Instant.now().minus(partyTimeoutMinutes, ChronoUnit.MINUTES);

    for (DropParty party : activeParties) {
        if (party.getLastDropAt().isBefore(cutoffTime)) {
            party.setIsActive(false);
            dropPartyRepository.save(party);
        }
    }
}
```

### Example Timeline

```
Time         Event                               Status
────────────────────────────────────────────────────────────
10:00:00     Party created on world 301         is_active=true
10:02:00     Last drop received                 is_active=true
10:04:00     Scheduled cleanup runs             is_active=true (2 min since last)
10:06:00     Scheduled cleanup runs             is_active=true (4 min since last)
10:07:01     5 minutes since last drop          still active
10:08:00     Scheduled cleanup runs             is_active=FALSE (6 min since last)
```

---

## Query Flow

### Get Active Parties

**Endpoint**: GET /api/party

**Flow**:

```
Client
  │
  │ GET /api/party
  ▼
PartyController
  │
  │ getActiveParties()
  ▼
DropService
  │
  │ findByIsActiveTrue()
  ▼
DropPartyRepository
  │
  │ SELECT * FROM drop_party WHERE is_active = true
  ▼
Database
  │
  │ List<DropParty>
  ▼
DropService
  │
  │ map to DropPartyResponse
  │ - get drop count via drops.size()
  ▼
PartyController
  │
  │ List<DropPartyResponse>
  ▼
Client
```

**Optimization**: Uses database index on `is_active` column for fast filtering.

**Code**: `src/main/java/rip/jade/partytimeserverjava/Controller/PartyController.java:12`

---

## Transaction Boundaries

### Key Transactions

**Drop Submission** (`@Transactional`):
- Ensures atomic creation of party + drop
- Rollback if database error occurs
- Prevents orphaned drops

**Cleanup** (`@Transactional`):
- Ensures consistent state updates
- Multiple party updates in single transaction

### Isolation Level

**Default**: PostgreSQL default (READ COMMITTED)

**Implications**:
- Concurrent drops on different worlds: No conflict
- Concurrent drops on same world: Serialized via database locks
- Duplicate detection: Relies on transaction isolation

---

## Error Handling

### Validation Errors

**Layer**: Controller (via `@Valid`)

**Response**: 400 Bad Request with field errors

**Flow**: Request → Validation → Error → Client (no service/DB interaction)

### Database Errors

**Layer**: Service/Repository

**Response**: 500 Internal Server Error

**Flow**: Service → Repository → Exception → Controller → Client

### Recommended Improvements

From TODO.md:
- Add GlobalExceptionHandler
- Custom error responses
- Proper logging
- Error tracking (e.g., Sentry)

---

## Performance Considerations

### Database Queries per Drop

**Optimized path** (drop added to existing party):
1. SELECT party by world (indexed)
2. INSERT drop
3. UPDATE party

**Total**: 1 SELECT, 1 INSERT, 1 UPDATE

**Worst case** (timeout, new party created):
1. SELECT old party
2. UPDATE old party (set inactive)
3. INSERT new party
4. INSERT drop

**Total**: 1 SELECT, 1 UPDATE, 2 INSERTs

### Indexing Strategy

Indexes optimize:
- Finding active party by world: `idx_world_active`
- Cleanup queries: `idx_active_last_drop`

See [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md#indexes) for details.

---

## Future Flow Enhancements

Based on TODO.md:

1. **Authentication Flow**:
   - API key validation before drop submission
   - JWT token validation for admin endpoints

2. **Rate Limiting Flow**:
   - Check request count per client
   - Return 429 Too Many Requests if exceeded

3. **WebSocket Flow**:
   - Real-time push of new parties to connected clients
   - Event-driven updates on drop additions

4. **Analytics Flow**:
   - Background job aggregating statistics
   - Pre-calculated metrics for dashboard
