# API Documentation

## Overview

The PartyTime Server exposes a RESTful API for tracking and querying drop party events. All endpoints return JSON responses and use standard HTTP status codes.

**Base URL**: `http://localhost:8000/api`

**Content-Type**: `application/json`

**Authentication**: None (currently open endpoints - see Security section)

---

## Table of Contents

- [Drops API](#drops-api)
  - [POST /api/drops](#post-apidrops) - Report a new drop
- [Party API](#party-api)
  - [GET /api/party](#get-apiparty) - Get all active parties
  - [POST /api/party/cleanup](#post-apipartycleanup) - Manually trigger cleanup
- [Error Responses](#error-responses)
- [Data Transfer Objects](#data-transfer-objects)

---

## Drops API

### POST /api/drops

Report a new item drop from a game client. The server will either create a new party, add the drop to an existing party, or mark it as a duplicate.

**Controller**: `src/main/java/rip/jade/partytimeserverjava/Controller/DropsController.java:13`

**Endpoint**: `POST /api/drops`

#### Request

**Headers**:
```
Content-Type: application/json
```

**Body**: `DropRequest`

```json
{
  "world": 301,
  "itemId": 13576,
  "itemName": "Dragon Warhammer",
  "quantity": 1,
  "value": 50000000
}
```

**Field Validation**:

| Field     | Type    | Required | Constraints | Description |
|-----------|---------|----------|-------------|-------------|
| world     | Integer | Yes      | Min: 0, Max: 999 | Game world number |
| itemId    | Integer | Yes      | Min: 1 | Game item identifier |
| itemName  | String  | Yes      | Not blank | Human-readable item name |
| quantity  | Integer | Yes      | Min: 1 | Number of items dropped |
| value     | Integer | Yes      | Min: 15000 | Value per item in GP (gold pieces) |

**Validation Source**: `src/main/java/rip/jade/partytimeserverjava/dto/DropRequest.java:8`

#### Response

**Success**: `200 OK`

**Body**: `DropResponse`

```json
{
  "status": "party_created",
  "world": 301,
  "dropPartyId": 42,
  "message": "New drop party created for world 301"
}
```

**Status Values**:

| Status        | Description |
|--------------|-------------|
| party_created | No active party existed on this world; created new party with this drop |
| drop_added   | Drop added to existing active party on this world |
| duplicate    | Drop reported within 500ms of last drop (ignored to prevent double-counting) |

**Response Schema**: `src/main/java/rip/jade/partytimeserverjava/dto/DropResponse.java:3`

#### Response Examples

**Scenario 1: First drop on a world (new party)**

Request:
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

Response:
```json
{
  "status": "party_created",
  "world": 301,
  "dropPartyId": 1,
  "message": "New drop party created for world 301"
}
```

**Scenario 2: Additional drop on active party**

Request:
```bash
curl -X POST http://localhost:8000/api/drops \
  -H "Content-Type: application/json" \
  -d '{
    "world": 301,
    "itemId": 11834,
    "itemName": "Bandos Chestplate",
    "quantity": 1,
    "value": 30000000
  }'
```

Response:
```json
{
  "status": "drop_added",
  "world": 301,
  "dropPartyId": 1,
  "message": "Drop added to existing party on world 301"
}
```

**Scenario 3: Duplicate drop (within 500ms)**

Request (sent 200ms after previous drop):
```bash
curl -X POST http://localhost:8000/api/drops \
  -H "Content-Type: application/json" \
  -d '{
    "world": 301,
    "itemId": 11834,
    "itemName": "Bandos Chestplate",
    "quantity": 1,
    "value": 30000000
  }'
```

Response:
```json
{
  "status": "duplicate",
  "world": 301,
  "dropPartyId": 1,
  "message": "Duplicate drop detected within 500ms window"
}
```

#### Error Responses

**400 Bad Request** - Validation failure

```json
{
  "timestamp": "2025-12-21T10:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "value",
      "message": "must be greater than or equal to 15000"
    }
  ]
}
```

Common validation errors:
- `world`: "must be between 0 and 999"
- `itemId`: "must be greater than or equal to 1"
- `itemName`: "must not be blank"
- `quantity`: "must be greater than or equal to 1"
- `value`: "must be greater than or equal to 15000"

---

## Party API

### GET /api/party

Retrieve all currently active drop parties across all worlds.

**Controller**: `src/main/java/rip/jade/partytimeserverjava/Controller/PartyController.java:12`

**Endpoint**: `GET /api/party`

#### Request

**Headers**: None required

**Query Parameters**: None

**Example**:
```bash
curl http://localhost:8000/api/party
```

#### Response

**Success**: `200 OK`

**Body**: Array of `DropPartyResponse`

```json
[
  {
    "id": 1,
    "world": 301,
    "avgDrop": 40000000,
    "isActive": true,
    "createdAt": "2025-12-21T10:00:00Z",
    "lastDropAt": "2025-12-21T10:15:30Z",
    "dropCount": 5
  },
  {
    "id": 2,
    "world": 302,
    "avgDrop": 25000000,
    "isActive": true,
    "createdAt": "2025-12-21T10:05:00Z",
    "lastDropAt": "2025-12-21T10:16:00Z",
    "dropCount": 3
  }
]
```

**Field Descriptions**:

| Field      | Type    | Description |
|-----------|---------|-------------|
| id        | Long    | Unique party identifier |
| world     | Integer | Game world number where party is active |
| avgDrop   | Integer | Average drop value in GP (calculated as total value / drop count) |
| isActive  | Boolean | Whether party is currently active (always true for this endpoint) |
| createdAt | String  | ISO 8601 timestamp when party was created |
| lastDropAt| String  | ISO 8601 timestamp of most recent drop |
| dropCount | Integer | Total number of drops in this party |

**Response Schema**: `src/main/java/rip/jade/partytimeserverjava/dto/DropPartyResponse.java:8`

---

### POST /api/party/cleanup

Manually trigger cleanup of inactive parties. Normally runs automatically every 2 minutes, but this endpoint allows on-demand cleanup.

**Controller**: `src/main/java/rip/jade/partytimeserverjava/Controller/PartyController.java:18`

**Endpoint**: `POST /api/party/cleanup`

#### Request

**Headers**: None required

**Body**: None

**Example**:
```bash
curl -X POST http://localhost:8000/api/party/cleanup
```

#### Response

**Success**: `200 OK`

**Body**: JSON object with cleanup status

```json
{
  "status": "success",
  "message": "Cleanup completed",
  "partiesCleaned": 3
}
```

---

## Error Responses

### Standard Error Format

All error responses follow Spring Boot's default error structure:

```json
{
  "timestamp": "2025-12-21T10:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for object='dropRequest'",
  "path": "/api/drops"
}
```

### HTTP Status Codes

| Code | Description | When It Occurs |
|------|-------------|----------------|
| 200  | OK | Successful request |
| 400  | Bad Request | Validation failure on input data |
| 404  | Not Found | Endpoint doesn't exist |
| 405  | Method Not Allowed | Wrong HTTP method (e.g., GET on POST-only endpoint) |
| 500  | Internal Server Error | Database error, unexpected exception |

---

## Data Transfer Objects

### DropRequest

**Source**: `src/main/java/rip/jade/partytimeserverjava/dto/DropRequest.java:8`

```json
{
  "world": 301,
  "itemId": 11834,
  "itemName": "Bandos Chestplate",
  "quantity": 1,
  "value": 30000000
}
```

### DropResponse

**Source**: `src/main/java/rip/jade/partytimeserverjava/dto/DropResponse.java:3`

```json
{
  "status": "drop_added",
  "world": 301,
  "dropPartyId": 42,
  "message": "Drop added to existing party on world 301"
}
```

### DropPartyResponse

**Source**: `src/main/java/rip/jade/partytimeserverjava/dto/DropPartyResponse.java:8`

```json
{
  "id": 1,
  "world": 301,
  "avgDrop": 40000000,
  "isActive": true,
  "createdAt": "2025-12-21T10:00:00Z",
  "lastDropAt": "2025-12-21T10:15:30Z",
  "dropCount": 5
}
```

---

## Security

**Current State**: No authentication or authorization

**Security Concerns**:
- All endpoints are publicly accessible
- No rate limiting
- No API key validation
- Anyone can submit drops or trigger cleanup

**Planned Security** (from TODO.md):
1. API keys for game clients on drop submission
2. JWT tokens for admin/web dashboard access
3. Role-based access control (RBAC)
4. Rate limiting per client/IP

---

## Testing the API

### Using curl

**Submit a drop**:
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

**Get active parties**:
```bash
curl http://localhost:8000/api/party
```

**Trigger cleanup**:
```bash
curl -X POST http://localhost:8000/api/party/cleanup
```
