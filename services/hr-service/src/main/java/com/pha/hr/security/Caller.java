package com.pha.hr.security;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * The signed-in user making a request: their Keycloak subject, realm roles and the facilities they are scoped to.
 */
public record Caller(String userId, Set<String> roles, Set<String> facilities) {

    public static final String PLATFORM_ADMIN = "platform-admin";
    public static final String HR_OFFICER = "hr-officer";

    static final String FACILITY_GROUP_PREFIX = "/facilities/";

    public Caller {
        roles = Set.copyOf(roles);
        facilities = Set.copyOf(facilities);
    }

    public static Caller from(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken token)) {
            throw new InsufficientAuthenticationException("A bearer token is required");
        }
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(KeycloakRealmRoleConverter.ROLE_PREFIX))
                .map(authority -> authority.substring(KeycloakRealmRoleConverter.ROLE_PREFIX.length()))
                .collect(Collectors.toUnmodifiableSet());
        return new Caller(token.getToken().getSubject(), roles, facilityCodes(token.getToken().getClaimAsStringList("groups")));
    }

    static Set<String> facilityCodes(List<String> groups) {
        if (groups == null) {
            return Set.of();
        }
        return groups.stream()
                .filter(path -> path.startsWith(FACILITY_GROUP_PREFIX))
                .map(path -> path.substring(FACILITY_GROUP_PREFIX.length()))
                .filter(code -> !code.isEmpty() && !code.contains("/"))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isPlatformAdmin() {
        return roles.contains(PLATFORM_ADMIN);
    }

    /** HR officers manage staff and leave only at the facilities they are assigned to. */
    public boolean canManageFacility(String facilityCode) {
        return isPlatformAdmin() || (roles.contains(HR_OFFICER) && facilities.contains(facilityCode));
    }
}
