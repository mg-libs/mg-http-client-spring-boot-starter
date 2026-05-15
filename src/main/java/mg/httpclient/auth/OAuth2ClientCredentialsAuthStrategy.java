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
 * OAuth2 Client Credentials flow.
 *
 * <pre>
 * POST /oauth/token
 * Content-Type: application/x-www-form-urlencoded
 * grant_type=client_credentials&client_id=...&client_secret=...
 *
 * → {"access_token": "eyJ...", "token_type": "Bearer", ...}
 * </pre>
 *
 * <p>Typical use: machine-to-machine, Keycloak, Auth0, Azure AD.
 * Set {@code token-field: access_token} in your configuration.
 */
public class OAuth2ClientCredentialsAuthStrategy implements AuthStrategy {

    @Override
    public String retrieveToken(ClientConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", config.getClientId());
        form.add("client_secret", config.getClientSecret());

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
