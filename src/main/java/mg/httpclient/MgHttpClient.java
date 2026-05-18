package mg.httpclient;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

/**
 * Authenticated HTTP client for a configured API.
 *
 * <p>JWT token acquisition and refresh are handled transparently.
 * Obtain instances via {@link MgHttpClientFactory}:
 *
 * <pre>{@code
 * private final MgHttpClient billingClient;
 *
 * public MyService(MgHttpClientFactory clients) {
 *     this.billingClient = clients.get("billing");
 * }
 *
 * // Simple GET
 * MyDto data = billingClient.get("/v1/resource", MyDto.class);
 *
 * // GET with query parameters
 * MyDto data = billingClient.get("/v1/resource", Map.of("page", 1, "size", 20), MyDto.class);
 *
 * // GET with generic/collection type
 * List<Item> items = billingClient.get("/v1/items", new ParameterizedTypeReference<>() {});
 *
 * // GET with query parameters + generic type
 * List<Item> items = billingClient.get("/v1/items", Map.of("status", "active"),
 *                                      new ParameterizedTypeReference<>() {});
 *
 * // POST with body
 * InvoiceDto invoice = billingClient.post("/v1/invoices", request, InvoiceDto.class);
 *
 * // GET with body (non-standard) or any custom need — use exchange
 * ResponseEntity<byte[]> resp = billingClient.exchange(
 *     "/v1/file", HttpMethod.GET, new HttpEntity<>(body, headers), byte[].class);
 * }</pre>
 */
public interface MgHttpClient {

    /**
     * GET request — simple response type.
     */
    <T> T get(String path, Class<T> responseType);

    /**
     * GET request with query parameters — simple response type.
     *
     * <p>Parameters are URL-encoded automatically.
     * Example: {@code Map.of("page", 1, "status", "active")}
     * produces {@code ?page=1&status=active}.
     */
    <T> T get(String path, Map<String, ?> params, Class<T> responseType);

    /**
     * GET request — generic/collection response type.
     */
    <T> T get(String path, ParameterizedTypeReference<T> responseType);

    /**
     * GET request with query parameters — generic/collection response type.
     */
    <T> T get(String path, Map<String, ?> params, ParameterizedTypeReference<T> responseType);

    /**
     * POST request with a body.
     */
    <T> T post(String path, Object body, Class<T> responseType);

    /**
     * Full control: any method, custom headers, generic response type.
     *
     * <p>Use this for non-standard needs such as:
     * <ul>
     *   <li>GET with a request body</li>
     *   <li>Custom {@code Accept} or {@code Content-Type} headers</li>
     *   <li>Binary responses ({@code byte[]})</li>
     * </ul>
     */
    <T> ResponseEntity<T> exchange(String path, HttpMethod method,
                                   HttpEntity<?> requestEntity, Class<T> responseType);
}
