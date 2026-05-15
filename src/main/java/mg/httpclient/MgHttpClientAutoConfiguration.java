package mg.httpclient;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * Auto-configuration for mg-http-client.
 *
 * <p>Activates when {@link RestTemplate} is on the classpath (spring-web).
 * The factory is always registered; it returns an {@link IllegalArgumentException}
 * at runtime if {@code get(name)} is called for a client that has no configuration
 * under {@code mg.clients}.
 */
@AutoConfiguration
@ConditionalOnClass(RestTemplate.class)
@EnableConfigurationProperties(MgHttpClientProperties.class)
public class MgHttpClientAutoConfiguration {

    @Bean
    public MgHttpClientFactory mgHttpClientFactory(MgHttpClientProperties properties,
                                                   ApplicationContext context) {
        return new MgHttpClientFactory(properties, context);
    }
}
