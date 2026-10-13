package com.pha.hr.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Facility-scoped checks for {@code @PreAuthorize} expressions, e.g.
 * {@code @facilityAccess.canManage(authentication, #facilityCode)}.
 */
@Component("facilityAccess")
public class FacilityAccess {

    public boolean canManage(Authentication authentication, String facilityCode) {
        return facilityCode != null && Caller.from(authentication).canManageFacility(facilityCode);
    }
}
