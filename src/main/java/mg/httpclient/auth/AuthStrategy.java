package mg.httpclient.auth;

import mg.httpclient.MgHttpClientProperties.ClientConfig;

/**
 * Strategy for retrieving a JWT token from an authentication endpoint.
 *
 * <p>Three implementations are provided out of the box:
 * <ul>
 *   <li>{@link JsonPostAuthStrategy} — POST JSON body (default)</li>
 *   <li>{@link BasicGetAuthStrategy} — GET with Basic auth header</li>
 *   <li>{@link FormPostAuthStrategy} — POST form-encoded credentials</li>
 * </ul>
 *
 * <p>To override for a specific client, declare a Spring bean named
 * {@code <clientName>MgAuthStrategy}:
 * <pre>{@code
 * @Bean("billingMgAuthStrategy")
 * public AuthStrategy customBillingAuth() {
 *     return config -> { /* custom logic *\/ };
 * }
 * }</pre>
 */
@FunctionalInterface
public interface AuthStrategy {
    String retrieveToken(ClientConfig config);
}
