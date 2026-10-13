package com.pha.hr.staff;

public final class StaffRules {

    /** Facility codes as used in Keycloak group paths, e.g. {@code /facilities/PMGH}. */
    public static final String FACILITY_CODE = "[A-Z0-9][A-Z0-9-]{1,31}";

    private StaffRules() {
    }
}
