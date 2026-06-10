# mg-http-client-spring-boot-starter

Spring Boot Starter for authenticated HTTP clients with **automatic JWT token management**.

Configure any number of external APIs in `application.yml` and inject a ready-to-use client — no boilerplate needed.

---

## Features

- Automatic JWT token acquisition and refresh (configurable TTL)
- Five built-in auth strategies: JSON POST, Basic GET, Form POST, OAuth2 Client Credentials, OAuth2 Password
- Override the auth strategy per client via a Spring bean
- Thread-safe token cache (`volatile` + double-check `synchronized`)
- Extra token headers (e.g. `x-access-token`) propagated alongside `Authorization`
- IDE auto-completion for all configuration properties

---

## Installation

Build and install locally:

```bash
cd mg-http-client-spring-boot-starter
mvn install
```

Add to your project:

```xml
<dependency>
    <groupId>io.github.mg-libs</groupId>
    <artifactId>mg-http-client-spring-boot-starter</artifactId>
    <version>1.1.0</version>
</dependency>
```

> Requires Spring Boot 3.x and Java 21+.

---

## Quick start

### 1. Configure clients in `application.yml`

```yaml
mg:
  clients:
    billing:
      base-url: https://billing.example.com
      auth-url: https://billing.example.com/auth/login
      username: my-user
      password: ${BILLING_PASSWORD}

    keycloak-api:
      base-url: https://api.example.com
      auth-url: https://sso.example.com/realms/myrealm/protocol/openid-connect/token
      client-id: my-app
      client-secret: ${KC_SECRET}
      auth-method: OAUTH2_CLIENT_CREDENTIALS
      token-field: access_token
      token-expiration-delay: 300
```

### 2. Inject and use

```java
@Service
public class InvoiceService {

    private final MgHttpClient billingClient;

    public InvoiceService(MgHttpClientFactory clients) {
        this.billingClient = clients.get("billing");
    }

    public InvoiceDto create(CreateInvoiceRequest req) {
        return billingClient.post("/v1/invoices", req, InvoiceDto.class);
    }

    public List<InvoiceDto> list() {
        return billingClient.get("/v1/invoices", new ParameterizedTypeReference<>() {});
    }
}
```

---

## Configuration reference

| Property | Required | Default | Description |
|---|---|---|---|
| `base-url` | yes | — | Base URL of the target API |
| `auth-url` | yes | — | Authentication endpoint (full URL) |
| `username` | no* | — | User login — required for JSON_POST, BASIC_GET, FORM_POST, OAUTH2_PASSWORD |
| `password` | no* | — | Password — same as above (use env var) |
| `client-id` | no* | — | OAuth2 client id — required for OAUTH2_CLIENT_CREDENTIALS, optional for OAUTH2_PASSWORD |
| `client-secret` | no* | — | OAuth2 client secret — same as above (use env var) |
| `auth-method` | no | `JSON_POST` | Auth strategy (see below) |
| `token-field` | no | `token` | JSON field name containing the JWT in the auth response |
| `token-expiration-delay` | no | `60` | Token TTL in seconds |
| `timeout` | no | `5000` | Connect and read timeout in milliseconds |
| `additional-token-headers` | no | `[]` | Headers populated with the token value on every request |
| `request-headers` | no | `{}` | Static headers added to every request (`key: value` map) |

### Auth methods

| Value | Credentials used | Typical use case |
|---|---|---|
| `JSON_POST` | `username` + `password` | Custom APIs, proprietary auth *(default)* |
| `BASIC_GET` | `username` + `password` | Lightweight auth with Basic header |
| `FORM_POST` | `username` + `password` | Legacy form-based auth |
| `OAUTH2_CLIENT_CREDENTIALS` | `client-id` + `client-secret` | Machine-to-machine (Keycloak, Auth0, Azure AD) |
| `OAUTH2_PASSWORD` | `username` + `password` + optional `client-id`/`client-secret` | Trusted client acting on behalf of a user |

---

## Available client methods

```java
// Simple GET
MyDto dto = client.get("/v1/resource", MyDto.class);

// GET with query parameters (URL-encoded automatically)
MyDto dto = client.get("/v1/resource", Map.of("id", 42, "lang", "fr"), MyDto.class);

// GET with query parameters + generic/collection type
List<MyDto> items = client.get("/v1/items", Map.of("page", 1, "size", 20),
                               new ParameterizedTypeReference<>() {});

// GET with generic/collection type (no params)
List<MyDto> items = client.get("/v1/items", new ParameterizedTypeReference<>() {});

// POST with body
ResultDto result = client.post("/v1/items", body, ResultDto.class);

// Full control: any method, custom headers
// → utilisez exchange pour GET avec body, réponses binaires, headers personnalisés
ResponseEntity<byte[]> resp = client.exchange(
    "/v1/file",
    HttpMethod.GET,
    new HttpEntity<>(headers),
    byte[].class
);

// GET avec body (non standard mais certaines APIs l'exigent)
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);
ResponseEntity<MyDto> resp = client.exchange(
    "/v1/search",
    HttpMethod.GET,
    new HttpEntity<>(searchBody, headers),
    MyDto.class
);
```

---

## Advanced usage

### Static request headers

Add fixed headers to every request — useful for API versioning, tenant routing, or any header the API requires:

```yaml
mg:
  clients:
    my-api:
      base-url: https://api.example.com
      auth-url: https://api.example.com/login
      username: user
      password: ${PASSWORD}
      request-headers:
        x-api-version: "2"
        x-tenant-id: acme
        accept-language: fr
```

### Additional token headers

Some APIs require the JWT in multiple headers simultaneously:

```yaml
mg:
  clients:
    legacy-api:
      base-url: https://legacy.example.com
      auth-url: https://legacy.example.com/login
      username: user
      password: ${LEGACY_PASSWORD}
      additional-token-headers:
        - x-access-token
        - x-auth-token
```

Every request will carry:
```
Authorization: Bearer eyJ...
x-access-token: eyJ...
x-auth-token: eyJ...
```

### Custom auth strategy

If none of the three built-in strategies fits your API, declare a Spring bean named `<clientName>MgAuthStrategy`:

```java
@Configuration
public class MyApiAuthConfig {

    @Bean("myApiMgAuthStrategy")
    public AuthStrategy myApiAuth(MyApiKeyProvider keyProvider) {
        // config contains base-url, auth-url, username, password, etc.
        return config -> keyProvider.fetchToken(config.getAuthUrl());
    }
}
```

The custom bean takes full priority over the `auth-method` setting. The `AuthStrategy` interface is a `@FunctionalInterface` — lambdas work directly.

### Multiple clients of the same type

Each entry under `mg.clients` is independent. You can configure multiple clients hitting the same API with different credentials:

```yaml
mg:
  clients:
    reporting-fr:
      base-url: https://reports.example.com
      auth-url: https://reports.example.com/login
      username: ${REPORTING_FR_USER}
      password: ${REPORTING_FR_PASSWORD}

    reporting-en:
      base-url: https://reports.example.com
      auth-url: https://reports.example.com/login
      username: ${REPORTING_EN_USER}
      password: ${REPORTING_EN_PASSWORD}
```

---

## How it works

```
MgHttpClientFactory.get("billing")
        │
        ▼
MgHttpClientImpl (wraps a private RestTemplate)
        │
        │  on every request:
        ▼
TokenManager.getToken()
        │
        ├── token valid? ──► return cached token
        │
        └── expired? ──► AuthStrategy.retrieveToken(config)
                                │
                                ├── JsonPostAuthStrategy  (default)
                                ├── BasicGetAuthStrategy
                                ├── FormPostAuthStrategy
                                ├── OAuth2ClientCredentialsAuthStrategy
                                ├── OAuth2PasswordAuthStrategy
                                └── (or your custom bean)
```

Token refresh is **thread-safe**: `volatile` ensures cross-thread visibility, `synchronized` with double-check locking prevents concurrent refresh calls.

---

## Running the tests

```bash
mvn test
```

Tests use `ApplicationContextRunner` (lightweight, no server startup) and `MockWebServer` (OkHttp) to simulate auth and API servers. No external services required.
