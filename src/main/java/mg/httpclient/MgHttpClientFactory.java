package mg.httpclient;

import mg.httpclient.auth.ApiKeyAuthStrategy;
import mg.httpclient.auth.AuthStrategy;
import mg.httpclient.auth.BasicGetAuthStrategy;
import mg.httpclient.auth.FormPostAuthStrategy;
import mg.httpclient.auth.JsonPostAuthStrategy;
import mg.httpclient.auth.OAuth2ClientCredentialsAuthStrategy;
import mg.httpclient.auth.OAuth2PasswordAuthStrategy;
import mg.httpclient.token.TokenManager;
import org.springframework.context.ApplicationContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory and registry for {@link MgHttpClient} instances.
 *
 * <p>One client per configured entry under {@code mg.clients.*}.
 * Clients are created lazily and cached.
 *
 * <p>Inject in any Spring component:
 * <pre>{@code
 * @Autowired
 * private MgHttpClientFactory clients;
 *
 * MgHttpClient billingClient = clients.get("billing");
 * InvoiceDto invoice = billingClient.post("/v1/invoices", body, InvoiceDto.class);
 * }</pre>
 *
 * <p><b>Custom auth strategy:</b> declare a bean named {@code <clientName>MgAuthStrategy}:
 * <pre>{@code
 * @Bean("billingMgAuthStrategy")
 * public AuthStrategy customBillingAuth() {
 *     return config -> { /* your logic *\/ };
 * }
 * }</pre>
 */
public class MgHttpClientFactory {

    private final MgHttpClientProperties properties;
    private final ApplicationContext context;
    private final Map<String, MgHttpClient> cache = new ConcurrentHashMap<>();

    MgHttpClientFactory(MgHttpClientProperties properties, ApplicationContext context) {
        this.properties = properties;
        this.context = context;
    }

    /**
     * Returns the client registered under {@code name}.
     *
     * @throws IllegalArgumentException if no client is configured for that name
     */
    public MgHttpClient get(String name) {
        return cache.computeIfAbsent(name, this::create);
    }

    private MgHttpClient create(String name) {
        MgHttpClientProperties.ClientConfig config = properties.getClients().get(name);
        if (config == null) {
            throw new IllegalArgumentException(
                    "No mg.clients." + name + " configuration found");
        }
        AuthStrategy strategy = resolveStrategy(name, config);
        TokenManager tokenManager = new TokenManager(name, config, strategy);
        return new MgHttpClientImpl(config, tokenManager);
    }

    /**
     * Resolves the auth strategy for a client.
     * Checks for a custom bean named {@code <name>MgAuthStrategy} first,
     * then falls back to the default based on {@code auth-method}.
     */
    private AuthStrategy resolveStrategy(String name,
                                         MgHttpClientProperties.ClientConfig config) {
        String beanName = name + "MgAuthStrategy";
        if (context.containsBean(beanName)) {
            return context.getBean(beanName, AuthStrategy.class);
        }
        return switch (config.getAuthMethod()) {
            case JSON_POST                  -> new JsonPostAuthStrategy();
            case BASIC_GET                  -> new BasicGetAuthStrategy();
            case FORM_POST                  -> new FormPostAuthStrategy();
            case OAUTH2_CLIENT_CREDENTIALS  -> new OAuth2ClientCredentialsAuthStrategy();
            case OAUTH2_PASSWORD            -> new OAuth2PasswordAuthStrategy();
            case API_KEY                    -> new ApiKeyAuthStrategy();
            case BEARER_API_KEY             -> new ApiKeyAuthStrategy();
        };
    }
}
