package mg.httpclient.auth;

import mg.httpclient.MgHttpClientProperties.ClientConfig;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;

/**
 * Auth strategy: POST form-encoded credentials.
 *
 * <pre>
 * POST /token
 * Content-Type: application/x-www-form-urlencoded
 * username=...&password=...
 *
 * → {"access_token": "eyJ..."}
 * </pre>
 */
public class FormPostAuthStrategy implements AuthStrategy {

    @Override
    public String retrieveToken(ClientConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", config.getUsername());
        form.add("password", config.getPassword());

        var response = new RestTemplate().exchange(
                config.getAuthUrl(),
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                new ParameterizedTypeReference<Map<String, String>>() {}
        );

        return Objects.requireNonNull(response.getBody()).get(config.getTokenField());
    }
}
