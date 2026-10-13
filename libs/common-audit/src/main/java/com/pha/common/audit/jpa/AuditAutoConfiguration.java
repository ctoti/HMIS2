package com.pha.common.audit.jpa;

import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Adds the audit entities and repositories to the service's JPA setup without overriding its own entity scan,
 * migrates the audit schema, and exposes {@link AuditLog}.
 */
@AutoConfiguration(
        after = DataSourceAutoConfiguration.class,
        before = {HibernateJpaAutoConfiguration.class, DataJpaRepositoriesAutoConfiguration.class})
@AutoConfigurationPackage(basePackageClasses = AuditAutoConfiguration.class)
@ConditionalOnBean(DataSource.class)
public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    AuditSchemaMigrator auditSchemaMigrator(DataSource dataSource) {
        return new AuditSchemaMigrator(dataSource);
    }

    // Taking the migrator as a parameter makes Spring migrate the schema before the audit log can be used.
    @Bean
    @ConditionalOnMissingBean(AuditLog.class)
    JpaAuditLog auditLog(AuditSchemaMigrator migrator, AuditLogEntryRepository entries, AuditChainHeadRepository heads) {
        return new JpaAuditLog(entries, heads);
    }
}
