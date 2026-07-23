# Member Preferences Service

A lightweight REST API for managing member preferences built with Spring Boot, Gradle, and in-memory persistence.

## Quick Start

### Build
```bash
./gradlew build
```

### Run
```bash
./gradlew bootRun
```

The service runs on `http://localhost:8080`

### Test
```bash
./gradlew test
```

## API Endpoints

### Get Member Preferences
```bash
curl -X GET http://localhost:8080/v1/preferences/{memberId}
```

**Response (200):**
```json
{
  "memberId": "123",
  "emailNotifications": true,
  "smsNotifications": false,
  "language": "en",
  "timezone": "UTC",
  "marketingConsent": false,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

**Error (404):**
```json
{
  "error": "NOT_FOUND",
  "message": "Member preferences not found",
  "timestamp": "2024-01-01T10:00:00",
  "path": "/v1/preferences/123"
}
```

### Create or Update (Upsert) Preferences
```bash
curl -X PUT http://localhost:8080/v1/preferences/{memberId} \
  -H "Content-Type: application/json" \
  -d '{
    "emailNotifications": true,
    "smsNotifications": false,
    "language": "en",
    "timezone": "UTC",
    "marketingConsent": false
  }'
```

**Response (200):** Same as GET

### Partially Update (Patch) Preferences
```bash
curl -X PATCH http://localhost:8080/v1/preferences/{memberId} \
  -H "Content-Type: application/json" \
  -d '{
    "language": "es",
    "emailNotifications": false
  }'
```

**Response (200):** Updated preferences (only specified fields updated)

## Features

- **In-Memory Persistence**: Data stored in thread-safe ConcurrentHashMap
- **Structured Logging**: MDC with correlation IDs for request tracing
- **Metrics**: Micrometer metrics accessible via `/actuator/metrics`
- **Rate Limiting**: 10 requests per minute per member
- **Error Handling**: Consistent error payloads with clear error codes
- **Configuration**: Feature flags for patch operations and rate limiting
- **OpenAPI Documentation**: Interactive API docs at `/swagger-ui.html`

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
preferences:
  patch:
    enabled: true          # Enable PATCH operations
  rate-limit:
    enabled: true          # Enable rate limiting
    requests-per-minute: 60
    per-member-limit: 10   # Requests per member per minute
  request-timeout-ms: 5000 # Request timeout in milliseconds
```

## Actuator Endpoints

- Health: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- Configuration: `GET /actuator/configprops`

## Development

### Project Structure
```
src/
├── main/java/com/preferences/
│   ├── domain/
│   │   ├── dto/          # Data transfer objects
│   │   ├── model/        # Domain models
│   │   └── repository/   # Data access layer
│   ├── application/
│   │   └── service/      # Business logic
│   ├── presentation/
│   │   ├── controller/   # REST endpoints
│   │   └── exception/    # Exception handling
│   └── infrastructure/
│       ├── config/       # Configuration beans
│       └── filter/       # Filters and interceptors
├── resources/
│   └── application.yml   # Application properties
└── test/java/com/preferences/
    └── *IntegrationTest  # Integration tests
```

### Testing

Run the complete test suite:
```bash
./gradlew test
```

Key test scenarios:
- Happy path: Create, read, update preferences
- Validation: Invalid inputs rejected with 400
- Not Found: Missing members return 404
- Rate Limiting: Exceeded limits return 429

## Dependencies

- **Spring Boot 3.2**: Web, Validation, Actuator
- **springdoc-openapi**: OpenAPI UI at `/swagger-ui.html`
- **Micrometer**: Metrics collection (built-in with Actuator)
- **Jakarta Validation**: Input validation

## Error Codes

| Code | Status | Description |
|------|--------|-------------|
| NOT_FOUND | 404 | Member preferences not found |
| INVALID_INPUT | 400 | Validation failed |
| RATE_LIMIT_EXCEEDED | 429 | Too many requests |
| INTERNAL_SERVER_ERROR | 500 | Unexpected server error |

## Example Workflow

1. **Create preferences:**
   ```bash
   curl -X PUT http://localhost:8080/v1/preferences/user123 \
     -H "Content-Type: application/json" \
     -d '{"emailNotifications":true,"smsNotifications":false,"language":"en"}'
   ```

2. **Read preferences:**
   ```bash
   curl http://localhost:8080/v1/preferences/user123
   ```

3. **Update language:**
   ```bash
   curl -X PATCH http://localhost:8080/v1/preferences/user123 \
     -H "Content-Type: application/json" \
     -d '{"language":"fr"}'
   ```

4. **Check metrics:**
   ```bash
   curl http://localhost:8080/actuator/metrics
   ```

## Notes

- All data is stored in-memory and will be lost on service restart
- Timestamps are in ISO 8601 format (LocalDateTime)
- Rate limiting uses a sliding window per member ID
- Feature flags allow toggling behavior without code changes
