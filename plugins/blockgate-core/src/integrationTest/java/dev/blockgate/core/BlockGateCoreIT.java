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

import com.zaxxer.hikari.HikariDataSource;
import dev.blockgate.core.db.DataSourceFactory;
import dev.blockgate.core.db.DatabaseSettings;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;
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

    @Test
    @DisplayName("T14 / AC-7: 비활성화 → 풀 닫힘")
    void closesPoolOnDisable() {
        BlockGateCore plugin = loadWithRealDb();
        plugin.startupFuture().orTimeout(60, TimeUnit.SECONDS).join();
        server.getScheduler().performOneTick();
        var dataSource = plugin.dataSource();
        assertThat(dataSource.isClosed()).isFalse();

        server.getPluginManager().disablePlugin(plugin);

        assertThat(dataSource.isClosed()).isTrue();
    }

    @Test
    @DisplayName("T18 / AC-7: 초기화 도중(풀 생성 직후, 마이그레이션 전) 비활성화 → 그 풀은 닫히고, 실행기 종료, dataSource() null")
    void disableDuringStartupDoesNotLeakPool() throws Exception {
        AtomicReference<BlockGateCore> self = new AtomicReference<>();
        AtomicReference<HikariDataSource> opened = new AtomicReference<>();
        CountDownLatch poolCreated = new CountDownLatch(1);
        Function<DatabaseSettings, HikariDataSource> opener = settings -> {
            HikariDataSource ds = DataSourceFactory.create(settings);
            opened.set(ds);
            poolCreated.countDown();
            // 시뮬레이션 관문: 플러그인이 종료를 시작할 때까지 초기화를 붙잡아 둔다 (상한 30초)
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            while ((self.get() == null || !self.get().isShuttingDown()) && System.nanoTime() < deadline
                    && !Thread.currentThread().isInterrupted()) {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(5));
            }
            return ds;
        };
        BlockGateCore plugin = MockBukkit.load(BlockGateCore.class, envFor(PG), opener);
        self.set(plugin);
        assertThat(poolCreated.await(30, TimeUnit.SECONDS)).isTrue();

        server.getPluginManager().disablePlugin(plugin);

        assertThat(opened.get().isClosed()).as("초기화 중 만든 풀").isTrue();
        assertThat(plugin.dbExecutor().isTerminated()).as("DB 실행기 종료").isTrue();
        assertThat(plugin.dataSource()).isNull();
    }

    private static Map<String, String> envFor(PostgreSQLContainer pg) {
        return Map.of(
                "BLOCKGATE_DB_URL", pg.getJdbcUrl(),
                "BLOCKGATE_DB_USER", pg.getUsername(),
                "BLOCKGATE_DB_PASSWORD", pg.getPassword());
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
