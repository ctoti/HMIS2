package com.pha.hr;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;

/**
 * Fails when a controller endpoint is missing from the OpenAPI document, or the document lists one that does not
 * exist.
 */
class OpenApiContractTest extends PostgresIntegrationTest {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mappings;

    @Test
    void documentsExactlyTheImplementedEndpoints() throws IOException {
        assertThat(documented()).isEqualTo(implemented());
    }

    private Set<String> implemented() {
        Set<String> endpoints = new TreeSet<>();
        for (RequestMappingInfo info : mappings.getHandlerMethods().keySet()) {
            for (String path : info.getPatternValues()) {
                if (path.startsWith("/api/")) {
                    info.getMethodsCondition().getMethods().forEach(method -> endpoints.add(method + " " + path));
                }
            }
        }
        return endpoints;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> documented() throws IOException {
        try (InputStream spec = OpenApiContractTest.class.getResourceAsStream("/openapi/hr-service.yaml")) {
            Map<String, Object> document = new Yaml().load(spec);
            assertThat(document.get("openapi")).isEqualTo("3.0.3");
            Set<String> endpoints = new TreeSet<>();
            ((Map<String, Map<String, Object>>) document.get("paths")).forEach((path, operations) ->
                    operations.keySet().forEach(method -> endpoints.add(method.toUpperCase() + " " + path)));
            return endpoints;
        }
    }
}
