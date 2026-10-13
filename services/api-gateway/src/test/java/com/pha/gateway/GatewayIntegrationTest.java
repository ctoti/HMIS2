package com.pha.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = {
        "RATE_LIMIT_PER_SECOND=1", "RATE_LIMIT_BURST=2", "BACKEND_TIMEOUT=3s",
        "logging.level.org.springframework.web.cors=DEBUG"})
@Testcontainers
class GatewayIntegrationTest {

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    private static final AtomicInteger FLAKY_CALLS = new AtomicInteger();
    private static final HttpServer BACKEND = startBackend();
    private static final int DEAD_PORT = unusedPort();

    @Autowired
    private ApplicationContext context;

    private WebTestClient client;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("HR_SERVICE_URI", () -> "http://localhost:" + BACKEND.getAddress().getPort());
        registry.add("FACILITIES_SERVICE_URI", () -> "http://localhost:" + DEAD_PORT);
    }

    @AfterAll
    static void stopBackend() {
        BACKEND.stop(0);
    }

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToApplicationContext(context).apply(springSecurity()).configureClient().build();
    }

    @Test
    void rejectsRequestsWithoutTokenAsProblemDetails() {
        client.get().uri("/api/v1/hr/ping")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .expectHeader().exists("X-Correlation-Id")
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.type").isEqualTo("urn:pha:problem:unauthorized")
                .jsonPath("$.status").isEqualTo(401);
    }

    @Test
    void healthIsPublic() {
        client.get().uri("/actuator/health").exchange().expectStatus().isOk();
    }

    @Test
    void proxiesAuthenticatedRequestsAndPropagatesCorrelationId() {
        client.mutateWith(mockJwt().jwt(jwt -> jwt.subject("user-proxy")))
                .get().uri("/api/v1/hr/ping")
                .header("X-Correlation-Id", "corr-12345678")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Correlation-Id", "corr-12345678")
                .expectBody()
                .jsonPath("$.correlationId").isEqualTo("corr-12345678");
    }

    @Test
    void retriesReadsThatFailWithServerErrors() {
        client.mutateWith(mockJwt().jwt(jwt -> jwt.subject("user-retry")))
                .get().uri("/api/v1/hr/flaky")
                .exchange()
                .expectStatus().isOk();

        assertThat(FLAKY_CALLS.get()).isEqualTo(2);
    }

    @Test
    void answersWithServiceUnavailableWhenBackendIsDown() {
        client.mutateWith(mockJwt().jwt(jwt -> jwt.subject("user-down")))
                .get().uri("/api/v1/facilities/assets")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectHeader().valueEquals("Retry-After", "30")
                .expectBody()
                .jsonPath("$.type").isEqualTo("urn:pha:problem:service-unavailable");
    }

    @Test
    void rateLimitsEachUser() {
        WebTestClient user = client.mutateWith(mockJwt().jwt(jwt -> jwt.subject("user-rate")));
        List<Integer> statuses = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            statuses.add(user.get().uri("/api/v1/hr/ping").exchange().returnResult(String.class).getStatus().value());
        }

        assertThat(statuses).contains(200, 429);
    }

    @Test
    void acceptsWritesWithBearerTokenAndNoCookies() {
        client.mutateWith(mockJwt().jwt(jwt -> jwt.subject("user-write")))
                .post().uri("/api/v1/hr/ping")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void requiresCsrfTokenForWritesThatCarryCookies() {
        client.mutateWith(mockJwt().jwt(jwt -> jwt.subject("user-cookie")))
                .post().uri("/api/v1/hr/ping")
                .cookie("SESSION", "abc")
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.type").isEqualTo("urn:pha:problem:forbidden");
    }

    @Test
    void bindsPortalOriginFromConfiguration() {
        assertThat(context.getBean(PhaGatewayProperties.class).allowedOrigins()).containsExactly("http://localhost:5173");
    }

    @Test
    void allowsPreflightFromThePortal() {
        client.options().uri("/api/v1/hr/ping")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173");
    }

    @Test
    void rejectsPreflightFromOtherOrigins() {
        client.options().uri("/api/v1/hr/ping")
                .header(HttpHeaders.ORIGIN, "https://evil.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange()
                .expectStatus().isForbidden();
    }

    private static HttpServer startBackend() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/api/v1/hr/ping", exchange -> respond(exchange, 200,
                    "{\"correlationId\":\"" + exchange.getRequestHeaders().getFirst("X-Correlation-Id") + "\"}"));
            server.createContext("/api/v1/hr/flaky", exchange ->
                    respond(exchange, FLAKY_CALLS.incrementAndGet() == 1 ? 503 : 200, "{}"));
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void respond(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static int unusedPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
