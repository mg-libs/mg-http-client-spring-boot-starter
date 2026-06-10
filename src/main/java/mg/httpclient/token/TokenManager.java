package mg.httpclient.token;

import mg.httpclient.MgHttpClientProperties.ClientConfig;
import mg.httpclient.auth.AuthStrategy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.time.Instant;

/**
 * Manages JWT token lifecycle for a single API client.
 *
 * <p>Token is fetched on first use and refreshed automatically before expiration.
 * Thread-safe: {@code volatile} ensures cross-thread visibility,
 * {@code synchronized} prevents concurrent refresh calls.
 */
public class TokenManager {

    private static final Log log = LogFactory.getLog(TokenManager.class);

    private final ClientConfig config;
    private final AuthStrategy authStrategy;
    private final String clientName;

    private volatile String token;
    private Instant expirationDate;

    public TokenManager(String clientName, ClientConfig config, AuthStrategy authStrategy) {
        this.clientName = clientName;
        this.config = config;
        this.authStrategy = authStrategy;
    }

    public String getToken() {
        if (isExpired()) {
            refresh();
        }
        return token;
    }

    private boolean isExpired() {
        return token == null || expirationDate == null || Instant.now().isAfter(expirationDate);
    }

    private synchronized void refresh() {
        // Double-check: another thread may have refreshed while we were waiting
        if (!isExpired()) return;

        log.info("[mg-http-client] Refreshing auth credential for client '" + clientName + "'");
        token = authStrategy.retrieveToken(config);
        expirationDate = Instant.now().plusSeconds(config.getTokenExpirationDelay());
    }
}
