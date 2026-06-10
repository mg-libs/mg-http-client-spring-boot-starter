package mg.httpclient.auth;

import mg.httpclient.MgHttpClientProperties.ClientConfig;

/**
 * {@link AuthStrategy} for static API key authentication.
 *
 * <p>No HTTP call is made — the configured {@code api-key} value is returned directly.
 * The key is transmitted on every request via the header defined by {@code api-key-header}
 * (default: {@code X-API-Key}), <b>not</b> as a Bearer token.
 *
 * <pre>{@code
 * mg:
 *   clients:
 *     maps-api:
 *       base-url: https://maps.example.com
 *       api-key: ${MAPS_API_KEY}
 *       auth-method: API_KEY
 *       api-key-header: X-Goog-Api-Key  # optional, default: X-API-Key
 * }</pre>
 */
public class ApiKeyAuthStrategy implements AuthStrategy {

    @Override
    public String retrieveToken(ClientConfig config) {
        String key = config.getApiKey();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "API_KEY auth method requires 'api-key' to be configured");
        }
        return key;
    }
}
