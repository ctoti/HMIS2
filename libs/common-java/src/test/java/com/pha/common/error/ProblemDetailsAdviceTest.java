package com.pha.common.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pha.common.web.CorrelationIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

class ProblemDetailsAdviceTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new ProblemDetailsAdvice())
                .addFilters(new CorrelationIdFilter())
                .build();
    }

    @Test
    void notFoundBecomesProblemDetail() throws Exception {
        mvc.perform(get("/things/42").header(CorrelationIdFilter.HEADER, "corr-12345678"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:pha:problem:not-found"))
                .andExpect(jsonPath("$.detail").value("Thing 42 was not found"))
                .andExpect(jsonPath("$.correlationId").value("corr-12345678"));
    }

    @Test
    void unexpectedErrorHidesInternalMessage() throws Exception {
        mvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("urn:pha:problem:internal-error"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void frameworkErrorsAlsoCarryCorrelationId() throws Exception {
        mvc.perform(post("/boom").header(CorrelationIdFilter.HEADER, "corr-12345678"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.correlationId").value("corr-12345678"));
    }

    @RestController
    static class TestController {

        @GetMapping("/things/{id}")
        String thing(@PathVariable String id) {
            throw new ResourceNotFoundException("Thing", id);
        }

        @GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("db password=hunter2 leaked");
        }
    }
}
