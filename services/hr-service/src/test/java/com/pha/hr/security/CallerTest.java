package com.pha.hr.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class CallerTest {

    @Test
    void readsSubjectRolesAndFacilitiesFromTheToken() {
        Jwt jwt = token(Map.of(
                "realm_access", Map.of("roles", List.of("staff", "hr-officer")),
                "groups", List.of("/facilities/PMGH", "/facilities/ANGAU", "/facilities/", "/facilities/A/B", "/other")));

        Caller caller = Caller.from(SecurityConfig.jwtAuthenticationConverter().convert(jwt));

        assertThat(caller.userId()).isEqualTo("user-1");
        assertThat(caller.roles()).containsExactlyInAnyOrder("staff", "hr-officer");
        assertThat(caller.facilities()).containsExactlyInAnyOrder("PMGH", "ANGAU");
    }

    @Test
    void treatsMissingClaimsAsNoAccess() {
        Caller caller = Caller.from(new JwtAuthenticationToken(token(Map.of("scope", "openid")), List.of()));

        assertThat(caller.roles()).isEmpty();
        assertThat(caller.facilities()).isEmpty();
        assertThat(caller.canManageFacility("PMGH")).isFalse();
    }

    @Test
    void hrOfficersManageOnlyTheirOwnFacilities() {
        Caller officer = new Caller("u", Set.of("hr-officer"), Set.of("PMGH"));
        Caller staff = new Caller("u", Set.of("staff"), Set.of("PMGH"));
        Caller admin = new Caller("u", Set.of("platform-admin"), Set.of());

        assertThat(officer.canManageFacility("PMGH")).isTrue();
        assertThat(officer.canManageFacility("ANGAU")).isFalse();
        assertThat(staff.canManageFacility("PMGH")).isFalse();
        assertThat(admin.canManageFacility("ANGAU")).isTrue();
    }

    @Test
    void refusesNonBearerAuthentication() {
        assertThatThrownBy(() -> Caller.from(new TestingAuthenticationToken("user", "password")))
                .isInstanceOf(InsufficientAuthenticationException.class);
    }

    @Test
    void facilityAccessRejectsMissingFacilityCodes() {
        Authentication officer = SecurityConfig.jwtAuthenticationConverter().convert(token(Map.of(
                "realm_access", Map.of("roles", List.of("hr-officer")), "groups", List.of("/facilities/PMGH"))));
        FacilityAccess access = new FacilityAccess();

        assertThat(access.canManage(officer, "PMGH")).isTrue();
        assertThat(access.canManage(officer, null)).isFalse();
    }

    static Jwt token(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claims(all -> all.putAll(claims))
                .build();
    }
}
