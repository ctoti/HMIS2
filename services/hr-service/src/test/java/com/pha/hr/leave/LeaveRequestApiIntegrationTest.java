package com.pha.hr.leave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.pha.common.audit.jpa.AuditLog;
import com.pha.hr.PostgresIntegrationTest;
import com.pha.hr.security.Caller;
import com.pha.hr.staff.CreateStaffRequest;
import com.pha.hr.staff.StaffService;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

class LeaveRequestApiIntegrationTest extends PostgresIntegrationTest {

    private static final List<String> STAFF = List.of("staff");
    private static final List<String> HR_OFFICER = List.of("staff", "hr-officer");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private StaffService staffService;

    @Autowired
    private LeaveRequestService leaveService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AuditLog auditLog;

    private String facility;
    private String employee;

    @BeforeEach
    void linkedStaffMember() {
        facility = newFacilityCode();
        employee = newUserId();
        staffService.create(new Caller("seed", Set.of("platform-admin"), Set.of()), new CreateStaffRequest(
                newEmployeeNumber(), employee, "John", "Wari", facility, "Health Extension Officer"));
    }

    @Test
    void submitsALeaveRequestAndAuditsIt() throws Exception {
        submit(employee, "key-submit-1", "ANNUAL", "2026-11-02", "2026-11-06")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/v1/hr/leave-requests/")))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.facilityCode").value(facility))
                .andExpect(jsonPath("$.calendarDays").value(5));

        String payload = jdbc.queryForObject(
                "select payload from audit.audit_log where action = 'leave-request.submitted' and actor_id = ?",
                String.class, employee);
        assertThat(payload).contains("ANNUAL", "2026-11-02", facility).doesNotContain("John").doesNotContain("Wari");
        assertThat(auditLog.verify().intact()).isTrue();
    }

    @Test
    void replaysTheSameIdempotencyKeyAndRejectsItsReuseForDifferentContent() throws Exception {
        String first = id(submit(employee, "key-replay-1", "SICK", "2026-12-01", "2026-12-02"));

        submit(employee, "key-replay-1", "SICK", "2026-12-01", "2026-12-02")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(first));
        submit(employee, "key-replay-1", "SICK", "2026-12-01", "2026-12-03")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:pha:problem:hr:idempotency-key-reused"));
        assertThat(countFor(employee)).isEqualTo(1);
    }

    @Test
    void concurrentRetriesWithOneKeyCreateOneRequest() throws Exception {
        Caller caller = new Caller(employee, Set.of("staff"), Set.of(facility));
        SubmitLeaveRequest request = new SubmitLeaveRequest(
                LeaveType.FAMILY, LocalDate.parse("2027-01-04"), LocalDate.parse("2027-01-05"));
        ExecutorService pool = Executors.newFixedThreadPool(6);
        try {
            List<Future<LeaveSubmission>> attempts = IntStream.range(0, 12)
                    .mapToObj(i -> pool.submit(() -> leaveService.submit(caller, "key-concurrent", request)))
                    .toList();
            long created = 0;
            for (Future<LeaveSubmission> attempt : attempts) {
                created += attempt.get().created() ? 1 : 0;
            }
            assertThat(created).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
        assertThat(countFor(employee)).isEqualTo(1);
    }

    @Test
    void rejectsOverlappingAndInvalidRequests() throws Exception {
        submit(employee, "key-overlap-1", "ANNUAL", "2027-02-01", "2027-02-10").andExpect(status().isCreated());

        submit(employee, "key-overlap-2", "STUDY", "2027-02-10", "2027-02-12")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:pha:problem:hr:overlapping-leave"));
        submit(employee, "key-overlap-3", "STUDY", "2027-02-12", "2027-02-11")
                .andExpect(status().isBadRequest());
        submit(employee, "short", "STUDY", "2027-03-01", "2027-03-02")
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/hr/leave-requests").with(user(employee, STAFF, facility))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"leaveType\":\"HOLIDAY\",\"startDate\":\"2027-03-01\",\"endDate\":\"2027-03-02\"}")
                        .header("Idempotency-Key", "key-bad-type"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/hr/leave-requests").with(user(employee, STAFF, facility))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("ANNUAL", "2027-03-01", "2027-03-02")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresALinkedStaffRecord() throws Exception {
        submit(newUserId(), "key-unlinked", "ANNUAL", "2027-04-01", "2027-04-02")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:pha:problem:hr:staff-record-not-linked"));
    }

    @Test
    void scopesListsAndReadsToTheRequesterAndTheirFacilityOfficers() throws Exception {
        String own = id(submit(employee, "key-list-1", "ANNUAL", "2027-05-03", "2027-05-04"));
        String officer = newUserId();
        String outsider = newUserId();

        mvc.perform(get("/api/v1/hr/leave-requests").with(user(employee, STAFF, facility)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(own));
        mvc.perform(get("/api/v1/hr/leave-requests").param("facilityCode", facility).param("status", "SUBMITTED")
                        .with(user(officer, HR_OFFICER, facility)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/hr/leave-requests").param("facilityCode", facility).param("status", "CANCELLED")
                        .with(user(officer, HR_OFFICER, facility)))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/hr/leave-requests").param("facilityCode", facility)
                        .with(user(employee, STAFF, facility)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/hr/leave-requests/" + own).with(user(officer, HR_OFFICER, facility)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/hr/leave-requests/" + own).with(user(outsider, HR_OFFICER, newFacilityCode())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/hr/leave-requests/" + UUID.randomUUID()).with(user(employee, STAFF, facility)))
                .andExpect(status().isNotFound());
    }

    @Test
    void onlyTheRequesterCancelsAndOnlyOnce() throws Exception {
        String own = id(submit(employee, "key-cancel-1", "ANNUAL", "2027-06-07", "2027-06-08"));

        mvc.perform(post("/api/v1/hr/leave-requests/" + own + "/cancel")
                        .with(user(newUserId(), HR_OFFICER, facility)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/hr/leave-requests/" + own + "/cancel").with(user(employee, STAFF, facility)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(post("/api/v1/hr/leave-requests/" + own + "/cancel").with(user(employee, STAFF, facility)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:pha:problem:hr:leave-not-cancellable"));

        // A cancelled request no longer blocks new requests for the same dates.
        submit(employee, "key-cancel-2", "ANNUAL", "2027-06-07", "2027-06-08").andExpect(status().isCreated());
        assertThat(jdbc.queryForObject(
                "select count(*) from audit.audit_log where action = 'leave-request.cancelled' and resource_id = ?",
                Integer.class, own)).isEqualTo(1);
    }

    private ResultActions submit(String userId, String key, String type, String start, String end) throws Exception {
        return mvc.perform(post("/api/v1/hr/leave-requests")
                .with(user(userId, STAFF, facility))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(type, start, end)));
    }

    private static String body(String type, String start, String end) {
        return "{\"leaveType\":\"%s\",\"startDate\":\"%s\",\"endDate\":\"%s\"}".formatted(type, start, end);
    }

    private static String id(ResultActions created) throws Exception {
        String json = created.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private Integer countFor(String userId) {
        return jdbc.queryForObject("select count(*) from leave_request where requested_by = ?", Integer.class, userId);
    }
}
