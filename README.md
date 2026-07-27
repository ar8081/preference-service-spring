# Member Preferences Service

A production-ready REST API service for managing member preferences built with Java 17, Spring Boot 3.2, Gradle, and in-memory persistence.

---

## 🚀 Quick Start

### Build
```bash
./gradlew build
```

### Run
```bash
./gradlew bootRun
```
The service starts on `http://localhost:8080`

### Test
```bash
./gradlew test
```

---

## 🔑 Authentication

All API endpoints (except Actuator `/actuator/**` and Swagger UI `/swagger-ui.html`) require an **API Key**:

```
X-API-Key: secret-api-key-12345
```

---

## 📡 API Endpoints

### 1. Get Member Preferences (GET)
```bash
curl -i http://localhost:8080/v1/preferences/123 \
  -H "X-API-Key: secret-api-key-12345"
```

**Conditional GET (ETag Validation - 304 Not Modified):**
```bash
curl -i http://localhost:8080/v1/preferences/123 \
  -H "X-API-Key: secret-api-key-12345" \
  -H 'If-None-Match: "a3f2d7c1b4e9"'
```
Returns `304 Not Modified` with no response body if data is unchanged.

---

### 2. Create or Update Preferences (PUT)
```bash
curl -i -X PUT http://localhost:8080/v1/preferences/123 \
  -H "X-API-Key: secret-api-key-12345" \
  -H "Content-Type: application/json" \
  -d '{
    "emailNotifications": true,
    "smsNotifications": false,
    "language": "en",
    "timezone": "UTC",
    "marketingConsent": false
  }'
```

**Write Protection / Optimistic Locking (If-Match):**
```bash
curl -i -X PUT http://localhost:8080/v1/preferences/123 \
  -H "X-API-Key: secret-api-key-12345" \
  -H "Content-Type: application/json" \
  -H 'If-Match: "a3f2d7c1b4e9"' \
  -d '{ ... }'
```
Returns `412 Precondition Failed` if the current server ETag does not match `If-Match`.

---

### 3. Partially Update Preferences (PATCH)
```bash
curl -i -X PATCH http://localhost:8080/v1/preferences/123 \
  -H "X-API-Key: secret-api-key-12345" \
  -H "Content-Type: application/json" \
  -d '{
    "language": "fr",
    "emailNotifications": false
  }'
```

---

### 4. Internationalization (i18n Error Messages)
Pass `Accept-Language` header to get localized error messages:

```bash
# French Error Response
curl -i http://localhost:8080/v1/preferences/nonexistent \
  -H "X-API-Key: secret-api-key-12345" \
  -H "Accept-Language: fr"
```

**Response (404 Not Found in French):**
```json
{
  "error": "NOT_FOUND",
  "message": "Préférences du membre introuvables",
  "timestamp": "2026-07-27T14:20:00",
  "path": "/v1/preferences/nonexistent"
}
```

---

## ✨ Features Implemented (Tasks 1–15 Complete)

- **In-Memory Persistence**: Thread-safe data storage using `ConcurrentHashMap`.
- **Security Hardening**: `ApiKeyFilter` enforcing `X-API-Key` authentication & max body payload limits.
- **Conditional Requests (ETags)**: Strong MD5 ETags supporting `If-None-Match` (304) and `If-Match` (412).
- **Caffeine Response Caching**: In-memory caching for `GET` with TTL (5 min) & invalidation on `PUT`/`PATCH`.
- **i18n Error Localization**: Translated error bundles (`messages.properties`, `messages_fr.properties`) honoring `Accept-Language`.
- **Rate Limiting**: Sliding window token bucket (10 requests per minute per member).
- **Structured Logging & Observability**: SLF4J MDC correlation IDs + Micrometer metrics at `/actuator/metrics`.
- **Input Validation**: Jakarta Bean Validation (`@NotNull`, `@Size`, `@Pattern`) for data hygiene and injection defense.
- **Contract & Property-Based Testing**: OpenAPI 3.0 schema conformance, boundary value testing, and status code verification.

---

## ⚙️ Configuration Reference

Edit `src/main/resources/application.yml`:

```yaml
preferences:
  patch:
    enabled: true
  rate-limit:
    enabled: true
    requests-per-minute: 60
    per-member-limit: 10
  request-timeout-ms: 5000
  cache:
    ttl-seconds: 300       # Caffeine cache TTL
    max-size: 1000         # Maximum cached members

security:
  api-key: secret-api-key-12345
  max-body-size-bytes: 2048
```

---

## 📊 Actuator Endpoints

- **Health**: `GET /actuator/health`
- **Metrics**: `GET /actuator/metrics`
- **Cache Hits**: `GET /actuator/metrics/cache.gets?tag=name:preferences&tag=result:hit`
- **Cache Misses**: `GET /actuator/metrics/cache.gets?tag=name:preferences&tag=result:miss`
- **OpenAPI / Swagger**: `GET /swagger-ui.html`

---

## 🧪 Testing

Run all 70+ integration and OpenAPI contract tests:
```bash
./gradlew test
```

Key test suites:
- `PreferencesControllerIntegrationTest`: End-to-end endpoint tests, security, caching, ETag, rate limiting, and i18n.
- `OpenApiContractTest`: Property-based and contract tests against `openapi.yaml`.

---

## ⚠️ Error Codes Table

| Code | Status | Description |
|---|---|---|
| `UNAUTHORIZED` | 401 | Missing `X-API-Key` header |
| `FORBIDDEN` | 403 | Invalid `X-API-Key` header |
| `INVALID_INPUT` | 400 | Validation failed or malformed JSON |
| `PAYLOAD_TOO_LARGE` | 400 | Body exceeds 2048 bytes |
| `NOT_FOUND` | 404 | Member preferences not found |
| `PRECONDITION_FAILED` | 412 | `If-Match` ETag header mismatch |
| `RATE_LIMIT_EXCEEDED` | 429 | Exceeded 10 req/min limit |
| `INTERNAL_SERVER_ERROR` | 500 | Unexpected server error |
