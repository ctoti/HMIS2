package com.pha.hr;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.pha.hr.security.KeycloakRealmRoleConverter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Shares one PostgreSQL container and one Spring context across the integration tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class PostgresIntegrationTest {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    /** A signed-in user with the given realm roles and facility groups, as Keycloak would issue. */
    protected static JwtRequestPostProcessor user(String userId, List<String> roles, String... facilities) {
        List<String> groups = Arrays.stream(facilities).map(code -> "/facilities/" + code).toList();
        return jwt()
                .jwt(token -> token
                        .subject(userId)
                        .claim("realm_access", Map.of("roles", roles))
                        .claim("groups", groups))
                .authorities(new KeycloakRealmRoleConverter());
    }

    protected static String newUserId() {
        return UUID.randomUUID().toString();
    }

    /** Facility codes unique to one test, so tests sharing the database never see each other's rows. */
    protected static String newFacilityCode() {
        return "F" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    protected static String newEmployeeNumber() {
        return "E-" + UUID.randomUUID().toString().substring(0, 12);
    }
}
