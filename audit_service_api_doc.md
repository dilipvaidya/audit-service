### 🔐 Audit Service API

This service provides a RESTful interface for reading and writing audit logs generated across the system. It supports secure, paginated, and filtered access to logs.

#### 📥 POST `/api/audit/logs`

Ingest a new audit log entry (used by services (mostly internal) that don’t push events via brokers).

- **Request Body:**
```json
{
  "eventId": "uuid",
  "timestamp": "ISO 8601",
  "sourceService": "user-service",
  "eventType": "USER_UPDATED",
  "entityType": "User",
  "entityId": "12345",
  "changedBy": {
    "username": "john.doe",
    "userId": "user-001",
    "roles": ["ADMIN"]
  },
  "changeSummary": {
    "email": {
      "old": "a@example.com",
      "new": "b@example.com"
    }
  },
  "metadata": {
    "ipAddress": "192.168.1.10",
    "userAgent": "Mozilla/5.0"
  }
}
```

- **Response:**
    - ```201 Created```

#### 📥 GET `/api/audit/logs`
Retrieves audit logs with filters as per the access controls. Admin users can access all the logs while non-admin users
can only access the logs entityId and changeBy.userId is accessible to user.

- **Query Parameters:**
    - `startTime (ISO 8601)`
    - `endTime (ISO 8601)`
    - `entityType (string)`
    - `entityId (string)`
    - `eventType (string)`
    - `sourceService (string)`
    - `changedByUserId (string)`
    - `page`, `size`, `sort`
- **Access Controls:**
    - admin user: all audit logs
    - non-admin user: only logs where `entityId` and `changeBy.username` is accessible to the user.
- **Response:**
    - ```200 OK```
    - JSON array of audit messages matching filter criteria.
- **Example:**
    - ```GET /api/audit/logs?entityType=User&entityId=123&page=0&size=10```

#### 📥 GET `/api/audit/logs/{eventId}`
Retrieve a single audit log by `eventId`.

- **Request Body:**
- **Path Variable:**
    - `eventId`: UUID of the audit message.
- **Access Controls:**
    - admin user: all audit logs
    - non-admin user: only logs where `entityId` and `changeBy.username` is accessible to the user.
- **Response:**
    - ```200 OK```
    - Single audit message matching filter criteria.
- **Example:**
    - ```GET /api/audit/logs/6f8e67ad-8c47-4299-b054-7c87173babc5```


#### 🔍 POST `/api/audit/query`
Advanced search for audit logs using JSON body for complex filters.

- **Request Body:**
```json
{
  "entityType": "Order",
  "eventType": "ORDER_UPDATED",
  "timeRange": {
    "from": "2025-04-01T00:00:00Z",
    "to": "2025-04-30T23:59:59Z"
  },
  "changedBy": {
    "userId": "user-002"
  }
}
```
- **Access Controls:**
    - admin user: all audit logs
    - non-admin user: only logs where `entityId` and `changeBy.username` is accessible to the user.
- **Response:**
    - ```200 OK```
    - Paginated list of audit logs matching criteria
- **Example:**
    - ```GET /api/audit/logs/6f8e67ad-8c47-4299-b054-7c87173babc5```
