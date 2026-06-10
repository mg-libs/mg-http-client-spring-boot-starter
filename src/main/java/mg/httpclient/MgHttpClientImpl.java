package mg.httpclient;

import mg.httpclient.MgHttpClientProperties.AuthMethod;
import mg.httpclient.token.TokenManager;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Default {@link MgHttpClient} implementation.
 *
 * <p>Uses composition: wraps a private {@link RestTemplate} configured with:
 * <ul>
 *   <li>Connect and read timeouts</li>
 *   <li>An interceptor that injects the JWT token on every request</li>
 *   <li>Buffered request factory for response body re-readability</li>
 * </ul>
 */
class MgHttpClientImpl implements MgHttpClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    MgHttpClientImpl(MgHttpClientProperties.ClientConfig config, TokenManager tokenManager) {
        this.baseUrl = config.getBaseUrl();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getTimeout());
        factory.setReadTimeout(config.getTimeout());

        this.restTemplate = new RestTemplate(new BufferingClientHttpRequestFactory(factory));
        this.restTemplate.getInterceptors().add((request, body, execution) -> {
            String credential = tokenManager.getToken();
            if (config.getAuthMethod() == AuthMethod.API_KEY) {
                request.getHeaders().set(config.getApiKeyHeader(), credential);
            } else {
                request.getHeaders().setBearerAuth(credential);
                config.getAdditionalTokenHeaders()
                      .forEach(header -> request.getHeaders().add(header, credential));
            }
            config.getRequestHeaders()
                  .forEach((key, value) -> request.getHeaders().add(key, value));
            return execution.execute(request, body);
        });
    }

    @Override
    public <T> T get(String path, Class<T> responseType) {
        return restTemplate.getForObject(url(path), responseType);
    }

    @Override
    public <T> T get(String path, Map<String, ?> params, Class<T> responseType) {
        return restTemplate.getForObject(url(path, params), responseType);
    }

    @Override
    public <T> T get(String path, ParameterizedTypeReference<T> responseType) {
        return restTemplate.exchange(url(path), HttpMethod.GET, null, responseType).getBody();
    }

    @Override
    public <T> T get(String path, Map<String, ?> params, ParameterizedTypeReference<T> responseType) {
        return restTemplate.exchange(url(path, params), HttpMethod.GET, null, responseType).getBody();
    }

    @Override
    public <T> T post(String path, Object body, Class<T> responseType) {
        return restTemplate.postForObject(url(path), body, responseType);
    }

    @Override
    public <T> ResponseEntity<T> exchange(String path, HttpMethod method,
                                          HttpEntity<?> requestEntity, Class<T> responseType) {
        return restTemplate.exchange(url(path), method, requestEntity, responseType);
    }

    private String url(String path) {
        return UriComponentsBuilder.fromUriString(baseUrl).path(path).toUriString();
    }

    private String url(String path, Map<String, ?> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl).path(path);
        params.forEach(builder::queryParam);
        return builder.build().toUriString();
    }
}
