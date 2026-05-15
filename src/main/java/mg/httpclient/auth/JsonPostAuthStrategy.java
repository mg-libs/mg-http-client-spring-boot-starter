package mg.httpclient.auth;

import mg.httpclient.MgHttpClientProperties.ClientConfig;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;

/**
 * Default auth strategy: POST {@code {username, password}} as JSON.
 *
 * <pre>
 * POST /auth/login
 * Content-Type: application/json
 * {"username": "...", "password": "..."}
 *
 * → {"token": "eyJ..."}
 * </pre>
 */
public class JsonPostAuthStrategy implements AuthStrategy {

    @Override
    public String retrieveToken(ClientConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of(
                "username", config.getUsername(),
                "password", config.getPassword()
        );

        var response = new RestTemplate().exchange(
                config.getAuthUrl(),
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                new ParameterizedTypeReference<Map<String, String>>() {}
        );

        return Objects.requireNonNull(response.getBody()).get(config.getTokenField());
    }
}
