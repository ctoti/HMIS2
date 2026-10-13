package com.pha.common.audit.jpa;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.InitializingBean;

/**
 * Applies the audit schema with its own Flyway history table, separate from the service's own migrations.
 * Deliberately not a {@link Flyway} bean, which would switch off Spring Boot's Flyway auto-configuration.
 */
public class AuditSchemaMigrator implements InitializingBean {

    static final String SCHEMA = "audit";
    static final String HISTORY_TABLE = "flyway_audit_history";
    static final String LOCATION = "classpath:db/audit";

    private final DataSource dataSource;

    public AuditSchemaMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() {
        migrate();
    }

    void migrate() {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas(SCHEMA)
                .createSchemas(true)
                .table(HISTORY_TABLE)
                .locations(LOCATION)
                .load()
                .migrate();
    }
}
