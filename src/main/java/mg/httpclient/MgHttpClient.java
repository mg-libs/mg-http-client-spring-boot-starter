package mg.httpclient;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

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
 * // Usage
 * InvoiceDto invoice = billingClient.post("/v1/invoices", request, InvoiceDto.class);
 * MyDto      data    = billingClient.get("/v1/resource", MyDto.class);
 * List<Item> items   = billingClient.get("/v1/items", new ParameterizedTypeReference<>() {});
 * }</pre>
 */
public interface MgHttpClient {

    /**
     * GET request — simple response type.
     */
    <T> T get(String path, Class<T> responseType);

    /**
     * GET request — generic/collection response type.
     */
    <T> T get(String path, ParameterizedTypeReference<T> responseType);

    /**
     * POST request with a body.
     */
    <T> T post(String path, Object body, Class<T> responseType);

    /**
     * Full control: any method, custom headers, generic response type.
     */
    <T> ResponseEntity<T> exchange(String path, HttpMethod method,
                                   HttpEntity<?> requestEntity, Class<T> responseType);
}
