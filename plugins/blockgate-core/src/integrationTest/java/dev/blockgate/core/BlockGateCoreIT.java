package dev.blockgate.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Testcontainers
class BlockGateCoreIT {

    @Container
    static final PostgreSQLContainer PG = new PostgreSQLContainer("postgres:17.12");

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private BlockGateCore loadWithRealDb() {
        Map<String, String> env = Map.of(
                "BLOCKGATE_DB_URL", PG.getJdbcUrl(),
                "BLOCKGATE_DB_USER", PG.getUsername(),
                "BLOCKGATE_DB_PASSWORD", PG.getPassword());
        return MockBukkit.load(BlockGateCore.class, env);
    }

    @Test
    @DisplayName("T13 / AC-6: 실제 DB로 활성화 → INFO 'DB 연결 성공 (스키마 버전 1)', 플러그인 활성 유지")
    void logsSuccessWithRealDatabase() {
        BlockGateCore plugin = loadWithRealDb();
        List<LogRecord> logs = capture(plugin);

        plugin.startupFuture().orTimeout(60, TimeUnit.SECONDS).join();
        server.getScheduler().performOneTick();

        assertThat(plugin.isEnabled()).isTrue();
        assertThat(logs).anySatisfy(r -> {
            assertThat(r.getLevel()).isEqualTo(Level.INFO);
            assertThat(r.getMessage()).isEqualTo("DB 연결 성공 (스키마 버전 1)");
        });
    }

    static List<LogRecord> capture(BlockGateCore plugin) {
        List<LogRecord> records = new CopyOnWriteArrayList<>();
        plugin.getLogger().addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        return records;
    }
}
