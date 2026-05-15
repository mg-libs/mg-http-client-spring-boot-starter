package mg.httpclient.auth;

import mg.httpclient.MgHttpClientProperties.ClientConfig;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;

/**
 * Auth strategy: GET with Basic auth header.
 *
 * <pre>
 * GET /v1/login
 * Authorization: Basic base64(username:password)
 *
 * → {"token": "eyJ..."}
 * </pre>
 */
public class BasicGetAuthStrategy implements AuthStrategy {

    @Override
    public String retrieveToken(ClientConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(config.getUsername(), config.getPassword());

        var response = new RestTemplate().exchange(
                config.getAuthUrl(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<Map<String, String>>() {}
        );

        return Objects.requireNonNull(response.getBody()).get(config.getTokenField());
    }
}
