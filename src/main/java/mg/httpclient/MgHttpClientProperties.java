package mg.httpclient;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for mg-http-client.
 *
 * <pre>{@code
 * mg:
 *   clients:
 *     billing:
 *       base-url: https://billing.example.com
 *       auth-url: https://billing.example.com/auth/login
 *       username: USER
 *       password: ${BILLING_PASSWORD}
 *       auth-method: JSON_POST           # optional, default: JSON_POST
 *       token-field: token               # optional, default: token
 *       token-expiration-delay: 60       # optional, default: 60s
 *       timeout: 5000                    # optional, default: 5000ms
 *       additional-token-headers:        # optional: extra headers carrying the token value
 *         - x-access-token
 *       request-headers:                 # optional: static headers added to every request
 *         x-api-version: "2"
 *         x-tenant-id: my-tenant
 *
 *     keycloak-api:
 *       base-url: https://api.example.com
 *       auth-url: https://sso.example.com/realms/myrealm/protocol/openid-connect/token
 *       client-id: my-app
 *       client-secret: ${KC_SECRET}
 *       auth-method: OAUTH2_CLIENT_CREDENTIALS
 *       token-field: access_token
 * }</pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "mg")
public class MgHttpClientProperties {

    private Map<String, ClientConfig> clients = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class ClientConfig {

        /** Base URL of the target API. */
        private String baseUrl;

        /** Authentication endpoint (full URL). */
        private String authUrl;

        /** Username / resource-owner login. Used by JSON_POST, BASIC_GET, FORM_POST, OAUTH2_PASSWORD. */
        private String username;

        /** Password. Used by JSON_POST, BASIC_GET, FORM_POST, OAUTH2_PASSWORD. */
        private String password;

        /** OAuth2 client identifier. Used by OAUTH2_CLIENT_CREDENTIALS and OAUTH2_PASSWORD. */
        private String clientId;

        /** OAuth2 client secret. Used by OAUTH2_CLIENT_CREDENTIALS and OAUTH2_PASSWORD. */
        private String clientSecret;

        /** Field name containing the JWT in the auth response. Default: {@code token}. */
        private String tokenField = "token";

        /** Authentication mechanism to use. Default: {@link AuthMethod#JSON_POST}. */
        private AuthMethod authMethod = AuthMethod.JSON_POST;

        /** Token validity in seconds. Default: 60. */
        private int tokenExpirationDelay = 60;

        /** Connect and read timeout in milliseconds. Default: 5000. */
        private int timeout = 5000;

        /**
         * Headers whose value is set to the token on every request
         * (e.g. {@code x-access-token} for APIs that require the token in multiple headers).
         */
        private List<String> additionalTokenHeaders = new ArrayList<>();

        /**
         * Arbitrary static headers added to every request (key → value).
         * Useful for API versioning, tenant routing, or any fixed header the API requires.
         * <pre>{@code
         * request-headers:
         *   x-api-version: "2"
         *   x-tenant-id: my-tenant
         * }</pre>
         */
        private Map<String, String> requestHeaders = new LinkedHashMap<>();
    }

    public enum AuthMethod {
        /** POST {@code {"username": "...", "password": "..."}} as JSON → response contains {@code tokenField}. */
        JSON_POST,

        /** GET with {@code Authorization: Basic base64(username:password)} → response contains {@code tokenField}. */
        BASIC_GET,

        /** POST {@code username=...&password=...} as form data → response contains {@code tokenField}. */
        FORM_POST,

        /**
         * OAuth2 Client Credentials flow.
         * POST {@code grant_type=client_credentials&client_id=...&client_secret=...} → response contains {@code tokenField}.
         */
        OAUTH2_CLIENT_CREDENTIALS,

        /**
         * OAuth2 Resource Owner Password Credentials flow.
         * POST {@code grant_type=password&username=...&password=...&client_id=...&client_secret=...}
         * → response contains {@code tokenField}.
         */
        OAUTH2_PASSWORD
    }
}
