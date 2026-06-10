package mg.httpclient;

import mg.httpclient.auth.AuthStrategy;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MgHttpClientAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MgHttpClientAutoConfiguration.class));

    private MockWebServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = new MockWebServer();
        server.start();
        baseUrl = server.url("/").toString();
    }

    @AfterEach
    void stopServer() throws IOException {
        server.shutdown();
    }

    // -------------------------------------------------------------------------
    // Auto-configuration presence
    // -------------------------------------------------------------------------

    @Test
    void factoryBeanCreated_whenClientsConfigured() {
        runner.withPropertyValues(
                        "mg.clients.api.base-url=" + baseUrl,
                        "mg.clients.api.auth-url=" + baseUrl + "auth/login",
                        "mg.clients.api.username=user",
                        "mg.clients.api.password=pass"
                )
                .run(ctx -> assertThat(ctx).hasSingleBean(MgHttpClientFactory.class));
    }

    @Test
    void factoryBeanPresent_evenWithNoClientsConfigured() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(MgHttpClientFactory.class));
    }

    // -------------------------------------------------------------------------
    // JSON_POST strategy (default)
    // -------------------------------------------------------------------------

    @Test
    void jsonPost_sendsCredentialsAsJson_andReturnsToken() throws Exception {
        server.enqueue(tokenResponse("{\"token\":\"jwt-json\"}"));
        server.enqueue(apiResponse("{\"value\":\"ok\"}"));

        runner.withPropertyValues(
                        "mg.clients.svc.base-url=" + baseUrl,
                        "mg.clients.svc.auth-url=" + baseUrl + "auth/login",
                        "mg.clients.svc.username=user",
                        "mg.clients.svc.password=secret"
                )
                .run(ctx -> {
                    MgHttpClient client = ctx.getBean(MgHttpClientFactory.class).get("svc");
                    client.get("/data", String.class);

                    RecordedRequest authReq = server.takeRequest();
                    assertThat(authReq.getPath()).isEqualTo("/auth/login");
                    assertThat(authReq.getHeader(HttpHeaders.CONTENT_TYPE))
                            .startsWith(MediaType.APPLICATION_JSON_VALUE);
                    assertThat(authReq.getBody().readUtf8()).contains("\"username\"", "\"password\"");

                    RecordedRequest apiReq = server.takeRequest();
                    assertThat(apiReq.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer jwt-json");
                });
    }

    // -------------------------------------------------------------------------
    // BASIC_GET strategy
    // -------------------------------------------------------------------------

    @Test
    void basicGet_sendsBasicAuthHeader() throws Exception {
        server.enqueue(tokenResponse("{\"token\":\"jwt-basic\"}"));
        server.enqueue(apiResponse("{}"));

        runner.withPropertyValues(
                        "mg.clients.svc.base-url=" + baseUrl,
                        "mg.clients.svc.auth-url=" + baseUrl + "auth/token",
                        "mg.clients.svc.username=user",
                        "mg.clients.svc.password=pass",
                        "mg.clients.svc.auth-method=BASIC_GET"
                )
                .run(ctx -> {
                    ctx.getBean(MgHttpClientFactory.class).get("svc").get("/ping", String.class);

                    RecordedRequest authReq = server.takeRequest();
                    assertThat(authReq.getMethod()).isEqualTo("GET");
                    assertThat(authReq.getHeader(HttpHeaders.AUTHORIZATION)).startsWith("Basic ");

                    RecordedRequest apiReq = server.takeRequest();
                    assertThat(apiReq.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer jwt-basic");
                });
    }

    // -------------------------------------------------------------------------
    // FORM_POST strategy
    // -------------------------------------------------------------------------

    @Test
    void formPost_sendsFormEncodedBody() throws Exception {
        server.enqueue(tokenResponse("{\"access_token\":\"jwt-form\"}"));
        server.enqueue(apiResponse("{}"));

        runner.withPropertyValues(
                        "mg.clients.svc.base-url=" + baseUrl,
                        "mg.clients.svc.auth-url=" + baseUrl + "oauth/token",
                        "mg.clients.svc.username=user",
                        "mg.clients.svc.password=pass",
                        "mg.clients.svc.auth-method=FORM_POST",
                        "mg.clients.svc.token-field=access_token"
                )
                .run(ctx -> {
                    ctx.getBean(MgHttpClientFactory.class).get("svc").get("/ping", String.class);

                    RecordedRequest authReq = server.takeRequest();
                    assertThat(authReq.getMethod()).isEqualTo("POST");
                    assertThat(authReq.getHeader(HttpHeaders.CONTENT_TYPE))
                            .startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
                    String formBody = authReq.getBody().readUtf8();
                    assertThat(formBody).contains("username=user", "password=pass");

                    RecordedRequest apiReq = server.takeRequest();
                    assertThat(apiReq.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer jwt-form");
                });
    }

    // -------------------------------------------------------------------------
    // Additional token headers
    // -------------------------------------------------------------------------

    @Test
    void additionalTokenHeaders_areSetOnApiRequests() throws Exception {
        server.enqueue(tokenResponse("{\"token\":\"jwt-extra\"}"));
        server.enqueue(apiResponse("{}"));

        runner.withPropertyValues(
                        "mg.clients.svc.base-url=" + baseUrl,
                        "mg.clients.svc.auth-url=" + baseUrl + "auth/login",
                        "mg.clients.svc.username=user",
                        "mg.clients.svc.password=pass",
                        "mg.clients.svc.additional-token-headers[0]=x-access-token"
                )
                .run(ctx -> {
                    ctx.getBean(MgHttpClientFactory.class).get("svc").get("/ping", String.class);

                    server.takeRequest(); // auth
                    RecordedRequest apiReq = server.takeRequest();
                    assertThat(apiReq.getHeader("x-access-token")).isEqualTo("jwt-extra");
                });
    }

    // -------------------------------------------------------------------------
    // Custom auth strategy override
    // -------------------------------------------------------------------------

    @Test
    void customAuthStrategyBean_takesPreference_overDefault() throws Exception {
        server.enqueue(apiResponse("{}"));

        runner.withUserConfiguration(CustomStrategyConfig.class)
                .withPropertyValues(
                        "mg.clients.svc.base-url=" + baseUrl,
                        "mg.clients.svc.auth-url=" + baseUrl + "auth/login",
                        "mg.clients.svc.username=user",
                        "mg.clients.svc.password=pass"
                )
                .run(ctx -> {
                    ctx.getBean(MgHttpClientFactory.class).get("svc").get("/ping", String.class);

                    // Only 1 request recorded: the auth server was never called
                    assertThat(server.getRequestCount()).isEqualTo(1);
                    RecordedRequest apiReq = server.takeRequest();
                    assertThat(apiReq.getHeader(HttpHeaders.AUTHORIZATION))
                            .isEqualTo("Bearer custom-static-token");
                });
    }

    // -------------------------------------------------------------------------
    // Unknown client
    // -------------------------------------------------------------------------

    @Test
    void get_throwsIllegalArgument_forUnconfiguredClient() {
        runner.withPropertyValues(
                        "mg.clients.known.base-url=" + baseUrl,
                        "mg.clients.known.auth-url=" + baseUrl + "auth/login",
                        "mg.clients.known.username=u",
                        "mg.clients.known.password=p"
                )
                .run(ctx -> {
                    MgHttpClientFactory factory = ctx.getBean(MgHttpClientFactory.class);
                    assertThatThrownBy(() -> factory.get("unknown"))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessageContaining("mg.clients.unknown");
                });
    }

    // -------------------------------------------------------------------------
    // OAUTH2_CLIENT_CREDENTIALS strategy
    // -------------------------------------------------------------------------

    @Test
    void oauth2ClientCredentials_sendsGrantTypeAndClientCredentials() throws Exception {
        server.enqueue(tokenResponse("{\"access_token\":\"jwt-cc\"}"));
        server.enqueue(apiResponse("{}"));

        runner.withPropertyValues(
                        "mg.clients.svc.base-url=" + baseUrl,
                        "mg.clients.svc.auth-url=" + baseUrl + "oauth/token",
                        "mg.clients.svc.client-id=my-app",
                        "mg.clients.svc.client-secret=my-secret",
                        "mg.clients.svc.auth-method=OAUTH2_CLIENT_CREDENTIALS",
                        "mg.clients.svc.token-field=access_token"
                )
                .run(ctx -> {
                    ctx.getBean(MgHttpClientFactory.class).get("svc").get("/ping", String.class);

                    RecordedRequest authReq = server.takeRequest();
                    assertThat(authReq.getMethod()).isEqualTo("POST");
                    assertThat(authReq.getHeader(HttpHeaders.CONTENT_TYPE))
                            .startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
                    String body = authReq.getBody().readUtf8();
                    assertThat(body).contains("grant_type=client_credentials");
                    assertThat(body).contains("client_id=my-app");
                    assertThat(body).contains("client_secret=my-secret");

                    RecordedRequest apiReq = server.takeRequest();
                    assertThat(apiReq.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer jwt-cc");
                });
    }

    // -------------------------------------------------------------------------
    // OAUTH2_PASSWORD strategy
    // -------------------------------------------------------------------------

    @Test
    void oauth2Password_sendsGrantTypeAndUserCredentials() throws Exception {
        server.enqueue(tokenResponse("{\"access_token\":\"jwt-pw\"}"));
        server.enqueue(apiResponse("{}"));

        runner.withPropertyValues(
                        "mg.clients.svc.base-url=" + baseUrl,
                        "mg.clients.svc.auth-url=" + baseUrl + "oauth/token",
                        "mg.clients.svc.username=john",
                        "mg.clients.svc.password=secret",
                        "mg.clients.svc.client-id=my-app",
                        "mg.clients.svc.client-secret=my-secret",
                        "mg.clients.svc.auth-method=OAUTH2_PASSWORD",
                        "mg.clients.svc.token-field=access_token"
                )
                .run(ctx -> {
                    ctx.getBean(MgHttpClientFactory.class).get("svc").get("/ping", String.class);

                    RecordedRequest authReq = server.takeRequest();
                    String body = authReq.getBody().readUtf8();
                    assertThat(body).contains("grant_type=password");
                    assertThat(body).contains("username=john");
                    assertThat(body).contains("password=secret");
                    assertThat(body).contains("client_id=my-app");
                    assertThat(body).contains("client_secret=my-secret");

                    RecordedRequest apiReq = server.takeRequest();
                    assertThat(apiReq.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer jwt-pw");
                });
    }

    @Test
    void oauth2Password_withoutClientCredentials_omitsThemFromBody() throws Exception {
        server.enqueue(tokenResponse("{\"access_token\":\"jwt-pw-public\"}"));
        server.enqueue(apiResponse("{}"));

        runner.withPropertyValues(
                        "mg.clients.svc.base-url=" + baseUrl,
                        "mg.clients.svc.auth-url=" + baseUrl + "oauth/token",
                        "mg.clients.svc.username=john",
                        "mg.clients.svc.password=secret",
                        "mg.clients.svc.auth-method=OAUTH2_PASSWORD",
                        "mg.clients.svc.token-field=access_token"
                )
                .run(ctx -> {
                    ctx.getBean(MgHttpClientFactory.class).get("svc").get("/ping", String.class);

                    RecordedRequest authReq = server.takeRequest();
                    String body = authReq.getBody().readUtf8();
                    assertThat(body).contains("grant_type=password");
                    assertThat(body).doesNotContain("client_id");
                    assertThat(body).doesNotContain("client_secret");
                });
    }

    // -------------------------------------------------------------------------
    // API_KEY strategy
    // -------------------------------------------------------------------------

    @Test
    void apiKey_injectsKeyInDefaultHeader_withoutCallingAuthEndpoint() throws Exception {
        server.enqueue(apiResponse("{}"));

        runner.withPropertyValues(
                        "mg.clients.svc.base-url=" + baseUrl,
                        "mg.clients.svc.api-key=my-secret-key",
                        "mg.clients.svc.auth-method=API_KEY"
                )
                .run(ctx -> {
                    ctx.getBean(MgHttpClientFactory.class).get("svc").get("/ping", String.class);

                    // No auth endpoint called — only 1 request total
                    assertThat(server.getRequestCount()).isEqualTo(1);
                    RecordedRequest apiReq = server.takeRequest();
                    assertThat(apiReq.getHeader("X-API-Key")).isEqualTo("my-secret-key");
                    assertThat(apiReq.getHeader(HttpHeaders.AUTHORIZATION)).isNull();
                });
    }

    @Test
    void apiKey_usesCustomHeader_whenConfigured() throws Exception {
        server.enqueue(apiResponse("{}"));

        runner.withPropertyValues(
                        "mg.clients.svc.base-url=" + baseUrl,
                        "mg.clients.svc.api-key=my-secret-key",
                        "mg.clients.svc.auth-method=API_KEY",
                        "mg.clients.svc.api-key-header=X-Goog-Api-Key"
                )
                .run(ctx -> {
                    ctx.getBean(MgHttpClientFactory.class).get("svc").get("/ping", String.class);

                    RecordedRequest apiReq = server.takeRequest();
                    assertThat(apiReq.getHeader("X-Goog-Api-Key")).isEqualTo("my-secret-key");
                    assertThat(apiReq.getHeader("X-API-Key")).isNull();
                });
    }

    @Test
    void bearerApiKey_injectsKeyAsBearerToken_withoutCallingAuthEndpoint() throws Exception {
        server.enqueue(apiResponse("{}"));

        runner.withPropertyValues(
                        "mg.clients.svc.base-url=" + baseUrl,
                        "mg.clients.svc.api-key=my-secret-key",
                        "mg.clients.svc.auth-method=BEARER_API_KEY"
                )
                .run(ctx -> {
                    ctx.getBean(MgHttpClientFactory.class).get("svc").get("/ping", String.class);

                    // No auth endpoint called — only 1 request total
                    assertThat(server.getRequestCount()).isEqualTo(1);
                    RecordedRequest apiReq = server.takeRequest();
                    assertThat(apiReq.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer my-secret-key");
                    assertThat(apiReq.getHeader("X-API-Key")).isNull();
                });
    }

    // -------------------------------------------------------------------------
    // Static request headers
    // -------------------------------------------------------------------------

    @Test
    void requestHeaders_areAddedToEveryApiRequest() throws Exception {
        server.enqueue(tokenResponse("{\"token\":\"jwt-hdr\"}"));
        server.enqueue(apiResponse("{}"));
        server.enqueue(apiResponse("{}"));

        runner.withPropertyValues(
                        "mg.clients.svc.base-url=" + baseUrl,
                        "mg.clients.svc.auth-url=" + baseUrl + "auth/login",
                        "mg.clients.svc.username=user",
                        "mg.clients.svc.password=pass",
                        "mg.clients.svc.token-expiration-delay=3600",
                        "mg.clients.svc.request-headers.x-api-version=2",
                        "mg.clients.svc.request-headers.x-tenant-id=acme"
                )
                .run(ctx -> {
                    MgHttpClient client = ctx.getBean(MgHttpClientFactory.class).get("svc");
                    client.get("/a", String.class);
                    client.get("/b", String.class);

                    server.takeRequest(); // auth
                    RecordedRequest req1 = server.takeRequest();
                    assertThat(req1.getHeader("x-api-version")).isEqualTo("2");
                    assertThat(req1.getHeader("x-tenant-id")).isEqualTo("acme");

                    RecordedRequest req2 = server.takeRequest();
                    assertThat(req2.getHeader("x-api-version")).isEqualTo("2");
                    assertThat(req2.getHeader("x-tenant-id")).isEqualTo("acme");
                });
    }

    // -------------------------------------------------------------------------
    // Token caching — auth endpoint called only once across multiple requests
    // -------------------------------------------------------------------------

    @Test
    void token_isCached_acrossMultipleRequests() throws Exception {
        server.enqueue(tokenResponse("{\"token\":\"jwt-cached\"}"));
        server.enqueue(apiResponse("{}"));
        server.enqueue(apiResponse("{}"));

        runner.withPropertyValues(
                        "mg.clients.svc.base-url=" + baseUrl,
                        "mg.clients.svc.auth-url=" + baseUrl + "auth/login",
                        "mg.clients.svc.username=user",
                        "mg.clients.svc.password=pass",
                        "mg.clients.svc.token-expiration-delay=3600"
                )
                .run(ctx -> {
                    MgHttpClient client = ctx.getBean(MgHttpClientFactory.class).get("svc");
                    client.get("/a", String.class);
                    client.get("/b", String.class);

                    // 1 auth + 2 api = 3 total (not 2 auth calls)
                    assertThat(server.getRequestCount()).isEqualTo(3);
                });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private MockResponse tokenResponse(String json) {
        return new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(json);
    }

    private MockResponse apiResponse(String json) {
        return new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(json);
    }

    /** Registers a custom auth strategy bean for the "svc" client. */
    @Configuration
    static class CustomStrategyConfig {
        @Bean("svcMgAuthStrategy")
        AuthStrategy svcCustomAuth() {
            return config -> "custom-static-token";
        }
    }
}
