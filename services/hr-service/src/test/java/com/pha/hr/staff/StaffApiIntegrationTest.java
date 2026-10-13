package com.pha.hr.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pha.common.audit.jpa.AuditLog;
import com.jayway.jsonpath.JsonPath;
import com.pha.hr.PostgresIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

class StaffApiIntegrationTest extends PostgresIntegrationTest {

    private static final List<String> HR_OFFICER = List.of("staff", "hr-officer");
    private static final List<String> STAFF = List.of("staff");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AuditLog auditLog;

    @Test
    void rejectsCallsWithoutATokenAsProblemDetails() throws Exception {
        mvc.perform(get("/api/v1/hr/staff/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(header().string("Content-Type", startsWith("application/problem+json")))
                .andExpect(jsonPath("$.type").value("urn:pha:problem:unauthorized"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void hrOfficerCreatesStaffAtTheirFacilityAndTheWriteIsAudited() throws Exception {
        String facility = newFacilityCode();
        String officer = newUserId();
        Integer auditBefore = jdbc.queryForObject("select count(*) from audit.audit_log", Integer.class);

        create(officer, facility, newEmployeeNumber(), null)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/v1/hr/staff/")))
                .andExpect(jsonPath("$.facilityCode").value(facility))
                .andExpect(jsonPath("$.givenName").value("Mary"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        assertThat(jdbc.queryForObject("select count(*) from audit.audit_log", Integer.class)).isEqualTo(auditBefore + 1);
        String payload = jdbc.queryForObject(
                "select payload from audit.audit_log where action = 'staff.created' and actor_id = ?", String.class, officer);
        assertThat(payload).contains(facility).doesNotContain("Mary").doesNotContain("Kila");
        assertThat(auditLog.verify().intact()).isTrue();
    }

    @Test
    void hrOfficerCannotCreateStaffAtAnotherFacility() throws Exception {
        mvc.perform(post("/api/v1/hr/staff")
                        .with(user(newUserId(), HR_OFFICER, newFacilityCode()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(newFacilityCode(), newEmployeeNumber(), null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:pha:problem:forbidden"));
    }

    @Test
    void platformAdminCanCreateStaffAnywhere() throws Exception {
        mvc.perform(post("/api/v1/hr/staff")
                        .with(user(newUserId(), List.of("platform-admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(newFacilityCode(), newEmployeeNumber(), null)))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsDuplicateEmployeeNumbersAndLinkedAccounts() throws Exception {
        String facility = newFacilityCode();
        String officer = newUserId();
        String employeeNumber = newEmployeeNumber();
        String account = newUserId();
        create(officer, facility, employeeNumber, account).andExpect(status().isCreated());

        create(officer, facility, employeeNumber, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:pha:problem:hr:duplicate-employee-number"));
        create(officer, facility, newEmployeeNumber(), account)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:pha:problem:hr:account-already-linked"));
    }

    @Test
    void rejectsInvalidBodiesWithFieldErrors() throws Exception {
        String facility = newFacilityCode();
        mvc.perform(post("/api/v1/hr/staff")
                        .with(user(newUserId(), HR_OFFICER, facility))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"employeeNumber":"bad number!","givenName":"","familyName":"Kila",
                                 "facilityCode":"%s","positionTitle":"Nurse"}""".formatted(facility)))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", startsWith("application/problem+json")));
    }

    @Test
    void listsStaffAtAFacilityOnePageAtATime() throws Exception {
        String facility = newFacilityCode();
        String officer = newUserId();
        for (int i = 0; i < 3; i++) {
            create(officer, facility, newEmployeeNumber(), null).andExpect(status().isCreated());
        }
        create(officer, newFacilityCode(), newEmployeeNumber(), null).andExpect(status().isCreated());

        mvc.perform(get("/api/v1/hr/staff").param("facilityCode", facility).param("size", "2")
                        .with(user(officer, HR_OFFICER, facility)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/v1/hr/staff").param("facilityCode", facility).param("size", "500")
                        .with(user(officer, HR_OFFICER, facility)))
                .andExpect(jsonPath("$.size").value(100));
        mvc.perform(get("/api/v1/hr/staff").param("facilityCode", facility).param("sort", "noSuchField,asc")
                        .with(user(officer, HR_OFFICER, facility)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:pha:problem:invalid-sort"));
        mvc.perform(get("/api/v1/hr/staff").param("facilityCode", facility).with(user(officer, STAFF, facility)))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffSeeTheirOwnRecordButNotOthers() throws Exception {
        String facility = newFacilityCode();
        String officer = newUserId();
        String account = newUserId();
        String ownId = id(create(officer, facility, newEmployeeNumber(), account));
        String otherId = id(create(officer, facility, newEmployeeNumber(), null));

        mvc.perform(get("/api/v1/hr/staff/me").with(user(account, STAFF, facility)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ownId));
        mvc.perform(get("/api/v1/hr/staff/" + ownId).with(user(account, STAFF, facility)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/hr/staff/" + otherId).with(user(account, STAFF, facility)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/hr/staff/" + otherId).with(user(officer, HR_OFFICER, facility)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/hr/staff/me").with(user(newUserId(), STAFF)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:pha:problem:not-found"));
        mvc.perform(get("/api/v1/hr/staff/not-a-uuid").with(user(account, STAFF)))
                .andExpect(status().isBadRequest());
    }

    private ResultActions create(String officer, String facility, String employeeNumber, String account) throws Exception {
        return mvc.perform(post("/api/v1/hr/staff")
                .with(user(officer, HR_OFFICER, facility))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(facility, employeeNumber, account)));
    }

    static String body(String facility, String employeeNumber, String account) {
        String userId = account == null ? "null" : "\"" + account + "\"";
        return """
                {"employeeNumber":"%s","userId":%s,"givenName":"Mary","familyName":"Kila",
                 "facilityCode":"%s","positionTitle":"Nursing Officer"}""".formatted(employeeNumber, userId, facility);
    }

    private static String id(ResultActions created) throws Exception {
        String json = created.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }
}
