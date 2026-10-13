package com.pha.hr.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class KeycloakRealmRoleConverterTest {

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    @Test
    void prefixesEachRealmRole() {
        assertThat(converter.convert(CallerTest.token(Map.of("realm_access", Map.of("roles", List.of("staff", "hr-officer"))))))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_staff", "ROLE_hr-officer");
    }

    @Test
    void ignoresMalformedClaims() {
        assertThat(converter.convert(CallerTest.token(Map.of("realm_access", "staff")))).isEmpty();
        assertThat(converter.convert(CallerTest.token(Map.of("realm_access", Map.of("roles", "staff"))))).isEmpty();
        assertThat(converter.convert(CallerTest.token(Map.of("realm_access", Map.of("roles", List.of(1, "staff"))))))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_staff");
    }
}
