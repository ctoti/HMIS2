package com.pha.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.status.Status;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Loads the shipped logback include exactly as a service would, so a config element the encoder ignores fails here.
 */
class JsonConsoleAppenderTest {

    private static final String CONFIG = """
            <configuration>
                <include resource="com/pha/common/logging/json-console-appender.xml"/>
                <root level="INFO"><appender-ref ref="JSON_CONSOLE"/></root>
            </configuration>
            """;

    private final LoggerContext context = new LoggerContext();

    @BeforeEach
    void configure() throws Exception {
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        configurator.doConfigure(new ByteArrayInputStream(CONFIG.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void configuresWithoutWarnings() {
        assertThat(context.getStatusManager().getCopyOfStatusList())
                .filteredOn(status -> status.getLevel() >= Status.WARN)
                .isEmpty();
    }

    @Test
    void masksPersonalDataInJsonOutput() {
        Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
        @SuppressWarnings("unchecked")
        OutputStreamAppender<ILoggingEvent> appender = (OutputStreamAppender<ILoggingEvent>) root.getAppender("JSON_CONSOLE");
        LoggingEvent event = new LoggingEvent(Logger.class.getName(), root, Level.INFO,
                "leave approved, notified nurse.kila@pha.gov.pg", null, null);
        // A standalone LoggerContext has no MDC adapter; give the event its own empty MDC.
        event.setMDCPropertyMap(Map.of());

        String json = new String(appender.getEncoder().encode(event), StandardCharsets.UTF_8);

        assertThat(json).contains("notified " + PiiRedactor.MASK).doesNotContain("nurse.kila@pha.gov.pg");
    }
}
