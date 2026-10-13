package com.pha.common.audit.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pha.common.audit.AuditHashChain;
import com.pha.common.audit.AuditRecord;
import com.pha.common.audit.SealedAuditEntry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(classes = TestApplication.class)
@Testcontainers
class JpaAuditLogIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15-alpine");

    @Autowired
    private AuditLog auditLog;

    @Autowired
    private AuditLogEntryRepository entries;

    @Autowired
    private AuditSchemaMigrator migrator;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionTemplate transactions;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void appendsLinkedEntriesAndVerifiesThem() {
        long before = entries.count();

        SealedAuditEntry first = auditLog.append(record("{\"days\":3}"));
        SealedAuditEntry second = auditLog.append(record("{\"days\":4}"));

        assertThat(second.previousHash()).isEqualTo(first.hash());
        assertThat(entries.count()).isEqualTo(before + 2);
        ChainVerification verification = auditLog.verify();
        assertThat(verification.intact()).isTrue();
        assertThat(verification.entriesChecked()).isEqualTo(before + 2);
    }

    @Test
    void keepsTheChainIntactUnderConcurrentWriters() throws Exception {
        long before = entries.count();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<SealedAuditEntry>> writes = IntStream.range(0, 80)
                    .mapToObj(i -> pool.submit(() -> auditLog.append(record("{\"n\":" + i + "}"))))
                    .toList();
            for (Future<SealedAuditEntry> write : writes) {
                write.get();
            }
        } finally {
            pool.shutdown();
        }

        assertThat(entries.count()).isEqualTo(before + 80);
        assertThat(auditLog.verify().intact()).isTrue();
    }

    @Test
    void verifiesLogsLongerThanOnePage() {
        IntStream.range(0, 520).forEach(i -> auditLog.append(record("{\"bulk\":" + i + "}")));

        ChainVerification verification = auditLog.verify();

        assertThat(verification.intact()).isTrue();
        assertThat(verification.entriesChecked()).isEqualTo(entries.count());
    }

    @Test
    void databaseRejectsUpdatesDeletesAndTruncation() {
        auditLog.append(record("{}"));

        assertThatThrownBy(() -> jdbc.update("UPDATE audit.audit_log SET payload = '{}'"))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM audit.audit_log"))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.execute("TRUNCATE audit.audit_log"))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("append-only");
    }

    @Test
    void detectsAnEditMadeWithTheTriggerDisabled() {
        SealedAuditEntry target = auditLog.append(record("{\"amount\":100}"));
        auditLog.append(record("{\"after\":true}"));
        long sequenceNo = sequenceNoOf(target);

        withTriggerDisabled(() -> jdbc.update("UPDATE audit.audit_log SET payload = ? WHERE id = ?",
                "{\"amount\":1}", target.record().id()));
        ChainVerification tampered = auditLog.verify();
        withTriggerDisabled(() -> jdbc.update("UPDATE audit.audit_log SET payload = ? WHERE id = ?",
                target.record().payload(), target.record().id()));

        assertThat(tampered.firstBrokenSequenceNo()).hasValue(sequenceNo);
        assertThat(auditLog.verify().intact()).isTrue();
    }

    @Test
    void detectsRemovedNewestEntriesThroughTheChainHead() {
        auditLog.append(record("{}"));
        String realHash = jdbc.queryForObject("SELECT last_hash FROM audit.audit_chain_head", String.class);
        long realSequence = jdbc.queryForObject("SELECT last_sequence_no FROM audit.audit_chain_head", Long.class);

        jdbc.update("UPDATE audit.audit_chain_head SET last_sequence_no = ?, last_hash = ?",
                realSequence + 1, AuditHashChain.GENESIS_HASH);
        ChainVerification truncatedTail = auditLog.verify();
        jdbc.update("UPDATE audit.audit_chain_head SET last_sequence_no = ?, last_hash = ?", realSequence, realHash);

        assertThat(truncatedTail.firstBrokenSequenceNo()).hasValue(realSequence + 1);
        assertThat(auditLog.verify().intact()).isTrue();
    }

    @Test
    void rollsBackWithTheCallersTransaction() {
        long before = entries.count();

        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            auditLog.append(record("{\"rolled\":\"back\"}"));
            throw new IllegalStateException("business write failed");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(entries.count()).isEqualTo(before);
        assertThat(auditLog.verify().intact()).isTrue();
    }

    @Test
    void migratingAgainIsANoOp() {
        migrator.migrate();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit.flyway_audit_history WHERE success", Integer.class))
                .isEqualTo(1);
    }

    private long sequenceNoOf(SealedAuditEntry entry) {
        return jdbc.queryForObject("SELECT sequence_no FROM audit.audit_log WHERE id = ?", Long.class, entry.record().id());
    }

    private void withTriggerDisabled(Runnable change) {
        jdbc.execute("ALTER TABLE audit.audit_log DISABLE TRIGGER audit_log_append_only");
        try {
            change.run();
        } finally {
            jdbc.execute("ALTER TABLE audit.audit_log ENABLE TRIGGER audit_log_append_only");
        }
    }

    private static AuditRecord record(String payload) {
        return new AuditRecord(UUID.randomUUID(), Instant.now(), "user-123", "UPDATE", "leave-request",
                UUID.randomUUID().toString(), payload);
    }
}
