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
 * OAuth2 Resource Owner Password Credentials (ROPC) flow.
 *
 * <pre>
 * POST /oauth/token
 * Content-Type: application/x-www-form-urlencoded
 * grant_type=password&username=...&password=...&client_id=...&client_secret=...
 *
 * → {"access_token": "eyJ...", "token_type": "Bearer", ...}
 * </pre>
 *
 * <p>Use when a trusted client acts on behalf of a specific user.
 * {@code client_id} / {@code client_secret} are optional for public clients —
 * they are omitted from the form body if not set in config.
 */
public class OAuth2PasswordAuthStrategy implements AuthStrategy {

    @Override
    public String retrieveToken(ClientConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("username", config.getUsername());
        form.add("password", config.getPassword());
        if (config.getClientId() != null) {
            form.add("client_id", config.getClientId());
        }
        if (config.getClientSecret() != null) {
            form.add("client_secret", config.getClientSecret());
        }

        var response = new RestTemplate().exchange(
                config.getAuthUrl(),
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        Object value = Objects.requireNonNull(response.getBody()).get(config.getTokenField());
        return Objects.requireNonNull(value, "Token field '" + config.getTokenField() + "' not found in response").toString();
    }
}
